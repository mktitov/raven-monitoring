/*
 *  Copyright 2009 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.sched.impl;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.CancelableTask;
import org.raven.sched.CancelationProcessor;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.ManagedTask;
import org.raven.sched.Task;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.AbstractActionViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.LoggerHelper;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.util.LoadAverageStatistic;
import org.raven.util.NodeUtils;
import org.raven.util.OperationStatistic;
import org.weda.annotations.constraints.NotNull;
import org.weda.converter.TypeConverterException;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=SchedulersNode.class)
public class ExecutorServiceNode extends BaseNode
        implements ExecutorService, RejectedExecutionHandler, Viewable
{
    private final String managedTasksLock = "l";
    private final int CHECK_MANAGED_TASKS_INTERVAL = 10000;

    @NotNull @Parameter(defaultValue="2")
    private Integer corePoolSize;

    @NotNull @Parameter(defaultValue="6")
    private Integer maximumPoolSize;

    @NotNull @Parameter(defaultValue="60")
    private Long keepAliveTime;

    @NotNull @Parameter(defaultValue="SECONDS")
    private TimeUnit timeUnit;

    @Parameter
    private Integer maximumQueueSize;

    @Parameter(readOnly=true)
    private OperationStatistic executedTasks;

    @Parameter(readOnly=true)
    private AtomicLong rejectedTasks;

    @Parameter(readOnly=true)
    private LoadAverageStatistic loadAverage;

    @NotNull @Parameter(defaultValue=""+CHECK_MANAGED_TASKS_INTERVAL)
    private Long checkManagedTasksInterval;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean cacheTaskWrappers;

    @Message private String interruptDisplayMessage;
    @Message private String interruptConfirmationMessage;
    @Message private String interruptCompletionMessage;

    private ThreadPoolExecutor executor;
    private ScheduledThreadPoolExecutor scheduledExecutor;
    private BlockingQueue queue;
    private Collection<AbstractTaskWrapper> executingTasks;
    private Set<Node> managedTasks;
    private DelayQueue<DelayedTaskWrapper> delayedTasks;
    private AtomicLong taskIdCounter;
    private volatile long maxExecutionWaitTime;
    private volatile long avgExecutionWaitTime;
    private Lock waitTimeLock;
    private GenericObjectPool taskWrappersPool;

    @Override
    protected void initFields() {
        super.initFields();
        executor = null;
        queue = null;
        executingTasks = null;
        taskWrappersPool = null;
        rejectedTasks = new AtomicLong();
        executedTasks = new OperationStatistic();
        taskIdCounter = new AtomicLong();
        managedTasks = new HashSet<Node>();
        waitTimeLock = new ReentrantLock();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        resetStatFields();
        Integer capacity = maximumQueueSize;
        if (capacity==null)
            queue = new ZeroBlockingQueue();
        else
            queue = new LinkedBlockingQueue(capacity);
        executingTasks = new ConcurrentSkipListSet();
        delayedTasks = new DelayQueue<DelayedTaskWrapper>();
        managedTasks = new HashSet<Node>();
        executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, queue);
        executor.execute(new TaskWrapper(new DelayedTaskExecutor()));
        loadAverage = new LoadAverageStatistic(300000, maximumPoolSize);
        if (cacheTaskWrappers)
            taskWrappersPool = new GenericObjectPool(new TaskWrapperObjectPoolFactory(), 
                    maximumPoolSize+(capacity==null?0:capacity), 
                    GenericObjectPool.WHEN_EXHAUSTED_FAIL, 0, corePoolSize);
        delayedTasks.add(new DelayedTaskWrapper(new TasksManagerTask(checkManagedTasksInterval)
                , checkManagedTasksInterval, delayedTasks));
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        executor.shutdown();
        resetStatFields();
        executingTasks = null;
        delayedTasks.clear();
        delayedTasks = null;
    }

    public void execute(Task task) throws ExecutorServiceException {
        try {
            if (Status.STARTED.equals(getStatus())) { 
//                getLogger().debug("Executing: "+task.getStatusMessage());
                executor.execute(getTaskWrapper(task));
            }
        } catch (Exception e) {
            rejectedTasks.incrementAndGet();
            String message = String.format(
                        "Error executing task for node (%s)"
                        , task.getTaskNode().getPath());
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(message, e);
            throw new ExecutorServiceException(message, e);
        } 
    }

    public void execute(long delay, Task task) throws ExecutorServiceException {
        if (Status.STARTED.equals(getStatus()))
            delayedTasks.add(new DelayedTaskWrapper(task, delay, delayedTasks));
    }

    public boolean executeQuietly(Task task) {
        try {
            execute(task);
            return true;
        } catch (ExecutorServiceException ex) {
            if (task.getTaskNode().isLogLevelEnabled(LogLevel.ERROR))
                task.getTaskNode().getLogger().error(String.format(
                        "Error executing task (%s)", task.getStatusMessage()));
            return false;
        }
    }

    public boolean executeQuietly(long delay, Task task) {
        try {
            execute(delay, task);
            return true;
        } catch (ExecutorServiceException ex) {
            if (task.getTaskNode().isLogLevelEnabled(LogLevel.ERROR))
                task.getTaskNode().getLogger().error(String.format(
                        "Error executing delayed task (%s)", task.getStatusMessage()));
            return false;
        }
    }
    
    private AbstractTaskWrapper getTaskWrapper(Task task) throws Exception {
        if (taskWrappersPool==null) 
            return new TaskWrapper(task);
        else {
            PoolableTaskWrapper taskWrapper = (PoolableTaskWrapper)taskWrappersPool.borrowObject();
            taskWrapper.init(task);
            return taskWrapper;
        }
    }

    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
    {
        rejectedTasks.incrementAndGet();
        if (isLogLevelEnabled(LogLevel.ERROR))
            error(String.format(
                    "Error executing task for node (%s). Executor service does not have " +
                    "the free thread for task execution"
                    , ((Task)r).getTaskNode().getPath()));
    }

    @Parameter(readOnly=true)
    public Integer getExecutingTaskCount() {
        return executingTasks==null? null : executingTasks.size();
    }

    @Parameter(readOnly=true)
    public Integer getLargestPoolSize() {
        return executor==null? null : executor.getLargestPoolSize();
    }
    
    @Parameter(readOnly=true)
    public Integer getCurrentPoolSize() {
        return executor==null? null : executor.getPoolSize();
    }
    
    @Parameter(readOnly=true)
    public Long getMaxExecutionWaitTime() {
        return maxExecutionWaitTime;
    }
    
    @Parameter(readOnly=true)
    public String getMaxExecutionWaitTimeMS() {
        return String.format("%.5f", maxExecutionWaitTime*1e-6);
    }
    
    @Parameter(readOnly=true)
    public Long getAvgExecutionWaitTime() {
        return avgExecutionWaitTime;
    }

    @Parameter(readOnly=true)
    public String getAvgExecutionWaitTimeMS() {
        return String.format("%.5f", avgExecutionWaitTime*1e-6);
    }

    public OperationStatistic getExecutedTasks() {
        return executedTasks;
    }

    public Long getCheckManagedTasksInterval() {
        return checkManagedTasksInterval;
    }

    public void setCheckManagedTasksInterval(Long checkManagedTasksInterval) {
        this.checkManagedTasksInterval = checkManagedTasksInterval;
    }

    public Boolean getCacheTaskWrappers() {
        return cacheTaskWrappers;
    }

    public void setCacheTaskWrappers(Boolean cacheTaskWrappers) {
        this.cacheTaskWrappers = cacheTaskWrappers;
    }

    public AtomicLong getRejectedTasks() {
        return rejectedTasks;
    }

    public LoadAverageStatistic getLoadAverage() {
        return loadAverage;
    }

    public Integer getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(Integer corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public Long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(Long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public Integer getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(Integer maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public Integer getMaximumQueueSize() {
        return maximumQueueSize;
    }

    public void setMaximumQueueSize(Integer maximumQueueSize) {
        this.maximumQueueSize = maximumQueueSize;
    }

    private void addDelayedTasksTable(ArrayList<ViewableObject> vos) throws TypeConverterException {
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>Dealayed tasks</b>"));
        TableImpl table = new TableImpl(new String[]{"Node submited the task", "Execution delayed until"
                , "Seconds to execution", "Status"});
        Collection<DelayedTaskWrapper> _delayedTasks = delayedTasks;
        if (_delayedTasks != null && !_delayedTasks.isEmpty()) {
            long time = System.currentTimeMillis();
            for (DelayedTaskWrapper task : _delayedTasks) {
                String date = converter.convert(String.class, new Date(task.startAt), "dd.MM.yyyy HH:mm:ss");
                table.addRow(new Object[]{task.task.getTaskNode().getPath(), date
                        , (task.startAt - time) / 1000, task.task.getStatusMessage()});
            }
        }
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
    }

    private void addExecutingTasksTable(ArrayList<ViewableObject> vos) throws TypeConverterException {
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>Executing tasks</b>"));
        TableImpl table = new TableImpl(new String[]{"", "Node submited the task", "Status"
                , "Is Managed task?", "Execution start time", "Execution duration (sec)", "Thread status"
                , "Stack trace"});
        Collection<AbstractTaskWrapper> taskList = executingTasks;
        if (taskList != null) {
            for (AbstractTaskWrapper task : taskList) {
                String date = null;
                if (task.getExecutionStart() != 0) 
                    date = converter.convert(String.class, new Date(task.getExecutionStart())
                            , "dd.MM.yyyy HH:mm:ss");
                Thread thread = task.getThread();
                String threadStatus = thread == null ? null : thread.getState().toString();
                ViewableObject traceVO = null;
                if (thread != null) {
                    StackTraceElement[] elems = thread.getStackTrace();
                    if (elems != null && elems.length > 0) {
                        TableImpl trace = new TableImpl(new String[]{"Stack trace"});
                        for (StackTraceElement elem : elems) {
                            trace.addRow(new Object[]{elem.toString()});
                        }
                        traceVO = new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, trace, "trace");
                    }
                }
                InterruptThreadAction interruptAction = null;
                if (thread != null) 
                    interruptAction = new InterruptThreadAction(interruptConfirmationMessage
                            , interruptDisplayMessage, interruptCompletionMessage, this, thread);
                table.addRow(new Object[]{interruptAction, task.getTaskNode().getPath()
                        , task.getStatusMessage()
                        , task.getTask() instanceof ManagedTask ? "<b style='color:green'>Yes</b>" : "No"
                        , date, task.getExecutionDuation(), threadStatus, traceVO});
            }
        }
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
    }

    private void resetStatFields() {
        executedTasks.reset();
        rejectedTasks.set(0l);
        taskIdCounter.set(0l);
        maxExecutionWaitTime = 0;
        avgExecutionWaitTime = 0;
    }
    
    private void aggExecutionWaitTime(long waitTime) {
        waitTimeLock.lock();
        try {
            if (waitTime>maxExecutionWaitTime)
                maxExecutionWaitTime = waitTime;
            if (avgExecutionWaitTime==0)
                avgExecutionWaitTime = waitTime;
            else 
                avgExecutionWaitTime = (avgExecutionWaitTime+waitTime)/2;
        } finally {
            waitTimeLock.unlock();
        }
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(
            Map<String, NodeAttribute> refreshAttributes) throws Exception
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;
        ArrayList<ViewableObject> vos = new ArrayList<ViewableObject>(4);
        addDelayedTasksTable(vos);
        addExecutingTasksTable(vos);
        
        return vos;
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }
    
    private abstract class AbstractTaskWrapper implements Task, Comparable {
        private long executionStart;
        private Thread thread;
        
        
        public long getExecutionStart() {
            return executionStart;
        }

        public long getExecutionDuation() {
            return executionStart==0? 0 : (System.currentTimeMillis() - executionStart)/1000;
        }
        
        public Thread getThread() {
            return thread;
        }


        public Node getTaskNode() {
            return getTask().getTaskNode();
        }

        public String getStatusMessage() {
            return getTask().getStatusMessage();
        }

        public int compareTo(Object o) {
            return Long.compare(getId(), ((AbstractTaskWrapper)o).getId());
        }
        
        protected abstract Task getTask();
        protected abstract long getCreated();
        protected abstract long getId();
        
        public void run() {
            aggExecutionWaitTime(System.nanoTime()-getCreated());
            thread = Thread.currentThread();
            executionStart = executedTasks.markOperationProcessingStart();
            long startTime = System.currentTimeMillis();
            boolean enableTaskExecution = true;
            final Task _task = getTask();
            try {
                try {
                    if (_task instanceof ManagedTask) {
                        synchronized(managedTasksLock) {
                            ManagedTask managedTask = (ManagedTask) _task;
                            if (managedTasks.contains(managedTask)) {
                                enableTaskExecution = false;
                                if (isLogLevelEnabled(LogLevel.WARN))
                                    getLogger().warn("Skipping executing the task ({}). "
                                            + "The task is already executing", managedTask.getPath());
                            } else 
                                managedTasks.add(managedTask);
                        }
                    } else if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug(String.format(
                                "Executing task for node (%s)", _task.getTaskNode().getPath()));
                    if (enableTaskExecution) {
                        executingTasks.add(this);
                        _task.run();
                    }
                } catch(Throwable e) {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        error(String.format("Error executing task for node (%s)", _task.getTaskNode().getPath())
                                , e);
                }
            } finally {
                executedTasks.markOperationProcessingEnd(executionStart);
                loadAverage.addDuration(System.currentTimeMillis()-startTime);
                Collection executingTasksList = executingTasks;
                if (executingTasksList!=null)
                    executingTasksList.remove(this);
                if (enableTaskExecution && _task instanceof ManagedTask) {
                    Collection<Node> _managedTasks = managedTasks;
                    ManagedTask managedTask = (ManagedTask) _task;
                    if (_managedTasks!=null)
                        synchronized(managedTasksLock) {
                            _managedTasks.remove(managedTask);
                        }
                }
                thread = null;
                
            }            
        }

    }

    private class TaskWrapper extends AbstractTaskWrapper
    {
        private final Task task;
        private final Long id;
        private final long created = System.nanoTime();
//        private long executionStart;
//        private Thread thread;

        public TaskWrapper(Task task) {
            this.task = task;
            id = taskIdCounter.incrementAndGet();
        }

        public long getId() {
            return id;
        }
        
        @Override
        protected Task getTask() {
            return task;
        }

        @Override
        protected long getCreated() {
            return created;
        }

//        public Thread getThread() {
//            return thread;
//        }

//        public Node getTaskNode() {
//            return task.getTaskNode();
//        }
//
//        public String getStatusMessage() {
//            return task.getStatusMessage();
//        }
//
//        public long getExecutionStart() {
//            return executionStart;
//        }
//
//        public long getExecutionDuation() {
//            return executionStart==0? 0 : (System.currentTimeMillis() - executionStart)/1000;
//        }

//        public void run() {
//            aggExecutionWaitTime(System.nanoTime()-created);
//            thread = Thread.currentThread();
//            executionStart = executedTasks.markOperationProcessingStart();
//            long startTime = System.currentTimeMillis();
//            boolean enableTaskExecution = true;
//            try {
//                try {
//                    if (task instanceof ManagedTask) {
//                        synchronized(managedTasksLock) {
//                            ManagedTask managedTask = (ManagedTask) task;
//                            if (managedTasks.contains(managedTask)) {
//                                enableTaskExecution = false;
//                                if (isLogLevelEnabled(LogLevel.WARN))
//                                    getLogger().warn("Skipping executing the task ({}). "
//                                            + "The task is already executing", managedTask.getPath());
//                            } else 
//                                managedTasks.add(managedTask);
//                        }
//                    } else if (isLogLevelEnabled(LogLevel.DEBUG))
//                        debug(String.format(
//                                "Executing task for node (%s)", task.getTaskNode().getPath()));
//                    if (enableTaskExecution) {
//                        executingTasks.add(this);
//                        task.run();
//                    }
//                } catch(Throwable e) {
//                    if (isLogLevelEnabled(LogLevel.ERROR))
//                        error(String.format("Error executing task for node (%s)", task.getTaskNode().getPath())
//                                , e);
//                }
//            } finally {
//                executedTasks.markOperationProcessingEnd(executionStart);
//                loadAverage.addDuration(System.currentTimeMillis()-startTime);
//                Collection executingTasksList = executingTasks;
//                if (executingTasksList!=null)
//                    executingTasksList.remove(this);
//                if (enableTaskExecution && task instanceof ManagedTask) {
//                    Collection<Node> _managedTasks = managedTasks;
//                    ManagedTask managedTask = (ManagedTask) task;
//                    if (_managedTasks!=null)
//                        synchronized(managedTasksLock) {
//                            _managedTasks.remove(managedTask);
//                        }
//                }
//            }
//        }
//
//        public int compareTo(Object o) {
//            return id.compareTo(((TaskWrapper)o).getId());
//        }

    }
    
    private class PoolableTaskWrapper extends AbstractTaskWrapper {
        private final TaskWrapperObjectPoolFactory factory;
        private Task task;
        private long created;
        private long id;

        public PoolableTaskWrapper(TaskWrapperObjectPoolFactory factory) {
            this.factory = factory;
        }
        
        public void init(Task task) {
            this.task = task;
            this.id = taskIdCounter.incrementAndGet();
            this.created = System.nanoTime();
        }
        
        public void reset() {
            this.task = null;
            id = 0l;
            created = 0l;
        }

        @Override
        protected Task getTask() {
            return task;
        }

        @Override
        protected long getCreated() {
            return created;
        }

        @Override
        protected long getId() {
            return id;
        }        

        @Override
        public void run() {
            try {
                super.run();
            } finally {
                try {
                    factory.passivateObject(this);
                } catch (Exception ex) {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        getLogger().error("Error on task wrapper passivating", ex);
                }
            }
        }

    }

    private class DelayedTaskWrapper implements Delayed, CancelationProcessor {
        private final long startAt;
        private final Task task;
        private final DelayQueue<DelayedTaskWrapper> queue;

        public DelayedTaskWrapper(Task task, long delay, DelayQueue<DelayedTaskWrapper> queue) {
            this.task = task;
            this.startAt = System.currentTimeMillis()+delay;
            this.queue = queue;
            if (task instanceof CancelableTask)
                ((CancelableTask)task).setCancelationProcessor(this);
        }

        public long getDelay(TimeUnit unit) {
            return unit.convert(startAt-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        public int compareTo(Delayed o) {
            if (o==this)
                return 0;
            long d = getDelay(TimeUnit.MILLISECONDS)-o.getDelay(TimeUnit.MILLISECONDS);
            return d==0? 0 : (d<0? -1 : 1);
        }

        public void cancel() {
            queue.remove(this);
        }
    }

    private class DelayedTaskExecutor implements Task {
        public Node getTaskNode() {
            return ExecutorServiceNode.this;
        }

        public String getStatusMessage() {
            return "Servicing delayed tasks";
        }

        public void run() {
            try {
                for (;;) {
                    DelayQueue<DelayedTaskWrapper> _delayedTasks = delayedTasks;
                    if (_delayedTasks==null)
                        return;
                    DelayedTaskWrapper delayedTask = _delayedTasks.poll(1, TimeUnit.SECONDS);
                    if (delayedTask!=null)
                        executeQuietly(delayedTask.task);
                }
            } catch (InterruptedException e) {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    getLogger().debug("Delayed task executor stopped");
                Thread.interrupted();
            }
        }
    }

    private class InterruptThreadAction extends AbstractActionViewableObject {
        private final Thread thread;
        private final String completionMessage;

        public InterruptThreadAction(
                String confirmationMessage, String displayMessage, String completionMessage
                , Node owner, Thread thread)
        {
            super(confirmationMessage, displayMessage, owner, true);
            this.thread = thread;
            this.completionMessage = completionMessage;
        }

        @Override
        public String executeAction() throws Exception  {
            thread.interrupt();
            return completionMessage;
        }       
    }

    private class TasksManagerTask implements Task {
        private final long restartInterval;
        private final static long RESTART_TIMEOUT = 10000;
        private final LoggerHelper logger;

        public TasksManagerTask(long restartInterval) {
            this.restartInterval = restartInterval;
            this.logger = new LoggerHelper(ExecutorServiceNode.this, "Managed task reexecutor. ");
        }

        public Node getTaskNode() {
            return ExecutorServiceNode.this;
        }

        public String getStatusMessage() {
            return "Managed tasks reexecutor";
        }
        
        private void evictTaskWrappers() {
            if (taskWrappersPool!=null) {
                try {
                    taskWrappersPool.evict();
                } catch (Exception ex) {
                    if (logger.isErrorEnabled())
                        logger.error("Error evicting task wrappers pool", ex);
                }
            }
        }

        public void run() {
            try {
                evictTaskWrappers();
                List<ManagedTask> tasksToStart = new LinkedList<ManagedTask>();
                for (ManagedTask task: NodeUtils.extractNodesOfType(getDependentNodes(), ManagedTask.class)) {
                    boolean hasTask = false;
                    if (logger.isDebugEnabled())
                        logger.debug("Found managed task ({})", task.getPath());
                    synchronized(managedTasksLock) {
                        hasTask = managedTasks==null || !managedTasks.contains(task);
                    }
                    if (hasTask) {
                        try {
                            if (logger.isDebugEnabled())
                                logger.debug("Reexecuting task ({})", task.getPath());
                            switch (task.getTaskRestartPolicy()) {
                                case REEXECUTE_TASK: execute(task); break;
                                case RESTART_NODE:
                                    if (Status.STARTED.equals(task.getStatus())) {
                                        task.stop();
                                        tasksToStart.add(task);
                                    }
                                    break;
                            }
                        } catch (Throwable e){
                            if (logger.isErrorEnabled())
                                logger.error(
                                        String.format("Error reexecuting the task (%s)", task.getPath())
                                        , e);
                        }
                    } else if (logger.isDebugEnabled())
                        logger.debug("Managed task reexecutor. Task ({}) already executing", task.getPath());
                }
                long ts = System.currentTimeMillis();
                while (!tasksToStart.isEmpty() && System.currentTimeMillis()<ts+RESTART_TIMEOUT) {
                    for (Iterator<ManagedTask> it = tasksToStart.iterator(); it.hasNext();) {
                        ManagedTask task = it.next();
                        if (Status.INITIALIZED.equals(task.getStatus())) {
                            task.start();
                            it.remove();
                        }
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException ex) {
                    }
                }
            } finally {
                executeQuietly(restartInterval, this);
            }
        }
    }
    
    private class TaskWrapperObjectPoolFactory implements PoolableObjectFactory {

        public Object makeObject() throws Exception {
            return new PoolableTaskWrapper(this);
        }

        public void destroyObject(Object obj) throws Exception {
        }

        public boolean validateObject(Object obj) {
            return true;
        }

        public void activateObject(Object obj) throws Exception {
        }

        public void passivateObject(Object obj) throws Exception {
        }        
    } 
}

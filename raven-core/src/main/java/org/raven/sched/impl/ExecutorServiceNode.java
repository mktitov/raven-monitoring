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
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.ExecutorTask;
import org.raven.sched.ExecutorTaskHolder;
import org.raven.sched.ExecutorThreadFactory;
import org.raven.sched.ForkJoinTaskSupport;
import org.raven.sched.ManagedTask;
import org.raven.sched.Task;
import org.raven.sched.TaskExecutionListener;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
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
        implements ExecutorService, Viewable, TaskExecutionListener
{
    private final String managedTasksLock = "l";
    private final int CHECK_MANAGED_TASKS_INTERVAL = 10000;
    
    @NotNull @Parameter(defaultValue="THREADED_POOL")
    private Type type;

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
    
    @Message private String interruptDisplayMessage;
    @Message private String interruptConfirmationMessage;
    @Message private String interruptCompletionMessage;

    ExecutorThreadFactory threadFactory;
    private AbstractExecutorService executor;
    private LoggerHelper taskLogger;
    private BlockingQueue queue;
    private Set<Node> managedTasks;
    private volatile DelayQueue<DelayedTaskWrapper> delayedTasks;
    private AtomicLong taskIdCounter;
    private volatile long maxExecutionWaitTime;
    private volatile long avgExecutionWaitTime;
    private DelayedTaskExecutorThread delayedTaskExecutorThread;
    private Lock waitTimeLock;
//    private ScheduledExecutorService delayedTasksExecutor;
    private Timer delayedTasksTimer;

    @Override
    protected void initFields() {
        super.initFields();
        executor = null;
        threadFactory = null;
        queue = null;
        taskLogger = null;
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
        delayedTasks = new DelayQueue<DelayedTaskWrapper>();
        managedTasks = new HashSet<Node>();
        taskLogger = new LoggerHelper(this, "Task executor. ");        
        if (type==Type.THREADED_POOL) {
            if (capacity==null)
                queue = new ZeroBlockingQueue();
            else
                queue = new LinkedBlockingQueue(capacity);
            ThreadPoolExecutorThreadFactory _threadFactory = new ThreadPoolExecutorThreadFactory(getPath());
            threadFactory = _threadFactory;
            executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, queue, _threadFactory);
        } else {
            ForkJoinExecutorThreadFactory _threadFactory = new ForkJoinExecutorThreadFactory(getPath());
            threadFactory = _threadFactory;
            executor = new ForkJoinPool(corePoolSize, _threadFactory, null, true);
        }
//        executor.execute(new TaskWrapper(new DelayedTaskExecutor()));
//        delayedTasksExecutor = new ScheduledThreadPoolExecutor(1);
//        delayedTasksTimer = new Timer(true);
        delayedTaskExecutorThread = new DelayedTaskExecutorThread(new DelayedTaskExecutor());
        delayedTaskExecutorThread.start();
        loadAverage = new LoadAverageStatistic(300000, maximumPoolSize);
        delayedTasks.add(new DelayedTaskWrapper(new TasksManagerTask(checkManagedTasksInterval)
                , checkManagedTasksInterval, delayedTasks));
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        executor.shutdown();
        delayedTaskExecutorThread.interrupt();
        resetStatFields();
//        delayedTasks.clear();
//        delayedTasks = null;
    }

    @Override
    public synchronized void stop() throws NodeError {
        boolean needStop = isStarted();
        super.stop(); 
        if (needStop)
            try {
                final Node effectiveParent = getEffectiveParent();
                if (effectiveParent instanceof ExecutorServiceBalancerNode) {
                    ExecutorServiceBalancerNode balancer = (ExecutorServiceBalancerNode) effectiveParent;
                    for (DelayedTaskWrapper task : delayedTasks) {
                        if (delayedTasks.remove(task))
                            balancer.executeQuietly(task.getDelay(TimeUnit.MILLISECONDS), task.getTask());
                    }
                }
            } finally {
                delayedTasks.clear();
                delayedTasks = null;
            }
    }

    @Override
    public void execute(Task task) throws ExecutorServiceException {
        try {
            if (isStarted()) 
                if (type==Type.THREADED_POOL || !(task instanceof ForkJoinTaskSupport))
                    executor.execute(getTaskWrapper(task));
                else
                    ((ForkJoinPool)executor).execute(((ForkJoinTaskSupport)task).getForJoinTask());
            else
                throw new ExecutorServiceException("Can't execute task... Not Started");
            
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

    @Override
    public void execute(final long delay, final Task task) throws ExecutorServiceException {
        if (isStarted()) {
            delayedTasks.add(new DelayedTaskWrapper(task, delay, delayedTasks));
//            delayedTasksTimer.schedule(new TimerTask() {
//                final long expectedExecTime = System.currentTimeMillis()+delay;
//                @Override public void run() {
////                    System.out.println("DISPERTION: "+(System.currentTimeMillis()-expectedExecTime));
//                    executeQuietly(task);
//                }
//            }, delay);
//            delayedTasksExecutor.schedule(new Runnable() {
//                @Override public void run() {
//                    executeQuietly(task);
//                }
//            }, delay, TimeUnit.MILLISECONDS);
        } else
            throw new ExecutorServiceException("Can't execute task... Not Started");
    }

    @Override
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

    @Override
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
    
    private ExecutorTask getTaskWrapper(Task task) throws Exception {
        ExecutorTask result = task instanceof ExecutorTask? (ExecutorTask)task : new TaskWrapper(task);
        result.init(this);
        return result;
    }

    @Parameter(readOnly=true)
    public Integer getExecutingTaskCount() {
        return threadFactory!=null? threadFactory.getExecutingTasksCount() : null;
    }

    @Parameter(readOnly=true)
    public Integer getLargestPoolSize() {
        return executor instanceof ThreadPoolExecutor? ((ThreadPoolExecutor)executor).getLargestPoolSize() : null;
    }
    
    @Parameter(readOnly=true)
    public Integer getCurrentPoolSize() {
        return threadFactory==null? null : threadFactory.getRunningThreadsCount();
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

    public AtomicLong getRejectedTasks() {
        return rejectedTasks;
    }

    public LoadAverageStatistic getLoadAverage() {
        return loadAverage;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
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
                String date = converter.convert(String.class, new Date(task.getStartAt()), "dd.MM.yyyy HH:mm:ss");
                table.addRow(new Object[]{task.getTask().getTaskNode().getPath(), date
                        , (task.getStartAt() - time) / 1000, task.getTask().getStatusMessage()});
            }
        }
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
    }

    private void addExecutingTasksTable(ArrayList<ViewableObject> vos) throws TypeConverterException {
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>Executing tasks</b>"));
        TableImpl table = new TableImpl(new String[]{"", "Node submited the task", "Status"
                , "Is Managed task?", "Execution start time", "Execution duration (sec)", "Thread status"
                , "Stack trace"});
        final Collection<ExecutorTask> taskList = threadFactory.getExecutingTasks();
        final TimeUnit nano = TimeUnit.NANOSECONDS;
        for (ExecutorTask task : taskList) {
            String date = null;
            if (task.getExecutionStart() != 0) 
                date = converter.convert(String.class, new Date(nano.toMillis(task.getExecutionStart()))
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
            boolean managedTask = task instanceof TaskWrapper && ((TaskWrapper)task).getTask() instanceof ManagedTask;
            table.addRow(new Object[]{interruptAction, task.getTaskNode().getPath()
                    , task.getStatusMessage()
                    , managedTask ? "<b style='color:green'>Yes</b>" : "No"
                    , date, nano.toSeconds(task.getExecutionDuration()), threadStatus, traceVO});
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

    @Override
    public void taskExecutionFinished(final long taskStartTime, final long taskFinishTime) {
        executedTasks.markOperationProcessingEnd(TimeUnit.NANOSECONDS.toMillis(taskStartTime));
        loadAverage.addDuration(TimeUnit.NANOSECONDS.toMillis(taskFinishTime-taskStartTime));
    }

    @Override
    public void taskExecutionStarted(final long taskScheduleTime, final long taskStartTime) {
        aggExecutionWaitTime(taskStartTime-taskScheduleTime);
        executedTasks.markOperationProcessingStart();
    }

    @Override
    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return null;
    }

    @Override
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

    @Override
    public Boolean getAutoRefresh() {
        return true;
    }
    
    private class TaskWrapper extends AbstractExecutorTask {
        private final Task task;

        public TaskWrapper(Task task) {
            this.task = task;
        }
        
        @Override
        public Node getTaskNode() {
            return task.getTaskNode();
        }

        @Override
        public String getStatusMessage() {
            return task.getStatusMessage();
        }
        
        public Task getTask() {
            return task;
        }

        @Override
        public void doRun() throws Exception {
            boolean enableTaskExecution = true;
            try {
                if (task instanceof ManagedTask) {
                    synchronized(managedTasksLock) {
                        ManagedTask managedTask = (ManagedTask) task;
                        if (managedTasks.contains(managedTask)) {
                            enableTaskExecution = false;
                            if (isLogLevelEnabled(LogLevel.WARN))
                                getLogger().warn("Skipping executing the task ({}). "
                                        + "The task is already executing", managedTask.getPath());
                        } else 
                            managedTasks.add(managedTask);
                    }
                } else if (taskLogger.isDebugEnabled())
                    taskLogger.debug(String.format(
                            "Executing task for node (%s)", task.getTaskNode().getPath()));
                if (enableTaskExecution) 
                    task.run();
            } finally {
                if (enableTaskExecution && task instanceof ManagedTask) {
                    Collection<Node> _managedTasks = managedTasks;
                    ManagedTask managedTask = (ManagedTask) task;
                    if (_managedTasks!=null)
                        synchronized(managedTasksLock) {
                            _managedTasks.remove(managedTask);
                        }
                }
            }
        }
    }

//    private class DelayedTaskWrapper implements Delayed, CancelationProcessor {
//        private final long startAt;
//        private final Task task;
//        private final DelayQueue<DelayedTaskWrapper> queue;
//
//        public DelayedTaskWrapper(Task task, long delay, DelayQueue<DelayedTaskWrapper> queue) {
//            this.task = task;
//            this.startAt = System.currentTimeMillis()+delay;
//            this.queue = queue;
//            if (task instanceof CancelableTask)
//                ((CancelableTask)task).setCancelationProcessor(this);
//        }
//
//        public long getDelay(TimeUnit unit) {
//            return unit.convert(startAt-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
//        }
//
//        public int compareTo(Delayed o) {
//            if (o==this)
//                return 0;
//            long d = getDelay(TimeUnit.MILLISECONDS)-o.getDelay(TimeUnit.MILLISECONDS);
//            return d==0? 0 : (d<0? -1 : 1);
//        }
//
//        public void cancel() {
//            queue.remove(this);
//        }
//    }
//
    private class DelayedTaskExecutorThread extends Thread implements ExecutorTaskHolder {
        private final DelayedTaskExecutor task;

        public DelayedTaskExecutorThread(DelayedTaskExecutor task) {
            super(task);
            this.task = task;
        }

        @Override
        public void setExecutorTask(ExecutorTask task) {
        }

        @Override
        public ExecutorTask getExecutorTask() {
            return task;
        }
    }
    
    private class DelayedTaskExecutor extends AbstractExecutorTask {
        
        @Override
        public Node getTaskNode() {
            return ExecutorServiceNode.this;
        }

        @Override
        public String getStatusMessage() {
            return "Servicing delayed tasks";
        }

        @Override
        public void doRun() {
            if (isDebugEnabled())
                getLogger().debug("Delayed task executor started");
            try {
                for (;;) {
                    DelayQueue<DelayedTaskWrapper> _delayedTasks = delayedTasks;
                    if (_delayedTasks==null)
                        return;
                    DelayedTaskWrapper delayedTask = _delayedTasks.poll(1, TimeUnit.SECONDS);
                    if (delayedTask!=null)
                        executeQuietly(delayedTask.getTask());
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
        
        public void run() {
            try {
//                evictTaskWrappers();
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
}

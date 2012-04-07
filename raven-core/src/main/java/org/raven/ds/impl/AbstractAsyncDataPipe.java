/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.ds.impl;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.*;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataHandler;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.util.OperationStatistic;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractAsyncDataPipe extends AbstractSafeDataPipe implements Viewable
{
    public static final int RELEASE_HANDLERS_INTERVAL = 1000;
    
    private enum HandlerStatus {WAITING, EXECUTING}

    public static final int MAX_HANDLERS_LOCK_WAIT = 1000;

    @NotNull @Parameter(defaultValue="2")
    private Integer maxHandlersCount;

    @NotNull @Parameter(defaultValue="60")
    private Integer handlerIdleTime;

    @NotNull @Parameter(defaultValue="true")
    private Boolean waitForHandler;
    
    @NotNull @Parameter(defaultValue="30000")
    private Integer waitForHandlerTimeout;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;

    @NotNull @Parameter(defaultValue="true")
    private Boolean handleDataInSeparateThread;

    @Message
    private static String hanlderStatusColumn;
    @Message
    private static String handlerStatusMessageColumn;
    @Message
    private static String handlersTableTitle;
    @Message
    private static String handlerCreationsCountColumn;
    @Message
    private static String handlerCreationTimeColumn;
    @Message
    private static String handlerIdleTimeColumn;
    @Message
    private static String handledRequestsCountColumn;
    @Message
    private static String avgRequestsPerSecondColumn;
    @Message
    private static String avgMillisecondsPerRequest;
    @Message
    private static String handlerStatusColumn;

    protected Queue<HandlerWrapper> handlers;
    private ReadWriteLock handlersLock;
    private Condition waitForHandlerFree;
    private AtomicBoolean releaseHandlersTaskScheduled;

    public abstract DataHandler createDataHandler();
    
    @Override
    protected void initFields()
    {
        super.initFields();

//        handlers = new ArrayList<HandlerWrapper>(10);
        handlers = new ConcurrentLinkedQueue<HandlerWrapper>();
        handlersLock = new ReentrantReadWriteLock();
        waitForHandlerFree = handlersLock.writeLock().newCondition();
        releaseHandlersTaskScheduled = new AtomicBoolean(false);
    }

    @Override
    protected void doStop() throws Exception {
        for (HandlerWrapper handlerInfo: handlers)
            handlerInfo.release();
        handlers.clear();
    }

    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        if (handlersLock.writeLock().tryLock(MAX_HANDLERS_LOCK_WAIT, TimeUnit.MILLISECONDS)) {
            try {
                if (data!=null) {
                    boolean res = false;
                    if (!(res=processData(data, dataSource, context)) && waitForHandler) {
                        if (waitForHandlerFree.await(waitForHandlerTimeout, TimeUnit.MILLISECONDS))
                            res = processData(data, dataSource, context);
                    }
                    if (!res && isLogLevelEnabled(LogLevel.DEBUG))
                        debug("No free handlers to process data from "+dataSource.getPath());
                } else {
                    boolean hasBusy;
                    do {
                        hasBusy=false;
                        for (HandlerWrapper handlerInfo: handlers)
                            if (handlerInfo.isBusy()){
                                hasBusy = true;
                                break;
                            }
                        if (hasBusy)
                            waitForHandlerFree.await(1, TimeUnit.SECONDS);
                    } while (hasBusy);
                    sendDataToConsumers(data, context);
                }
            } finally {
                handlersLock.writeLock().unlock();
            }
        } else
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Handlers lock wait timeout");
    }
    
    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        List<ViewableObject> vos = new ArrayList<ViewableObject>(2);
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+handlersTableTitle+"</b>"));

        TableImpl table = new TableImpl(new String[]{
            hanlderStatusColumn, handlerStatusMessageColumn, handlerCreationsCountColumn,
            handlerCreationTimeColumn, handlerIdleTimeColumn,
            handledRequestsCountColumn, avgMillisecondsPerRequest, avgRequestsPerSecondColumn});
            SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        for (HandlerWrapper handlerInfo: handlers)  {
            table.addRow(new Object[]{
                handlerInfo.status.get().toString(),
                handlerInfo.getHandlerStatusMessage(),
                1, 
                handlerInfo.lastUseTime==0? "" : fmt.format(new Date(handlerInfo.lastUseTime)),
                handlerInfo.lastUseTime==0? 0 : (System.currentTimeMillis()-handlerInfo.lastUseTime)/1000,
                handlerInfo.stat.getOperationsCount(),
                handlerInfo.stat.getAvgMillisecondsPerOperation(),
                handlerInfo.stat.getAvgOperationsPerSecond()
            });
        }

        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));

        return vos;
    }

    private boolean processData(Object data, DataSource dataSource, DataContext context) throws Exception {
        for (HandlerWrapper handlerInfo: handlers)
            if (handlerInfo.handleData(data, dataSource, context))
                return true;
        if (handlers.size()<maxHandlersCount) {
            HandlerWrapper handlerInfo = new HandlerWrapper();
            handlers.add(handlerInfo);
            if (!handlerInfo.handleData(data, dataSource, context))
                return false;
            if (releaseHandlersTaskScheduled.compareAndSet(false, true))
                scheduleHandlerReleaseTask(new ReleaseHandlersTask());
            return true;
        }
        return false;
    }

    public Boolean getAutoRefresh() {
        return Boolean.TRUE;
    }

    public Integer getHandlerIdleTime() {
        return handlerIdleTime;
    }

    public void setHandlerIdleTime(Integer handlerIdleTime) {
        this.handlerIdleTime = handlerIdleTime;
    }

    public Integer getMaxHandlersCount() {
        return maxHandlersCount;
    }

    public void setMaxHandlersCount(Integer maxHandlersCount) {
        this.maxHandlersCount = maxHandlersCount;
    }

    public Integer getWaitForHandlerTimeout() {
        return waitForHandlerTimeout;
    }

    public void setWaitForHandlerTimeout(Integer waitForHandlerTimeout) {
        this.waitForHandlerTimeout = waitForHandlerTimeout;
    }

    public Boolean getWaitForHandler() {
        return waitForHandler;
    }

    public void setWaitForHandler(Boolean waitForHandler) {
        this.waitForHandler = waitForHandler;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Boolean getHandleDataInSeparateThread() {
        return handleDataInSeparateThread;
    }

    public void setHandleDataInSeparateThread(Boolean handleDataInSeparateThread) {
        this.handleDataInSeparateThread = handleDataInSeparateThread;
    }
    
    private void scheduleHandlerReleaseTask(ReleaseHandlersTask task) {
        if (!executor.executeQuietly(RELEASE_HANDLERS_INTERVAL, task) && isLogLevelEnabled(LogLevel.ERROR))
            getLogger().error("Error scheduling release handlers task");
    }
    
    private class ReleaseHandlersTask implements Task {
        public Node getTaskNode() {
            return AbstractAsyncDataPipe.this;
        }

        public String getStatusMessage() {
            return "Release handlers task";
        }

        public void run() {
            if (!Status.STARTED.equals(getStatus()))
                return;
            if (handlersLock.writeLock().tryLock()) {
                int maxIdleTime = handlerIdleTime;
                try {
                    for (Iterator<HandlerWrapper> it=handlers.iterator(); it.hasNext();) {
                        HandlerWrapper handler = it.next();
                        if (!handler.isBusy() && handler.lastUseTime+maxIdleTime*1000<System.currentTimeMillis()) {
                            handler.release();
                            it.remove();
                        }
                    }
                    if (!handlers.isEmpty())
                        scheduleHandlerReleaseTask(this);
                    else
                        releaseHandlersTaskScheduled.set(false);
                } finally {
                    handlersLock.writeLock().unlock();
                }
            } else
                scheduleHandlerReleaseTask(this);
        }
    }

    protected class HandlerWrapper implements Task {
        private final boolean useExecutor = handleDataInSeparateThread;
        private final OperationStatistic stat = new OperationStatistic();
        private final AtomicBoolean busy = new AtomicBoolean(false);
        private final AtomicReference<HandlerStatus> status = new AtomicReference<HandlerStatus>(
                HandlerStatus.WAITING);
        private final Lock taskLock = new ReentrantLock();
        private final Condition taskCondition = taskLock.newCondition();
        private volatile DataHandler handler;
        private volatile boolean hasNewTask = false;
        private volatile boolean stop = false;
        private long lastUseTime;
        private long operationStartTime;
        private boolean running;
        private Object data;
        private DataContext dataContext;
        private DataSource dataSource;

        public boolean handleData(Object data, DataSource dataSource, DataContext context) {
            if (!busy.compareAndSet(false, true) || stop)
                return false;
            operationStartTime = stat.markOperationProcessingStart();
            if (handler==null)
                handler = createDataHandler();
            this.data = data;
            this.dataSource = dataSource;
            this.dataContext = context;
            hasNewTask = true;
            if (useExecutor) {
                if (!handleDataUsingExecutor()) return false;
            } else 
                run();
            return true;
        }

        private boolean handleDataUsingExecutor() {
            if (!running) {
                if (!executor.executeQuietly(this)) {
                    busy.set(false);
                    return false;
                } else {
                    running = true;
                }
            } else {
                taskLock.lock();
                try {
                    taskCondition.signal();
                } finally {
                    taskLock.unlock();
                }
            }
            return true;
        }

        public boolean isBusy() {
            return busy.get();
        }

        public void release() {
            try {
                stop = true;
                if (handler!=null)
                    handler.releaseHandler();
            } finally {
                handler = null;
            }
        }

        public String getHandlerStatusMessage() {
            return handler!=null? handler.getStatusMessage() : "";
        }

        public Node getTaskNode() {
            return AbstractAsyncDataPipe.this;
        }

        public String getStatusMessage() {
            return handler==null? "" : handler.getStatusMessage();
        }
        
        public void run() {
            status.set(HandlerStatus.EXECUTING);
            while (!stop) {
                if (!hasNewTask) 
                    waitForTask();
                else {
                    hasNewTask = false;
                    executeTask();
                    stat.markOperationProcessingEnd(operationStartTime);
                    lastUseTime = System.currentTimeMillis();
                    status.set(HandlerStatus.WAITING);
                    busy.set(false);
                    if (handlersLock.writeLock().tryLock()) try {
                        waitForHandlerFree.signal();
                    } finally {
                        handlersLock.writeLock().unlock();
                    }
                    if (!useExecutor)
                        return;
                }
            }
        }
        
        private void executeTask() {
            try {
                Object resData = handler.handleData(data, dataSource, dataContext, AbstractAsyncDataPipe.this);
                if (SKIP_DATA!=resData)
                    sendDataToConsumers(resData, dataContext);
            } catch (Throwable e) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error("Error handling data", e);
            }
        }
        
        private void waitForTask() {
            try {
                if (taskLock.tryLock()) 
                    try {
                        taskCondition.await(100, TimeUnit.MILLISECONDS);
                    } finally {
                        taskLock.unlock();
                    }
                else
                    TimeUnit.MILLISECONDS.sleep(1);
            } catch (InterruptedException ex) { }
        }
    }
}

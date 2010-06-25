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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataHandler;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
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

    private List<HandlerInfo> handlers;
    private ReadWriteLock handlersLock;
    private Condition waitForHandlerFree;

    public abstract DataHandler createDataHandler();

    @Override
    protected void initFields()
    {
        super.initFields();

        handlers = new ArrayList<HandlerInfo>(10);
        handlersLock = new ReentrantReadWriteLock();
        waitForHandlerFree = handlersLock.writeLock().newCondition();
    }

    @Override
    protected void doStop() throws Exception
    {
        for (HandlerInfo handlerInfo: handlers)
            handlerInfo.release();
        handlers.clear();
    }

    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        if (handlersLock.writeLock().tryLock(MAX_HANDLERS_LOCK_WAIT, TimeUnit.MILLISECONDS))
        {
            try
            {
                boolean res = false;
                if (!(res=processData(data, dataSource, context)) && waitForHandler)
                {
                    if (waitForHandlerFree.await(waitForHandlerTimeout, TimeUnit.MILLISECONDS))
                        res = processData(data, dataSource, context);
                }
                if (!res && isLogLevelEnabled(LogLevel.DEBUG))
                    debug("No free handlers to process data from "+dataSource.getPath());
            }
            finally
            {
                handlersLock.writeLock().unlock();
            }
        }
        else
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Handlers lock wait timeout");
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) throws Exception
    {
        List<ViewableObject> vos = new ArrayList<ViewableObject>(2);
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+handlersTableTitle+"</b>"));

        TableImpl table = new TableImpl(new String[]{
            hanlderStatusColumn, handlerStatusMessageColumn, handlerCreationsCountColumn,
            handlerCreationTimeColumn, handlerIdleTimeColumn,
            handledRequestsCountColumn, avgMillisecondsPerRequest, avgRequestsPerSecondColumn});
        if (handlersLock.readLock().tryLock(MAX_HANDLERS_LOCK_WAIT, TimeUnit.MILLISECONDS))
        {
            try
            {
                SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                for (HandlerInfo handlerInfo: handlers)
                {
                    table.addRow(new Object[]{
                        handlerInfo.status.get().toString(),
                        handlerInfo.getHandlerStatusMessage(),
                        handlerInfo.handlerCreationCount, 
                        handlerInfo.lastUseTime==0? "" : fmt.format(new Date(handlerInfo.lastUseTime)),
                        handlerInfo.lastUseTime==0? 0 : (System.currentTimeMillis()-handlerInfo.lastUseTime)/1000,
                        handlerInfo.stat.getOperationsCount(),
                        handlerInfo.stat.getAvgMillisecondsPerOperation(),
                        handlerInfo.stat.getAvgOperationsPerSecond()
                    });
                }
            }
            finally
            {
                handlersLock.readLock().unlock();
            }
        }

        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));

        return vos;
    }

    private boolean processData(Object data, DataSource dataSource, DataContext context) throws Exception
    {
        for (HandlerInfo handlerInfo: handlers)
            if (handlerInfo.handleData(data, dataSource, context))
                return true;
        if (handlers.size()<maxHandlersCount)
        {
            HandlerInfo handlerInfo = new HandlerInfo();
            handlers.add(handlerInfo);
            handlerInfo.handleData(data, dataSource, context);
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

    private class HandlerInfo implements Task
    {
        private long lastUseTime;
        private AtomicBoolean busy = new AtomicBoolean(false);
        private DataHandler handler;
        private Object data;
        private DataContext dataContext;
        private DataSource dataSource;
        private OperationStatistic stat = new OperationStatistic();
        private int handlerCreationCount = 0;
        private AtomicReference<HandlerStatus> status = new AtomicReference<HandlerStatus>(HandlerStatus.WAITING);
        private Thread taskThread;

        public boolean handleData(Object data, DataSource dataSource, DataContext context)
                throws ExecutorServiceException
        {
            if (!busy.compareAndSet(false, true))
                return false;
            long startTime = stat.markOperationProcessingStart();
            try
            {
                if (handler==null || (System.currentTimeMillis()-handlerIdleTime*1000)>lastUseTime)
                {
                    if (handler!=null)
                        release();
                    lastUseTime = System.currentTimeMillis();
                    ++handlerCreationCount;
                    handler = createDataHandler();
                }
                else
                    lastUseTime = System.currentTimeMillis();

                this.data = data;
                this.dataSource = dataSource;
                this.dataContext = context;
                
                try
                {
                    if (handleDataInSeparateThread)
                        executor.execute(this);
                    else
                        run();
                }
                catch(ExecutorServiceException e)
                {
                    busy.set(false);
                    throw e;
                }
                return true;
            }
            finally
            {
                stat.markOperationProcessingEnd(startTime);
            }
        }

        public void release()
        {
            try
            {
                if (handler!=null)
                    handler.releaseHandler();
            }
            finally
            {
                handler = null;
            }
        }

        public String getHandlerStatusMessage()
        {
            return handler!=null? handler.getStatusMessage() : "";
        }

        public Node getTaskNode() 
        {
            return AbstractAsyncDataPipe.this;
        }

        public String getStatusMessage()
        {
            return handler==null? "" : handler.getStatusMessage();
        }

        public void run()
        {
            status.set(HandlerStatus.EXECUTING);
            try
            {
                try
                {
                    if (handleDataInSeparateThread)
                        taskThread = Thread.currentThread();
                    Object resData = handler.handleData(
                            data, dataSource, dataContext, AbstractAsyncDataPipe.this);
                    if (SKIP_DATA!=resData)
                        sendDataToConsumers(resData, dataContext);
                }
                finally
                {
                    busy.set(false);
                    if (handlersLock.writeLock().tryLock(MAX_HANDLERS_LOCK_WAIT, TimeUnit.MILLISECONDS))
                    {
                        try{
                            waitForHandlerFree.signal();
                        }finally{
                            handlersLock.writeLock().unlock();
                        }
                    }
                    taskThread = null;
                    status.set(HandlerStatus.WAITING);
                }
            } 
            catch (Exception ex)
            {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error("Error handling data", ex);
            }
        }
    }
}

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.raven.annotations.Parameter;
import org.raven.ds.DataHandler;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractAsyncDataPipe extends AbstractSafeDataPipe
{
    public static final int MAX_HANDLERS_LOCK_WAIT = 1000;
    @NotNull @Parameter(defaultValue="2")
    private Integer maxHandlersCount;

    @NotNull @Parameter(defaultValue="60")
    private Integer handlerLifeTime;

    @NotNull @Parameter(defaultValue="true")
    private Boolean waitForHandler;
    
    @NotNull @Parameter(defaultValue="60")
    private Integer waitForHandlerTimeout;

    @NotNull @Parameter
    private ExecutorService executor;

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
    protected void doSetData(DataSource dataSource, Object data) throws Exception
    {
        if (handlersLock.writeLock().tryLock(MAX_HANDLERS_LOCK_WAIT, TimeUnit.MILLISECONDS))
        {
            try
            {
                boolean res = false;
                if (!(res=processData(data)) && waitForHandler)
                {
                    if (waitForHandlerFree.await(waitForHandlerTimeout, TimeUnit.MILLISECONDS))
                        res = processData(data);
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

    private boolean processData(Object data) throws Exception
    {
        for (HandlerInfo handlerInfo: handlers)
            if (handlerInfo.handleData(data))
                return true;
        if (handlers.size()<maxHandlersCount)
        {
            HandlerInfo handlerInfo = new HandlerInfo();
            handlers.add(handlerInfo);
            handlerInfo.handleData(data);
            return true;
        }

        return false;
    }

    public Integer getHandlerLifeTime() {
        return handlerLifeTime;
    }

    public void setHandlerLifeTime(Integer handlerLifeTime) {
        this.handlerLifeTime = handlerLifeTime;
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

    private class HandlerInfo implements Task
    {
        private long creationTime;
        private AtomicBoolean busy = new AtomicBoolean(false);
        private DataHandler handler;
        private Object data;

        public boolean handleData(Object data) throws ExecutorServiceException
        {
            if (!busy.compareAndSet(false, true))
                return false;
            if (handler==null || (System.currentTimeMillis()-handlerLifeTime*1000)>creationTime)
            {
                if (handler!=null)
                    release();
                creationTime = System.currentTimeMillis();
                handler = createDataHandler();
            }

            this.data = data;

            try
            {
                executor.execute(this);
            }
            catch(ExecutorServiceException e)
            {
                busy.set(false);
                throw e;
            }
            return true;
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
            try
            {
                try
                {
                    Object resData = handler.handleData(data, AbstractAsyncDataPipe.this);
                    sendDataToConsumers(resData);
                }
                finally
                {
                    busy.set(false);
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

/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.ds.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class QueueDataPipe extends AbstractSafeDataPipe {
    public enum QueueType {ACTIVE, ACTIVE_ON_FULL, PASSIVE}
    
    @NotNull @Parameter(defaultValue="512")
    private Integer queueSize;
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @NotNull @Parameter(defaultValue="ACTIVE")
    private QueueType queueType;
    
    @Parameter
    private Integer dataLifetime;
    
    @NotNull @Parameter(defaultValue="SECONDS")
    private TimeUnit dataLifetimeUnit;
    
    private volatile Worker worker;
    
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        worker = new Worker(queueSize, queueType, executor);
    }

    @Override
    protected void doStop() throws Exception {
        Worker aworker = worker;
        if (aworker!=null) {
            aworker.queue.clear();
            worker = null;
        }
        super.doStop();
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        Worker aworker = worker;
        if (aworker!=null)
            aworker.offer(data, context);
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, 
        BindingSupport bindingSupport) 
    {
    }
    
    @Parameter(readOnly=true)
    public Integer getMaxQueueSize() {
        Worker aworker = worker;
        return aworker!=null? aworker.maxQueueSize : null;
    }
    
    @Parameter(readOnly=true)
    public Integer getCurrentQueueSize() {
        Worker aworker = worker;
        return aworker!=null? aworker.queue.size() : null;
    }
    
    public Integer getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public QueueType getQueueType() {
        return queueType;
    }

    public void setQueueType(QueueType queueType) {
        this.queueType = queueType;
    }

    public Integer getDataLifetime() {
        return dataLifetime;
    }

    public void setDataLifetime(Integer dataLifetime) {
        this.dataLifetime = dataLifetime;
    }

    public TimeUnit getDataLifetimeUnit() {
        return dataLifetimeUnit;
    }

    public void setDataLifetimeUnit(TimeUnit dataLifetimeUnit) {
        this.dataLifetimeUnit = dataLifetimeUnit;
    }
    
    private class Worker implements Task {
        public static final int OFFER_TIMEOUT = 100;
        
        private final BlockingQueue<DataWrapper> queue;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicBoolean flushing = new AtomicBoolean(false);
        private final QueueType queueType;
        private final ExecutorService executor;
        private volatile int maxQueueSize = 0;

        public Worker(int queueSize, QueueType queueType, ExecutorService executor) {
            this.queue = new ArrayBlockingQueue<DataWrapper>(queueSize);
            this.queueType = queueType;
            this.executor = executor;
        }
        
        public Node getTaskNode() {
            return QueueDataPipe.this;
        
        }
        public String getStatusMessage() {
            return "Pushing data to consumers";
        }
        
        public void offer(Object data, DataContext context) throws Exception {
            DataWrapper dataWrapper = new DataWrapper(data, context);
            switch (queueType) {
                case ACTIVE        : processOfferForActive(dataWrapper); break;
                case ACTIVE_ON_FULL: processOfferForActiveOnFull(dataWrapper); break;
                case PASSIVE       : processOfferForPassive(dataWrapper); break;
            }
        }
        
        public void processOfferForActive(DataWrapper dataWrapper) throws Exception {            
            offerWithTimeout(dataWrapper);
            processQueueSizeStat();
            if (running.compareAndSet(false, true))
                if (!executor.executeQuietly(this)) 
                    running.set(false);
        }
        
        private void processOfferForActiveOnFull(DataWrapper dataWrapper) throws Exception {
            if (!queue.offer(dataWrapper)) {
                flushQueue();
                offerWithTimeout(dataWrapper);
            }
            if (maxQueueSize < queueSize) processQueueSizeStat();
        }
        
        private void processOfferForPassive(DataWrapper dataWrapper) throws Exception {
            offerWithTimeout(dataWrapper);
            processQueueSizeStat();
        }
        
        public void run() {
            try {
                while(true) {
                    DataWrapper data;
                    while ( (data=queue.poll(1, TimeUnit.SECONDS)) != null) 
                        sendDataToConsumers(data.data, data.context);
                    running.set(false);
                    if (queue.peek()==null || !running.compareAndSet(false, true))
                        return;
                } 
            } catch(Exception e) {
                running.set(false);
                if (isLogLevelEnabled(LogLevel.WARN))
                    getLogger().warn("Data push task interrupted", e);
            }
        }
        
        private void flushQueue() throws Exception {
            if (!flushing.compareAndSet(false, true))
                return;
            try {
                final List<DataWrapper> dataList = new ArrayList<DataWrapper>(queueSize);
                queue.drainTo(dataList);
                executor.execute(new AbstractTask(QueueDataPipe.this, "Flushing data from queue to consumers") {
                    @Override public void doRun() throws Exception {
                        for (DataWrapper dataWrapper: dataList)
                            sendDataToConsumers(dataWrapper.data, dataWrapper.context);
                        sendDataToConsumers(null, new DataContextImpl());
                    }
                });
            } finally {
                flushing.set(false);
            }
        }

        private void offerWithTimeout(DataWrapper dataWrapper) throws Exception {
            if (!queue.offer(dataWrapper, OFFER_TIMEOUT, TimeUnit.MILLISECONDS)) 
                throw new Exception("Queue exhausted. Received DATA discarded");
        }

        private void processQueueSizeStat() {
            int queueSize = queue.size();
            if (queueSize > maxQueueSize)
                maxQueueSize = queueSize;
        }
    }

    private class DataWrapper {
        private final Object data;
        private final DataContext context;

        public DataWrapper(Object data, DataContext context) {
            this.data = data;
            this.context = context;
        }
    }
}

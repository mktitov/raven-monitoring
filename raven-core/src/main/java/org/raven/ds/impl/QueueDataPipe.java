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
import java.util.concurrent.locks.ReentrantLock;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
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
public class QueueDataPipe extends AbstractSafeDataPipe implements Schedulable {
    public enum QueueType {ACTIVE, ACTIVE_ON_FULL, PASSIVE}
    
    @NotNull @Parameter(defaultValue="512")
    private Integer queueSize;
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @NotNull @Parameter(defaultValue="ACTIVE")
    private QueueType queueType;
    
    @NotNull @Parameter(defaultValue="0")
    private Integer dataLifetime;
    
    @NotNull @Parameter(defaultValue="SECONDS")
    private TimeUnit dataLifetimeUnit;
    
    @NotNull @Parameter(defaultValue="1")
    private Integer dataCountThreshold;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean forwardPullRequest;
    
    @Parameter(valueHandlerType = SystemSchedulerValueHandlerFactory.TYPE)
    private Scheduler flushScheduler;
    
//    @NotNull @Parameter(defaultValue = "0")
//    private Long flushTimePeriod;    
//    
//    @NotNull @Parameter(defaultValue = "SECONDS")
//    private TimeUnit flushTimePeriodTimeUnit;
    
    private volatile Worker worker;
    private ReentrantLock lock;

    @Override
    protected void initFields() {
        super.initFields();
        lock = new ReentrantLock();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        worker = new Worker(queueSize, queueType, executor, dataCountThreshold, 
            dataLifetimeUnit.toMillis(dataLifetime));
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
    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        if (!isStarted()) return false;
        try {
            Worker _worker = worker;
            if (_worker!=null) _worker.flushQueue();
        } catch (Exception e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error("Error flushing queue", e);
        }
        return forwardPullRequest? super.getDataImmediate(dataConsumer, context) : true;
    }

    public void executeScheduledJob(Scheduler scheduler) {
        try {
            if (lock.tryLock()) {
                try {
                    Worker _worker = worker;
                    if (_worker != null)
                        _worker.flushQueue();
                } finally {
                    lock.unlock();
                }
            }
        } catch (Exception ex) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error("Error executing flush scheduler", ex);
        }
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

    public Integer getDataCountThreshold() {
        return dataCountThreshold;
    }

    public void setDataCountThreshold(Integer dataCountThreshold) {
        this.dataCountThreshold = dataCountThreshold;
    }

    public Boolean getForwardPullRequest() {
        return forwardPullRequest;
    }

    public void setForwardPullRequest(Boolean forwardPullRequest) {
        this.forwardPullRequest = forwardPullRequest;
    }

    public Scheduler getFlushScheduler() {
        return flushScheduler;
    }

    public void setFlushScheduler(Scheduler flushScheduler) {
        this.flushScheduler = flushScheduler;
    }

    private class Worker implements Task {
        public static final int OFFER_TIMEOUT = 100;
        
        private final BlockingQueue<DataWrapper> queue;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicBoolean flushing = new AtomicBoolean(false);
        private final AtomicBoolean deleting = new AtomicBoolean(false);
        private final QueueType queueType;
        private final int dataCountThreshold;
        private final ExecutorService executor;
        private final long lifetime;
        private volatile int maxQueueSize = 0;

        public Worker(int queueSize, QueueType queueType, ExecutorService executor, int dataCountThreshold, 
            long lifetime) 
        {
            this.queue = new ArrayBlockingQueue<DataWrapper>(queueSize);
            this.queueType = queueType;
            this.executor = executor;
            this.dataCountThreshold = dataCountThreshold;
            this.lifetime = lifetime;
        }
        
        public Node getTaskNode() {
            return QueueDataPipe.this;
        
        }
        public String getStatusMessage() {
            return "Pushing data to consumers";
        }
        
        public void offer(Object data, DataContext context) throws Exception {
            DataWrapper dataWrapper = new DataWrapper(data, context, System.currentTimeMillis());
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
            executeDeleteTask(lifetime);
            if (maxQueueSize < queueSize) processQueueSizeStat();
        }
        
        private void processOfferForPassive(DataWrapper dataWrapper) throws Exception {
            offerWithTimeout(dataWrapper);
            executeDeleteTask(lifetime);
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
            if (!flushing.compareAndSet(false, true)) {
                if (isLogLevelEnabled(LogLevel.WARN))
                    getLogger().warn("Already flushing");
                return;
            }
//            try {
                List<DataWrapper> dataList;
                synchronized(queue) {
                    if (queue.size()<dataCountThreshold) {
                        flushing.set(false);
                        return;
                    }
                    dataList = new ArrayList<DataWrapper>(queueSize);
                    queue.drainTo(dataList);
                }
                final List<DataWrapper> _dataList = dataList;
                if (dataList.size()>=dataCountThreshold)
                    try {
                        executor.execute(new AbstractTask(QueueDataPipe.this, "Flushing data from queue to consumers") {
                            @Override public void doRun() throws Exception {
                                try {
                                    for (DataWrapper dataWrapper: _dataList)
                                        sendDataToConsumers(dataWrapper.data, dataWrapper.context);
                                    sendDataToConsumers(null, new DataContextImpl());
                                } finally {
                                    flushing.set(false);
                                }
                            }
                        });
                    } catch (ExecutorServiceException e) {
                        flushing.set(false);
                    }
                else 
                    flushing.set(false);
//            } finally {
////                flushing.set(false);
//            }
        }
        
        private void executeDeleteTask(long interval) {
            if (lifetime<=0 || !deleting.compareAndSet(false, true))
                return;
            try {
                executor.execute(interval, new AbstractTask(QueueDataPipe.this, "Deleting old data in queue") {
                    @Override public void doRun() throws Exception {
                        DataWrapper dataWrapper = null;
                        long timeBound = System.currentTimeMillis()-lifetime;
                        try {
                            synchronized(queue) {
                                dataWrapper = queue.peek();
                                while (dataWrapper!=null && dataWrapper.created<timeBound) {
                                    queue.poll();
                                    dataWrapper = queue.peek();
                                }
                            }
                        } finally {
                            deleting.set(false);
                        }
                        if (dataWrapper!=null) 
                            executeDeleteTask(dataWrapper.created-timeBound);
                    }
                });
            } catch (ExecutorServiceException e) {
                deleting.set(false);
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error("Error executing delete task");
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
        private final long created;

        public DataWrapper(Object data, DataContext context, long created) {
            this.data = data;
            this.context = context;
            this.created = created;
        }
    }
}

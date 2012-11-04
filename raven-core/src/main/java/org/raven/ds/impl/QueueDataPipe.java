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
import org.raven.sched.Task;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class QueueDataPipe extends AbstractSafeDataPipe {
    
    @NotNull @Parameter(defaultValue="512")
    private Integer queueSize;
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    private volatile Worker worker;
    
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        worker = new Worker(queueSize);
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
    
    private class Worker implements Task {
        private final BlockingQueue<DataWrapper> queue;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private volatile int maxQueueSize = 0;

        public Worker(int queueSize) {
            this.queue = new ArrayBlockingQueue<DataWrapper>(queueSize);
        }
        
        public Node getTaskNode() {
            return QueueDataPipe.this;
        
        }
        public String getStatusMessage() {
            return "Pushing data to consumers";
        }
        
        public void offer(Object data, DataContext context) throws Exception {
            if (!queue.offer(new DataWrapper(data, context), 100, TimeUnit.MILLISECONDS))
                throw new Exception("Queue exhausted. Received DATA discarded");
            int queueSize = queue.size();
            if (queueSize > maxQueueSize)
                maxQueueSize = queueSize;
            if (running.compareAndSet(false, true))
                if (!executor.executeQuietly(this)) 
                    running.set(false);
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

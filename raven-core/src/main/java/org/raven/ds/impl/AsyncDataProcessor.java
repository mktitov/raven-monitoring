/*
 * Copyright 2015 Mikhail Titov.
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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.raven.ds.DataProcessor;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;


/**
 *
 * @author Mikhail Titov
 */
public class AsyncDataProcessor<T> implements DataProcessor<T> {
    protected final Node owner;
    protected final DataProcessor<T> processor;
    protected final ExecutorService executor;
    private final Queue<T> queue;
    private final AtomicBoolean running;
    private final Task task;
    private final int maxMessageProcessTries;
    private final int maxExecuteMessageDispatcherTies;
    protected final LoggerHelper logger;
    private final AtomicBoolean terminated = new AtomicBoolean();

    public AsyncDataProcessor(AsyncDataProcessorConfig<T> config) {
        this.owner = config.getOwner();
        this.processor = config.getProcessor();
        this.executor = config.getExecutor();
        this.queue = config.getQueue()!=null? config.getQueue() : new ConcurrentLinkedQueue<T>();
        this.maxMessageProcessTries = config.getMaxMessageProcessTries();
        this.maxExecuteMessageDispatcherTies = config.getMaxExecuteMessageDispatcherTies();
        this.running = new AtomicBoolean();
        this.task = new Task(owner, "Processing messages");
        this.logger = config.getLogger();
    }
    
    public void terminate() {
        terminated.compareAndSet(false, true);
    }
    
    public boolean isTerminated() {
        return terminated.get();
    }
    
    public boolean processData(T dataPacket) {
        if (terminated.get())
            return false;
        final boolean res = queue.offer(dataPacket);
        if (res && running.compareAndSet(false, true)) { 
            boolean executed;
            int cnt = 0;
            while ( !(executed = executor.executeQuietly(task)) && cnt++<maxExecuteMessageDispatcherTies) ;            
            if (!executed) {
                running.set(false);
                if (logger.isErrorEnabled())
                    logger.error("Error executing message dispatcher task");
            }
        }
        return res;
    }
    
    private class Task extends AbstractTask {

        public Task(Node taskNode, String status) {
            super(taskNode, status);
        }

        @Override
        public void doRun() throws Exception {
            boolean stop = false;
            while (!stop)
                try {
                    T dataPacket;
                    int processTry = 0;
                    while ( !terminated.get() && (dataPacket=queue.peek()) != null ) {
                        boolean processed = processor.processData(dataPacket);
                        if (processed) {
                            queue.poll();
                            processTry = 0;
                        } else {
                            if (processTry++ > maxMessageProcessTries) {
                                queue.poll();
                                if (logger.isErrorEnabled())
                                    logger.error("The maximum number of attempts reached to process the message: "+dataPacket);
                            }
                        }
                    }
                } finally {
                    running.set(false);
                    if (terminated.get() || queue.isEmpty() || !running.compareAndSet(false, true))
                        stop = true;
                }
        }        
    }
}

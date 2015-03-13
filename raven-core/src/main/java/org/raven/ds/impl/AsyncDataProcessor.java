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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.raven.ds.DataProcessor;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;


/**
 *
 * @author Mikhail Titov
 */
public class AsyncDataProcessor<T> implements DataProcessor<T> {
    private final Node owner;
    private final DataProcessor<T> processor;
    private final ExecutorService executor;
    private final Queue<T> queue;
    private final AtomicBoolean running;
    private final Task task;

    public AsyncDataProcessor(Node owner, DataProcessor<T> processor, ExecutorService executor) {
        this(owner, processor, executor, new ConcurrentLinkedQueue<T>());
    }

    public AsyncDataProcessor(Node owner, DataProcessor<T> processor, ExecutorService executor, int queueSize) {
        this(owner, processor, executor, new LinkedBlockingQueue<T>(queueSize));
    }

    public AsyncDataProcessor(Node owner, DataProcessor<T> processor, ExecutorService executor, Queue<T> queue) {
        this.owner = owner;
        this.processor = processor;
        this.executor = executor;
        this.queue = queue;
        this.running = new AtomicBoolean();
        this.task = new Task(owner, "Processing data async");
    }
    
    public boolean processData(T dataPacket) throws Exception {
        final boolean res = queue.offer(dataPacket);
        if (res && running.compareAndSet(false, true)) 
            if (!executor.executeQuietly(task))
                running.set(false);
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
                    while ( (dataPacket=queue.peek()) != null ) {
                        boolean processed = processor.processData(dataPacket);
                        if (processed)
                            queue.poll();
                    }
                } finally {
                    running.set(false);
                    if (queue.isEmpty() || !running.compareAndSet(false, true))
                        stop = true;
                }
        }        
    }
}

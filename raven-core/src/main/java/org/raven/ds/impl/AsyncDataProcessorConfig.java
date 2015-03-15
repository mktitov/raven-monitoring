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
import java.util.concurrent.LinkedBlockingQueue;
import org.raven.ds.DataProcessor;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class AsyncDataProcessorConfig<T> {
    private final Node owner;
    private final DataProcessor<T> processor;
    private final ExecutorService executor;
    private final LoggerHelper logger;
    private Queue<T> queue;
    private int maxMessageProcessTries = 1;
    private int maxExecuteMessageDispatcherTies = 1;

    public AsyncDataProcessorConfig(Node owner, DataProcessor<T> processor, ExecutorService executor, LoggerHelper logger) {
        this.owner = owner;
        this.processor = processor;
        this.executor = executor;
        this.logger = logger;
    }
    
    public AsyncDataProcessorConfig<T> withQueueSize(int size) {
        queue = new LinkedBlockingQueue<T>(size);
        return this;
    }
    
    public AsyncDataProcessorConfig<T> withQueue(Queue<T> queue) {
        this.queue = queue;
        return this;
    }
    
    public AsyncDataProcessorConfig<T> withMaxMessageProcessTries(int maxMessageProcessTries) {
        this.maxMessageProcessTries = maxMessageProcessTries;
        return this;
    }
    public AsyncDataProcessorConfig<T> withMaxExecuteMessageDispatcherTies(int maxExecuteMessageDispatcherTies) {
        this.maxExecuteMessageDispatcherTies = maxExecuteMessageDispatcherTies;
        return this;
    }

    public Node getOwner() {
        return owner;
    }

    public DataProcessor<T> getProcessor() {
        return processor;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public LoggerHelper getLogger() {
        return logger;
    }

    public Queue<T> getQueue() {
        return queue;
    }

    public int getMaxMessageProcessTries() {
        return maxMessageProcessTries;
    }

    public int getMaxExecuteMessageDispatcherTies() {
        return maxExecuteMessageDispatcherTies;
    }
    
}

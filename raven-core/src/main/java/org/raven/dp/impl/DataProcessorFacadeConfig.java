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
package org.raven.dp.impl;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class DataProcessorFacadeConfig<T> {
    private final Node owner;
    private final DataProcessor<T> processor;
    private final ExecutorService executor;
    private final LoggerHelper logger;
    private Queue<T> queue;
    private int maxMessageProcessTries = 1;
    private int maxExecuteMessageDispatcherTies = 1;

    public DataProcessorFacadeConfig(Node owner, DataProcessor<T> processor, ExecutorService executor, LoggerHelper logger) {
        this.owner = owner;
        this.processor = processor;
        this.executor = executor;
        this.logger = logger;
    }
    
    public DataProcessorFacadeConfig<T> withQueueSize(int size) {
        queue = new LinkedBlockingQueue<T>(size);
        return this;
    }
    
    public DataProcessorFacadeConfig<T> withQueue(Queue<T> queue) {
        this.queue = queue;
        return this;
    }
    
    public DataProcessorFacadeConfig<T> withMaxMessageProcessTries(int maxMessageProcessTries) {
        this.maxMessageProcessTries = maxMessageProcessTries;
        return this;
    }
    public DataProcessorFacadeConfig<T> withMaxExecuteMessageDispatcherTies(int maxExecuteMessageDispatcherTies) {
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
    
    public DataProcessorFacade build() {
        return new DataProcessorFacadeImpl(this);
    }
    
}

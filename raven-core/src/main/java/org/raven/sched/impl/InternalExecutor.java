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
package org.raven.sched.impl;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import org.raven.sched.Executor;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class InternalExecutor implements Executor {
    private final Logger logger;
    private final ForkJoinPool executor;
    private final ForkJoinExecutorThreadFactory threadFactory;
    private final DelayQueue<DelayedTaskWrapper> delayedTasks;

    public InternalExecutor(String name, int parallelism, Logger logger) {
        this.logger = logger;
        this.threadFactory = new ForkJoinExecutorThreadFactory(name, true);
        this.executor = new ForkJoinPool(parallelism, threadFactory, null, true);
        this.delayedTasks = new DelayQueue<>();
        Thread thread = new Thread(threadFactory, new DelayedTaskExecutor(), "Delayed tasks executor");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void execute(Task task) throws ExecutorServiceException {
        try {
            executor.execute(task);
        } catch (Throwable e) {
            final String message = "Error executing task"+task.getTaskNode().getPath();
            if (logger.isErrorEnabled())
                logger.error(message, e);
            throw new ExecutorServiceException(message, e);
        }
    }

    @Override
    public void execute(long delay, Task task) throws ExecutorServiceException {
        delayedTasks.add(new DelayedTaskWrapper(task, delay, delayedTasks));        
    }

    @Override
    public boolean executeQuietly(Task task) {
        try {
            execute(task);
            return true;
        } catch(Throwable e) {
            return false;
        }
    }

    @Override
    public boolean executeQuietly(long delay, Task task) {
        delayedTasks.add(new DelayedTaskWrapper(task, delay, delayedTasks));
        return true;
    }
    
    private class DelayedTaskExecutor implements Runnable {
        @Override
        public void run() {
            try {
                for (;;) {
                    DelayedTaskWrapper delayedTask = delayedTasks.poll(1, TimeUnit.SECONDS);
                    if (delayedTask!=null)
                        executeQuietly(delayedTask.getTask());
                }
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
    }
}
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

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicReference;
import org.raven.sched.ExecutorTask;
import org.raven.sched.ExecutorTaskHolder;

/**
 *
 * @author Mikhail Titov
 */
public class ForkJoinExecutorThreadFactory extends ExecutorThreadGroup implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    private final boolean createDaemonThreads;

    public ForkJoinExecutorThreadFactory(String groupName) {
        this(groupName, false);
    }

    public ForkJoinExecutorThreadFactory(String groupName, boolean createDaemonThreads) {
        super(groupName);
        this.createDaemonThreads = createDaemonThreads;
    }

    @Override
    public ForkJoinWorkerThread newThread(final ForkJoinPool pool) {
        final AtomicReference<ExecutorThread> newThread = new AtomicReference<>();
        try {
            final Thread creator = new Thread(this, new Runnable() {
                @Override public void run() {
                    newThread.set(new ExecutorThread(pool));
                }
            });
            creator.start();
            creator.join();
        } catch (InterruptedException ex) {
        }
        return newThread.get();
    }
    
    
    private class ExecutorThread extends ForkJoinWorkerThread implements ExecutorTaskHolder {
        private volatile ExecutorTask task;

        public ExecutorThread(ForkJoinPool pool) {
            super(pool);
            this.setDaemon(createDaemonThreads);
        }

        @Override
        public void setExecutorTask(ExecutorTask task) {
            this.task = task;
        }

        @Override
        public ExecutorTask getExecutorTask() {
            return task;
        }
    }
    
}

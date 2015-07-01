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

import java.util.concurrent.ThreadFactory;
import org.raven.sched.ExecutorTask;
import org.raven.sched.ExecutorTaskHolder;

/**
 *
 * @author Mikhail Titov
 */
public class ThreadPoolExecutorThreadFactory extends ExecutorThreadGroup implements ThreadFactory 
{

    public ThreadPoolExecutorThreadFactory(String threadGroupName) {
        super(threadGroupName);
    }

    @Override
    public Thread newThread(Runnable r) {
        return new ExecutorThread(r);
    }
    
    public class ExecutorThread extends Thread implements ExecutorTaskHolder {
        private volatile ExecutorTask task;

        public ExecutorThread(Runnable target) {
            super(ThreadPoolExecutorThreadFactory.this, target);
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

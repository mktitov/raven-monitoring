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

import java.util.concurrent.ForkJoinTask;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorTask;
import org.raven.sched.ExecutorTaskHolder;
import org.raven.sched.ForkJoinTaskSupport;
import org.raven.sched.TaskExecutionListener;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractExecutorTask implements ExecutorTask, ForkJoinTaskSupport {
//public abstract class AbstractExecutorTask implements ExecutorTask {
    private TaskExecutionListener statListener;
    private long executionStart;
    private long scheduleTime;
    private Thread thread;
    
    private volatile FJTask fjTask;

    @Override
    public void init(TaskExecutionListener statListener) {
        this.statListener = statListener;
        this.scheduleTime = System.nanoTime();
    }

    @Override
    public ForkJoinTask getForJoinTask() {
        if (fjTask==null)
            fjTask = new FJTask();
        return fjTask.getAndSetNext();
    }

    @Override
    public long getScheduleTime() {
        return scheduleTime;
    }

    @Override
    public long getExecutionStart() {
        return executionStart;
    }
    
    @Override
    public long getExecutionDuration() {
        return System.nanoTime() - executionStart;
    }

    @Override
    public Thread getThread() {
        return thread;
    }

//    @Override
//    public Object getRawResult() {
//        return null;
//    }
//
//    @Override
//    protected void setRawResult(Object value) {
//        
//    }
//
//    @Override
//    protected boolean exec() {
//        run();
//        return true;
//    }
//    
    @Override
    public void run() {
        final TaskExecutionListener _listener = statListener;
        executionStart = System.nanoTime();
        thread = Thread.currentThread();
        if (thread instanceof ExecutorTaskHolder)
            ((ExecutorTaskHolder)thread).setExecutorTask(this);
        try {
            try {
                if (_listener!=null)
                    _listener.taskExecutionStarted(scheduleTime, executionStart);
                doRun();
            } finally {
                if (thread instanceof ExecutorTaskHolder)
                    ((ExecutorTaskHolder)thread).setExecutorTask(null);
                if (_listener!=null)
                    _listener.taskExecutionFinished(scheduleTime, scheduleTime);
            }
        } catch(Throwable e) {
            if (getTaskNode().isLogLevelEnabled(LogLevel.ERROR))
                getTaskNode().getLogger().error(String.format("Error executing (%s)", getStatusMessage()), e);
        }
    }

    public abstract void doRun() throws Exception;
 
    private class FJTask extends ForkJoinTask {
        private final FJTask next;
        private volatile boolean isNew = true;

        public FJTask() {
            next = new FJTask(new FJTask(this));
        }
        
        private FJTask prepareNext(final FJTask stopper) {
//            if (isNew) return this;
            if (isNew || isDone()) {
//                reinitialize();
                return this;
            } else {
                if (next==stopper) {
                    join();
                    return this;
                } else 
                    return next.prepareNext(stopper);
            }
        }
        
        public FJTask(FJTask next) {
            this.next = next;
        }

        @Override
        public Object getRawResult() {
            return null;
        }

        @Override
        protected void setRawResult(Object value) {
        }

        @Override
        protected boolean exec() {
            isNew = false;
            run();
            return true;
        }

        private ForkJoinTask getAndSetNext() {
            fjTask = next;
//            if (fjTask==null)
//                throw new RavenRuntimeException("Error assigning fork join task to task: "+this);
            if (!isNew) {
                if (!isDone()) {
//                    System.out.println(" >>>> JOIN <<<<");
                    join();
                }
                reinitialize();
            }
            return this;
        }
    }
}

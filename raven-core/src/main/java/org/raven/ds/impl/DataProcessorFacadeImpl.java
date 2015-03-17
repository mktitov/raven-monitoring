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

import java.util.concurrent.atomic.AtomicReference;
import org.raven.ds.DataProcessorFacade;
import org.raven.ds.DataProcessorLogic;
import org.raven.ds.TimeoutMessage;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class DataProcessorFacadeImpl extends AsyncDataProcessor implements DataProcessorFacade {
    public final static TimeoutMessage TIMEOUT_MESSAGE = new TimeoutMessage() {
        @Override public String toString() {
            return "TIMEOUT_MESSAGE";
        }
    };
    private volatile long lastMessageTs = System.currentTimeMillis();
    
    private final AtomicReference<CheckTimeoutTask> checkTimeoutTask = new AtomicReference<CheckTimeoutTask>();

    public DataProcessorFacadeImpl(AsyncDataProcessorConfig config) {
        super(config);
        if (processor instanceof DataProcessorLogic)
            ((DataProcessorLogic)processor).setFacade(this);
    }

    @Override
    public boolean processData(Object dataPacket) {
        if (super.processData(dataPacket)) {
            if (checkTimeoutTask.get()!=null)
                lastMessageTs = System.currentTimeMillis();
            return true;
        } else
            return false;
    }

    public boolean send(Object message) {
        if (processData(message))
            return true;
        else {
            if (logger.isErrorEnabled())
                logger.error("Message sending error. Message not queued. Message: "+message);
            return false;            
        }
    }

    public void sendDelayed(long delay, final Object message) throws ExecutorServiceException {
        executor.execute(delay, new AbstractTask(owner, "Delaying message before send") {            
            @Override public void doRun() throws Exception {
                send(message);
            }
        });
    }

    public void sendRepeatedly(final long delay, final long interval, final int times, final Object message) throws ExecutorServiceException {
        if (times < 0 || isTerminated())
            return;
        AbstractTask task = new AbstractTask(owner, "Sending message repeatedly") {
            private int cnt=0;
            private final long initialTs = System.currentTimeMillis();
            
            @Override public void doRun() throws Exception {
                send(message);
                cnt++;
                if (times==0 || cnt<times) {
                    final long nextSendTs = initialTs+delay+cnt*interval;
                    final long correctedDelay = nextSendTs - System.currentTimeMillis();
                    if (correctedDelay>0)
                        executeDelayedTask(interval, this);
                    else
                        executeTask(this);
                }
            }
        };        
        executor.execute(delay, task);
    }
    
    private void executeDelayedTask(long delay, Task task) {
        try {
            if (!isTerminated())
                executor.execute(delay, task);
        } catch (ExecutorServiceException e) {
            if (logger.isErrorEnabled())
                logger.error("Error executing delayed task", e);
        }
    }

    private void executeTask(Task task) {
        try {
            if (!isTerminated())
                executor.execute(task);
        } catch (ExecutorServiceException e) {
            if (logger.isErrorEnabled())
                logger.error("Error executing delayed task", e);
        }
    }

    public void setTimeout(long timeout, long checkTimeoutInterval)  throws ExecutorServiceException {
        lastMessageTs = System.currentTimeMillis();
        final CheckTimeoutTask newTask = new CheckTimeoutTask(owner, timeout, checkTimeoutInterval);
        final CheckTimeoutTask oldTask = checkTimeoutTask.getAndSet(newTask);
        if (oldTask!=null)
            oldTask.cancel();      
        executor.execute(checkTimeoutInterval, newTask);
    }
    
    private class CheckTimeoutTask extends AbstractTask {
        private final long timeout;    
        private final long checkTimeoutInterval;

        public CheckTimeoutTask(Node taskNode, long timeout, long checkTimeoutInterval) {
            super(taskNode, "Checking waiting for message timeout");
            this.timeout = timeout;
            this.checkTimeoutInterval = checkTimeoutInterval;
        }

        @Override
        public void doRun() throws Exception {
            if (lastMessageTs+timeout<=System.currentTimeMillis()) 
                send(TIMEOUT_MESSAGE);                
            executeDelayedTask(checkTimeoutInterval, this);
        }
    }
}

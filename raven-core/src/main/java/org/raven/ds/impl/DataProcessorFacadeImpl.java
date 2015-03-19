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
import java.util.concurrent.atomic.AtomicReference;
import org.raven.ds.AskCallback;
import org.raven.ds.DataProcessor;
import org.raven.ds.DataProcessorFacade;
import org.raven.ds.DataProcessorLogic;
import org.raven.ds.LoggerSupport;
import org.raven.ds.MessageQueueError;
import org.raven.ds.RavenFuture;
import org.raven.ds.TimeoutMessage;
import org.raven.ds.TimeoutMessageSelector;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class DataProcessorFacadeImpl implements  DataProcessorFacade {
    protected final Node owner;
    protected final DataProcessor processor;
    protected final ExecutorService executor;
    private final Queue queue;
    private final AtomicBoolean running;
    private final WorkerTask task;
    private final int maxExecuteMessageDispatcherTies;
    protected final LoggerHelper logger;
    private final AtomicBoolean terminated = new AtomicBoolean();
        
//    public final static TimeoutMessage TIMEOUT_MESSAGE = new TimeoutMessage() {
//        @Override public String toString() {
//            return "TIMEOUT_MESSAGE";
//        }
//    };
    private volatile long lastMessageTs = System.currentTimeMillis();
    
    private final AtomicReference<CheckTimeoutTask> checkTimeoutTask = new AtomicReference<CheckTimeoutTask>();

    public DataProcessorFacadeImpl(DataProcessorFacadeConfig config) {
        this.owner = config.getOwner();
        this.processor = config.getProcessor();
        this.executor = config.getExecutor();
        this.queue = config.getQueue()!=null? config.getQueue() : new ConcurrentLinkedQueue();
        this.maxExecuteMessageDispatcherTies = config.getMaxExecuteMessageDispatcherTies();
        this.running = new AtomicBoolean();
        this.task = new WorkerTask(owner, "Processing messages");
        this.logger = new LoggerHelper(config.getLogger(), "[DP Facade] ");
        if (processor instanceof DataProcessorLogic)
            ((DataProcessorLogic)processor).setFacade(this);
        if (processor instanceof LoggerSupport)
            ((LoggerSupport)processor).setLogger(new LoggerHelper(config.getLogger(), "[DP Logic] "));
    }

    public void terminate() {
        if (terminated.compareAndSet(false, true)) {
            if (logger.isDebugEnabled())
                logger.debug("Terminating");
        }
    }
    
    public boolean isTerminated() {
        return terminated.get();
    }    
    
    protected boolean queueMessage(Object message) {
        if (terminated.get())
            return false;
        final boolean res = queue.offer(message);
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

    public boolean send(Object message) {
        if (queueMessage(message)) {
            CheckTimeoutTask _checkTimeoutTask = checkTimeoutTask.get();
            if (_checkTimeoutTask!=null && _checkTimeoutTask.resetTimeout(message))
                lastMessageTs = System.currentTimeMillis();
            return true;
        } else {
            if (logger.isErrorEnabled())
                logger.error("Message sending error. Message not queued. Message: "+message);
            return false;            
        }
    }

    public boolean sendTo(DataProcessorFacade facade, Object message) {
        return facade.send(new MessageFromFacade(message, this));
    }

    public void sendDelayed(long delay, final Object message) throws ExecutorServiceException {
        executor.execute(delay, new AbstractTask(owner, "Delaying message before send") {            
            @Override public void doRun() throws Exception {
                send(message);
            }
        });
    }

    public void sendDelayedTo(DataProcessorFacade facade, long delay, Object message) 
            throws ExecutorServiceException 
    {
        facade.sendDelayed(delay, new MessageFromFacade(message, this));
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

    public void sendRepeatedlyTo(DataProcessorFacade facade, long delay, long interval, int times, Object message) 
            throws ExecutorServiceException 
    {
        facade.sendRepeatedly(delay, interval, times, new MessageFromFacade(message, this));
    }
    
    private RavenFuture sendAskFuture(Object message, AskCallback callback) {
        final AskFuture future = new AskFuture(message, callback);
        if (!send(future))
            future.setError(new MessageQueueError());
        return future;
    }

    public RavenFuture ask(Object message) {
        return sendAskFuture(message, null);
    }

    public RavenFuture ask(Object message, AskCallback callback) {
        return sendAskFuture(message, callback);
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
    
    protected void sendMessageToProcessor(Object message) {
        final AskFuture future = message instanceof AskFuture? (AskFuture)message : null;
        DataProcessorFacade sender = null;
        if (future!=null)
            message = future.message;
        else if (message instanceof MessageFromFacade) {
            final MessageFromFacade messageFromFacade = (MessageFromFacade) message;
            message = messageFromFacade.message;
            sender = messageFromFacade.facade;            
        }
        if (processor instanceof DataProcessorLogic)
            ((DataProcessorLogic)processor).setSender(sender);
        try {
            final Object result = processor.processData(message);
            if (future!=null)
                future.set(result);
            else if (sender!=null && result!=DataProcessor.VOID) 
                sendTo(sender, result);
        } catch (Throwable e) {
            if (logger.isErrorEnabled())
                logger.error("Error processing message", e);
            if (future!=null)
                future.setError(e);
        }
    }
    

    public void setTimeout(long timeout, long checkTimeoutInterval)  throws ExecutorServiceException {
        setTimeout(timeout, checkTimeoutInterval, null);
    }

    public void setTimeout(long timeout, long checkTimeoutInterval, TimeoutMessageSelector selector) 
            throws ExecutorServiceException 
    {
        lastMessageTs = System.currentTimeMillis();
        final CheckTimeoutTask newTask = new CheckTimeoutTask(owner, timeout, checkTimeoutInterval, selector);
        final CheckTimeoutTask oldTask = checkTimeoutTask.getAndSet(newTask);
        if (oldTask!=null)
            oldTask.cancel();      
        executor.execute(checkTimeoutInterval, newTask);
    }

    private class CheckTimeoutTask extends AbstractTask {
        private final long timeout;    
        private final long checkTimeoutInterval;
        private final TimeoutMessageSelector selector;

        public CheckTimeoutTask(Node taskNode, long timeout, long checkTimeoutInterval, TimeoutMessageSelector selector) {
            super(taskNode, "Checking waiting for message timeout");
            this.timeout = timeout;
            this.checkTimeoutInterval = checkTimeoutInterval;
            this.selector = selector;
        }
        
        public boolean resetTimeout(Object message) {
            return selector==null || message==TIMEOUT_MESSAGE? true : selector.resetTimeout(message);
        }

        @Override
        public void doRun() throws Exception {
            if (lastMessageTs+timeout<=System.currentTimeMillis()) 
                send(TIMEOUT_MESSAGE);                
            executeDelayedTask(checkTimeoutInterval, this);
        }
    }
    
    private class WorkerTask extends AbstractTask {

        public WorkerTask(Node taskNode, String status) {
            super(taskNode, status);
        }

        @Override
        public void doRun() throws Exception {
            boolean stop = false;
            while (!stop)
                try {
                    Object message;
                    while ( !terminated.get() && (message=queue.poll()) != null ) 
                        sendMessageToProcessor(message);
                } finally {
                    running.set(false);
                    if (terminated.get() || queue.isEmpty() || !running.compareAndSet(false, true))
                        stop = true;
                }
        }        
    }
    
    private static class AskFuture extends RavenFutureImpl {
        private final Object message;

        public AskFuture(Object message, AskCallback callback) {
            super(callback);
            this.message = message;
        }        
    }
    
    private static class MessageFromFacade {
        private final Object message;
        private final DataProcessorFacade facade;

        public MessageFromFacade(Object message, DataProcessorFacade facade) {
            this.message = message;
            this.facade = facade;
        }
    }
}

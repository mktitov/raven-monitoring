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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.raven.dp.FutureCallback;
import org.raven.dp.FutureCallbackWithTimeout;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorContext;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.DataProcessorLogic;
import org.raven.dp.MessageQueueError;
import org.raven.dp.RavenFuture;
import org.raven.dp.Terminated;
import org.raven.dp.UnbecomeFailureException;
import org.raven.dp.UnhandledMessageException;
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
    private final Node owner;
    private final DataProcessor processor;
    private final Context processorContext;
    private final ExecutorService executor;
    private final Queue queue;
    private final AtomicBoolean running;
    private final WorkerTask task;
    private final DataProcessorFacade unhandledMessagesProcessor;
    private final int maxExecuteMessageDispatcherTies;
    private final LoggerHelper logger;
    private final AtomicBoolean terminated = new AtomicBoolean();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final long defaultStopTimeout;
    private final LoggerHelper origLogger;
    private final AtomicBoolean terminateSentToWatcherService = new AtomicBoolean();
    private volatile DataProcessorFacade watcherService;
        
    private volatile long lastMessageTs = System.currentTimeMillis();
    
    private final AtomicReference<CheckTimeoutTask> checkTimeoutTask = new AtomicReference<CheckTimeoutTask>();

    public DataProcessorFacadeImpl(DataProcessorFacadeConfig config) {
        this.owner = config.getOwner();
        this.processor = config.getProcessor();
        this.executor = config.getExecutor();
        this.queue = config.getQueue()!=null? config.getQueue() : new ConcurrentLinkedQueue();
        this.maxExecuteMessageDispatcherTies = config.getMaxExecuteMessageDispatcherTies();
        this.unhandledMessagesProcessor = config.getUnhandledMessageProcessor();
        this.running = new AtomicBoolean();
        this.task = new WorkerTask(owner, "Processing messages");
        this.defaultStopTimeout = config.getDefaultStopTimeout();
        this.origLogger = config.getLogger();
        this.logger = new LoggerHelper(config.getLogger(), "[DP] ");
        if (processor instanceof DataProcessorLogic) {
            this.processorContext = new Context();
            ((DataProcessorLogic)processor).init(this, processorContext);
        } else
            this.processorContext = null;
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

    public void stop() {
        send(STOP_MESSAGE);
    }
    
    public void stop(long timeoutMs) {
        send(new StopFuture().onComplete(timeoutMs, new StopCallback(null)));
    }
    
    public RavenFuture askStop() {
        return ask(STOP_MESSAGE);
    }
    
    public RavenFuture askStop(long timeoutMs) {
        StopFuture future = new StopFuture();
        future.onComplete(timeoutMs, new StopCallback(null));
        if (!send(future))
            future.setError(new MessageQueueError());
        return future;
    }

    public RavenFuture watch() {
        final WatchFuture future = new WatchFuture(executor);
        getWatcherService(true).send(future);
        return future;
    }

    public void watch(DataProcessorFacade watcher) {
        getWatcherService(true).send(watcher);
    }
    
    private DataProcessorFacade getWatcherService(final boolean create) {
        DataProcessorFacade _watcherService = watcherService;
        if (_watcherService!=null || !create)
            return _watcherService;
        else {
            synchronized(this) {
                _watcherService = watcherService;
                if (_watcherService!=null)
                    return _watcherService;
                watcherService = new DataProcessorFacadeConfig(
                                owner, new WatcherDataProcessor(), executor, new LoggerHelper(origLogger, "[Death Watcher] ")
                            ).build();
                if (isTerminated())
                    sendTerminateToWatcherService();
                return watcherService;
            }
        }
    }
    
    private void sendTerminateToWatcherService() {
        if (terminateSentToWatcherService.compareAndSet(false, true))
            watcherService.send(new TerminatedImpl(this, true));
    }
    
    private void processTermination(Object stopMessage, boolean success) {
        if (terminated.compareAndSet(false, true)) {
            if (getWatcherService(false)!=null)
                sendTerminateToWatcherService();
            if (success && processor instanceof DataProcessorLogic) 
                ((DataProcessorLogic)processor).postStop();
            if (stopMessage instanceof MessageFromFacade) 
                ((MessageFromFacade)stopMessage).facade.send(new TerminatedImpl(this, success));
            else if (stopMessage instanceof AskFuture)
                ((AskFuture)stopMessage).set(true);
        }
    }

    protected boolean queueMessage(Object message) {
        if (terminated.get() || stopping.get())
            return false;
        if (message instanceof StopFuture) {
            if (!stopping.compareAndSet(false, true)) {
                ((StopFuture)message).cancel(true);
                return false;
            }
        }
        final boolean res = queue.offer(message);
        if (stopping.get() && !res) {
            ((StopFuture)message).setError(new Exception("Normal STOP process was interrupted because of ERROR queuing STOP_MESSAGE. Terminating..."));
        } 
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
    
    private boolean isStopMessage(Object message) {
        if (message==STOP_MESSAGE)
            return true;
        if (message instanceof AskFuture && ((AskFuture)message).message==STOP_MESSAGE)
            return true;
        if (message instanceof MessageFromFacade && ((MessageFromFacade)message).message==STOP_MESSAGE)
            return true;
        return false;
    }

    public boolean send(Object message) {
        if (queueMessage(wrapToStopIfNeed(message))) {
            CheckTimeoutTask _checkTimeoutTask = checkTimeoutTask.get();
            if (_checkTimeoutTask!=null && _checkTimeoutTask.resetTimeout(message))
                lastMessageTs = System.currentTimeMillis();
            return true;
        } else {
            if (logger.isErrorEnabled()) {
                final String cause = terminated.get()? "processor was terminated" 
                        : (stopping.get()? "processor is stopping" : "messages queue is full");
                logger.error("Message sending error. Message not queued because of {}. Message: {}", cause, message);
            }
            return false;            
        }
    }
    
    private Object wrapToStopIfNeed(Object message) {
        if (isStopMessage(message))
            return new StopFuture().onComplete(defaultStopTimeout, new StopCallback(message));
        else
            return message;
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
    
//    private RavenFuture sendAskFuture(Object message, FutureCallback callback) {
//        final AskFuture future = new AskFuture(message, callback);        
//        if (!send(future))
//            future.setError(new MessageQueueError());
//        return future;
//    }

    public RavenFuture ask(Object message) {
        final AskFuture future = new AskFuture(message, executor);        
        if (!send(future))
            future.setError(new MessageQueueError());
        return future;
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
        if (message instanceof StopFuture) {
            ((StopFuture)message).set(true);
            return;
        }
        final AskFuture future = message instanceof AskFuture? (AskFuture)message : null;
        final Object origMessage = message;
        DataProcessorFacade sender = null;
        if (future!=null)
            message = future.message;
        else if (message instanceof MessageFromFacade) {
            final MessageFromFacade messageFromFacade = (MessageFromFacade) message;
            message = messageFromFacade.message;
            sender = messageFromFacade.facade;       
        }
        if (processorContext!=null) {
            processorContext.sender = sender;
            processorContext.message = message;
            processorContext.origMessage = origMessage;
            processorContext.unhandled = false;
        }
        try {
            final DataProcessor _processor = processorContext==null? processor : processorContext.currentBehaviuor;
            final Object result = _processor.processData(message);
            final boolean unhandled = processorContext!=null && processorContext.unhandled;            
            if (future!=null) {
                if (!unhandled) future.set(result);
                else future.setError(new UnhandledMessageException(message));
            }
            else if (!unhandled && sender!=null && result!=DataProcessor.VOID) 
                sendTo(sender, result);
            if (unhandled && unhandledMessagesProcessor!=null)
                unhandledMessagesProcessor.send(new UnhandledMessageImpl(sender, this, message));
        } catch (Throwable e) {
            if (logger.isErrorEnabled())
                logger.error("Error processing message: "+message, e);
            if (future!=null)
                future.setError(e);
            if (unhandledMessagesProcessor!=null)
                unhandledMessagesProcessor.send(new UnhandledMessageImpl(sender, this, message, e));
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
    
    private class Context implements DataProcessorContext {
        private DataProcessorFacade sender;
        private LoggerHelper logger;
        private Queue<DataProcessor> behaviourStack;
        private DataProcessor currentBehaviuor = processor;
        private Object origMessage;
        private Object message;
        private boolean unhandled;

        public DataProcessorFacade getSender() {
            return sender;
        }

        public LoggerHelper getLogger() {
            if (logger==null)
                logger = new LoggerHelper(origLogger, "[DP Logic: "+currentBehaviuor+"] ");
            return logger;
        }
        
        public void become(final DataProcessor dataProcessor, final boolean replace) {
            if (getLogger().isDebugEnabled())
                getLogger().debug("Behaviour "+(replace?"replaced by ":"changed on ")+dataProcessor);
            if (!replace) {
                if (behaviourStack==null)
                    behaviourStack = new LinkedList<DataProcessor>();
                behaviourStack.offer(currentBehaviuor);
            }
            currentBehaviuor = dataProcessor;
            logger = null;
        }

        public void unbecome() throws UnbecomeFailureException {
            final DataProcessor behaviour = behaviourStack==null? null : behaviourStack.poll();
            if (behaviour==null)
                throw new UnbecomeFailureException();
            logger = null;
            if (getLogger().isDebugEnabled())
                getLogger().debug("Behaviour changed from "+currentBehaviuor);
            currentBehaviuor = behaviour;
            if (behaviourStack!=null && behaviourStack.isEmpty())
                behaviourStack = null;
        }

        public void unhandled() {
            unhandled = true;
            if (getLogger().isWarnEnabled())
                getLogger().warn("Unhandled message: "+message);
        }

        public void forward(DataProcessorFacade facade) {
            facade.send(origMessage);
        }

        public void forward(DataProcessorFacade facade, Object message) {
            facade.send(sender==null? message : new MessageFromFacade(message, sender));
        }
    }
    
    private class StopFuture extends RavenFutureImpl {
        public StopFuture() {
            super(executor);
        }        
    }
    
    private class StopCallback implements FutureCallbackWithTimeout {
        private final Object message;

        public StopCallback(Object message) {
            this.message = message;
        }

        public void onTimeout() {
            processTermination(message, false);
        }

        public void onSuccess(Object askResult) {
            processTermination(message, true);
        }

        public void onError(Throwable error) {
            processTermination(message, false);
        }

        public void onCanceled() {
        }
    }
    
    private static class AskFuture extends RavenFutureImpl {
        private final Object message;

        public AskFuture(Object message, ExecutorService executor) {
            super(executor);
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
    
    private static class WatchFuture extends RavenFutureImpl {
        public WatchFuture(ExecutorService executor) {
            super(executor);
        }        
    }
    
    private static class WatcherDataProcessor extends AbstractDataProcessorLogic {
        private Terminated terminated = null;
        private List watchers;

        public Object processData(Object message) throws Exception {
            if (terminated!=null)
                sendReplay(message);
            else if (message instanceof Terminated) {
                terminated = (Terminated) message;
                notifyWatchers();
            } else {
                if (watchers==null)
                    watchers = new LinkedList();
                watchers.add(message);
            }
            return VOID;
        }
        
        private void notifyWatchers() {
            if (watchers!=null) {
                for (Object watcher: watchers)
                    sendReplay(watcher);
                watchers = null;
            }
        }
        
        private void sendReplay(Object replayDest) {
            if (replayDest instanceof WatchFuture)
                ((WatchFuture)replayDest).set(true);
            else if (replayDest instanceof DataProcessorFacade)
                ((DataProcessorFacade)replayDest).send(terminated);
        }
    }
}

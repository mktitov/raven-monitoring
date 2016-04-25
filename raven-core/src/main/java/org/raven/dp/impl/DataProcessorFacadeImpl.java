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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.raven.dp.FutureCallbackWithTimeout;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorContext;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.DataProcessorFacadeException;
import org.raven.dp.DataProcessorLogic;
import org.raven.dp.FutureCallback;
import org.raven.dp.FutureTimeoutException;
import org.raven.dp.NonUniqueNameException;
import org.raven.dp.RavenFuture;
import org.raven.dp.Stashed;
import org.raven.dp.Terminated;
import org.raven.dp.UnbecomeFailureException;
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
    private final String name;
    private final String path;
    private final DataProcessorFacade parent; 
    private final Node owner;
    private final DataProcessor processor;
    private final Context processorContext;
    private final ExecutorService executor;
    private final Queue queue;
    private final AtomicBoolean running;
    private final WorkerTask task;
    private final DataProcessorFacade unhandledMessagesProcessor;
//    private final int maxExecuteMessageDispatcherTies;
    private final LoggerHelper logger;
    private final AtomicBoolean terminated = new AtomicBoolean();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final long defaultStopTimeout;
    private final long defaultAskTimeout;
    private final LoggerHelper origLogger;
    private final AtomicBoolean terminateSentToWatcherService = new AtomicBoolean();
    private final int maxMessagesPerCycle;
    private volatile DataProcessorFacade watcherService;
        
//    private volatile long lastMessageTs = System.currentTimeMillis();
    
    private final AtomicReference<CheckTimeoutTask> checkTimeoutTask = new AtomicReference<CheckTimeoutTask>();

    public DataProcessorFacadeImpl(DataProcessorFacadeConfig config) {
        this.name = config.getFacadeName();
        this.owner = config.getOwner();
        this.parent = config.getParent();
        this.path = (parent!=null? parent.getPath()+"/" : "")+name;
        this.processor = config.getProcessor();
        this.executor = config.getExecutor();
        this.queue = config.getQueue()!=null? config.getQueue() : new ConcurrentLinkedQueue();
//        this.maxExecuteMessageDispatcherTies = config.getMaxExecuteMessageDispatcherTies();
        this.unhandledMessagesProcessor = config.getUnhandledMessageProcessor();
        this.running = new AtomicBoolean();
        this.task = new WorkerTask(owner, "Processing messages");
        this.defaultStopTimeout = config.getDefaultStopTimeout();
        this.defaultAskTimeout = config.getDefaultAskTimeout();
        this.origLogger = config.getLogger(); // 
        this.maxMessagesPerCycle = config.getMaxMessagesPerCycle();
        this.logger = new LoggerHelper(config.getLogger(), String.format("[DP %s] ", path));
        if (processor instanceof DataProcessorLogic) {
            this.processorContext = new Context();
            ((DataProcessorLogic)processor).init(this, processorContext);
        } else
            this.processorContext = null;
    }
    
    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
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
    
    public RavenFuture watch() {
        final WatchFuture future = new WatchFuture(executor);
        getWatcherService(true).send(future);
        return future;
    }

    public void watch(DataProcessorFacade watcher) {
        getWatcherService(true).send(watcher);
    }

    @Override
    public void unwatch(DataProcessorFacade watcher) {
        DataProcessorFacade _watcherService = getWatcherService(false);
        if (_watcherService!=null)
            _watcherService.send(new Unwatch(watcher));
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
                                "Death Watcher", owner, new WatcherDataProcessor(), executor, 
                                new LoggerHelper(logger, "<-> ")
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
            if (parent!=null)
                parent.send(new ChildTerminated(this));
            if (logger.isDebugEnabled())
                logger.debug(success? "Stopped" : "Terminated");
        }
    }
    
    private boolean isMessageAllowed(final Object message) {
        if (!terminated.get() && !stopping.get() && !(message instanceof StopFuture))
            return true;
        else { 
            if (terminated.get() || stopping.get()) {
                if (message instanceof StopFuture)
                    ((StopFuture)message).cancel(true);
                if (!terminated.get() && message instanceof ChildTerminated)
                    return true;
            } else  {
                if (!stopping.compareAndSet(false, true)) {
                    ((StopFuture)message).cancel(true);
                } else {
                    if (logger.isDebugEnabled()) 
                        logger.debug("Stopping...");
                    return true;
                }
            }
            if (logger.isDebugEnabled())
                logger.debug("Message sending error. Message not queued because of processor is stopping. Message: {}", message);
            return false;            
        }
    }

    private boolean queueMessage(final Object message) {
        if (!isMessageAllowed(message))
            return false;
        if (queue.offer(message)) {
            if (running.compareAndSet(false, true))
                executeTask();
            CheckTimeoutTask _checkTimeoutTask = checkTimeoutTask.get();
            if (_checkTimeoutTask!=null)
                _checkTimeoutTask.messageReceived(message);
            return true;
        } else {
            if (stopping.get())
                ((StopFuture)message).setError(new Exception("Normal STOP process was interrupted because of ERROR queuing STOP_MESSAGE. Terminating..."));
            if (logger.isErrorEnabled()) {
                logger.error("Message sending error. Message not queued because of messages queue is full. Message: {}", message);
            }
            return false;
        }
    }
    
    private void executeTask() {
        try {
            executor.execute(task);
        } catch (ExecutorServiceException ex) {
            running.set(false);
            if (logger.isErrorEnabled())
                logger.error("Error executing message dispatcher task", ex);
        }
    }
    
    @Override
    public boolean send(final Object message) {
        if (message!=STOP_MESSAGE)
            return queueMessage(message);
        else
            return queueMessage(wrapToStop(message));
    }
    
    private Object wrapToStop(Object message) {
        return new StopFuture().onComplete(defaultStopTimeout, new StopCallback(message));
    }

    @Override
    public boolean sendTo(final DataProcessorFacade facade, final Object message) {
        if (message!=STOP_MESSAGE)
            return facade.send(new MessageFromFacade(message, this));
        else 
            return facade.send(wrapToStop(new MessageFromFacade(message, this)));
    }

    @Override
    public void sendDelayed(long delay, final Object message) {
        executor.executeQuietly(delay, new AbstractTask(owner, "Delaying message before send") {            
            @Override public void doRun() throws Exception {
                send(message);
            }
        });
    }

    @Override
    public void sendDelayedTo(DataProcessorFacade facade, long delay, Object message) 
            throws ExecutorServiceException 
    {
        facade.sendDelayed(delay, new MessageFromFacade(message, this));
    }

    @Override
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

    @Override
    public void sendRepeatedlyTo(DataProcessorFacade facade, long delay, long interval, int times, Object message) 
            throws ExecutorServiceException 
    {
        facade.sendRepeatedly(delay, interval, times, new MessageFromFacade(message, this));
    }
    
    @Override
    public RavenFuture ask(final Object message, final long timeout, final TimeUnit timeoutTimeUnit) {
        if (message==STOP_MESSAGE)
            return askStop(timeout, timeoutTimeUnit);
        else {
            final RavenFutureImpl future = new RavenFutureImpl(executor);
            final AskDataProcessor ask = new AskDataProcessor(message, timeoutTimeUnit.toMillis(timeout), future);
            new DataProcessorFacadeConfig("ask <"+message+">", owner, ask, executor, new LoggerHelper(logger, "<-> ")).build();        
            return future;
        }
    }

    @Override
    public RavenFuture ask(Object message) {
        return ask(message, defaultAskTimeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public RavenFuture askStop() {
        return askStop(defaultStopTimeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public RavenFuture askStop(long timeout, TimeUnit timeoutTimeUnit) {
        final RavenFutureImpl future = new RavenFutureImpl(executor);
        final StopDataProcessor ask = new StopDataProcessor(timeoutTimeUnit.toMillis(timeout), future);
        new DataProcessorFacadeConfig("ask stop", owner, ask, executor, new LoggerHelper(logger, "<-> ")).build();
        return future;
    }

    @Override
    public String toString() {
        return owner.getPath()+": "+logger.logMess("");
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
    
    private void processStopSequence(final Object message) {
        if (message instanceof ChildTerminated) {
            if (processorContext!=null)
                processorContext.childTerminated((ChildTerminated) message);
        } else if (message instanceof StopFuture) {
            if (processorContext!=null)
                processorContext.stopSelfAndChildren((StopFuture) message);
            else
                ((StopFuture)message).set(true);
        }
    }
    
    private void sendMessageToProcessor(Object message) {
        if (logger.isTraceEnabled())
            logger.trace("Received message: "+message);
        if (message instanceof ChildTerminated || message instanceof StopFuture) {
            processStopSequence(message);
            return;
        }
        final Object origMessage = message;
        DataProcessorFacade sender = null;
        if (message instanceof MessageFromFacade) {
            final MessageFromFacade messageFromFacade = (MessageFromFacade) message;
            message = messageFromFacade.message;
            sender = messageFromFacade.facade;       
        }
        try {
            Object result;
            if (processorContext==null) {
                result = processor.processData(message);
            } else {
                processorContext.sender = sender;
                processorContext.origMessage = origMessage;
                result = processorContext.currentBehaviuor.processData(message);
            }
            if (result!=DataProcessor.UNHANDLED) {
                if (sender!=null && result!=DataProcessor.VOID && result!=DataProcessor.STASHED) 
                    sendTo(sender, result);                
            } else {
                if (unhandledMessagesProcessor!=null)
                    unhandledMessagesProcessor.send(new UnhandledMessageImpl(sender, this, message));
                final LoggerHelper _logger = processorContext!=null? processorContext.getLogger() : logger;
                if (_logger.isWarnEnabled())
                    _logger.warn("Message ({}) was UNHANDLED", message);
            }
        } catch (Throwable e) {
            if (logger.isErrorEnabled())
                logger.error("Error processing message: "+message, e);
            if (unhandledMessagesProcessor!=null)
                unhandledMessagesProcessor.send(new UnhandledMessageImpl(sender, this, message, e));
        }
    }
    
    @Override
    public void setReceiveTimeout(long timeout)  throws ExecutorServiceException {
        setReceiveTimeout(timeout, null);
    }

    @Override
    public void setReceiveTimeout(long timeout, TimeoutMessageSelector selector) throws ExecutorServiceException {
        final CheckTimeoutTask newTask = new CheckTimeoutTask(owner, timeout, selector);
        final CheckTimeoutTask oldTask = checkTimeoutTask.getAndSet(newTask);
        if (oldTask!=null)
            oldTask.cancel();      
        executor.execute(timeout, newTask);
    }

    @Override
    public void resetReceiveTimeout() {
        final CheckTimeoutTask task = checkTimeoutTask.getAndSet(null);
        if (task!=null)
            task.cancel();
    }

    private class CheckTimeoutTask extends AbstractTask {
        private final long timeout;    
        private final TimeoutMessageSelector selector;
        private volatile long lastMessageReceiveTime = System.currentTimeMillis();

        public CheckTimeoutTask(Node taskNode, long timeout, TimeoutMessageSelector selector) {
            super(taskNode, "Checking waiting for message timeout");
            this.timeout = timeout;
            this.selector = selector;
        }
        
        public void messageReceived(Object message) {
            if (selector==null || message==TIMEOUT_MESSAGE || selector.resetTimeout(message)) 
                lastMessageReceiveTime = System.currentTimeMillis();
        }

        @Override
        public void doRun() throws Exception {
            if (lastMessageReceiveTime+timeout<=System.currentTimeMillis()) 
                send(TIMEOUT_MESSAGE);
            if (!isCanceled())
                executeDelayedTask(timeout-(System.currentTimeMillis()-lastMessageReceiveTime), this);
        }
    }
    
    private class WorkerTask extends AbstractTask {

        public WorkerTask(Node taskNode, String status) {
            super(taskNode, status);
        }
        
        private boolean isUnstashing() {
            return processorContext!=null && processorContext.unstashing;
        }
        
        private Object getNextMessage() {
            return processorContext.unstashing? processorContext.nextStashed() : queue.poll();
        }

        @Override
        public void doRun() throws Exception {
            boolean stop = false;
            int processedMessages = 0;
            while (!stop) {
                try {
                    Object message;
                    if (processorContext==null)
                        while ( processedMessages++<maxMessagesPerCycle && (message=queue.poll()) != null && !terminated.get() ) 
                            sendMessageToProcessor(message);
                    else 
                        while ( processedMessages++<maxMessagesPerCycle && (message=getNextMessage()) != null && !terminated.get() ) 
                        sendMessageToProcessor(message);
                } finally {
                    running.set(false);
                    if (terminated.get() || (queue.isEmpty() && !isUnstashing()) || !running.compareAndSet(false, true)) {
                        stop = true;
                    } else if (processedMessages-1==maxMessagesPerCycle) {
                        stop = true;
                        executeTask();
                    }
                }
            }
        }        
    }
    
    private class Context implements DataProcessorContext {
        private DataProcessorFacade sender;
        private LoggerHelper logger;
        private Queue<DataProcessor> behaviourStack;
        private DataProcessor currentBehaviuor = processor;
        private Object origMessage;
        private Map<String, DataProcessorFacade> children;
        private StopFuture stopFuture;
        private Queue stashedMessages;
        private boolean unstashing = false;

        @Override
        public Node getOwner() {
            return owner;
        }

        @Override
        public ExecutorService getExecutor() {
            return executor;
        }

        @Override
        public DataProcessorFacade getSender() {
            return sender;
        }

        @Override
        public DataProcessorFacade getParent() {
            return parent;
        }

        @Override
        public Stashed stash() {
            if (stashedMessages==null)
                stashedMessages = new LinkedList();
            stashedMessages.offer(origMessage);
            return DataProcessor.STASHED;
        }

        @Override
        public void unstashAll() {
            unstashing = stashedMessages!=null;
        }
        
        private Object nextStashed() {
            final Object next = stashedMessages.poll();
            if (stashedMessages.isEmpty()) {
                stashedMessages = null;
                unstashing = false;
            }
            return next;
        }

        @Override
        public DataProcessorFacadeConfig createChild(String name, DataProcessor processor) {
            return new DataProcessorFacadeConfig(name, owner, processor, executor, origLogger);
        }

        @Override
        public DataProcessorFacade addChild(DataProcessorFacadeConfig config) throws NonUniqueNameException {
            if (children==null)
                children = new HashMap<>();
            else if (children.containsKey(config.getFacadeName()))
                throw new NonUniqueNameException(String.format("Child with name %s is already exists", config.getFacadeName()));
            DataProcessorFacade child = config.withParent(DataProcessorFacadeImpl.this).build();
            children.put(child.getName(), child);            
            return child;
        }

        @Override
        public DataProcessorFacade getChild(String name) {
            return children!=null? children.get(name) : null;
        }

        @Override
        public Collection<DataProcessorFacade> getChildren() {
            if (children==null || children.isEmpty())
                return Collections.EMPTY_LIST;
            return Collections.unmodifiableCollection(children.values());
        }
        
        private void childTerminated(ChildTerminated termMessage) {
            if (children!=null) {
                DataProcessorFacade child = children.remove(termMessage.child.getName());
                if (child!=null)
                    ((DataProcessorLogic)processor).childTerminated(child);
            }
            if (children!=null && children.isEmpty())
                children = null;
            if (children==null && stopFuture!=null)
                processStop(stopFuture);
        }
        
        private void stopSelfAndChildren(StopFuture stopFuture) {
            if (children==null || children.isEmpty()) 
                processStop(stopFuture);
            else {
                this.stopFuture = stopFuture;
                for (DataProcessorFacade child: children.values())
                    child.stop();
            }
        }
        
        private void processStop(StopFuture stopFuture) {
            ((DataProcessorLogic)processor).postStop();
            stopFuture.set(true);
        }
        
        public LoggerHelper getLogger() {
            if (logger==null)
                logger = new LoggerHelper(origLogger, String.format("[DP %s: %s] ", path, currentBehaviuor));
            return logger;
        }
        
        public void become(final DataProcessor dataProcessor, final boolean replace) {
            if (getLogger().isDebugEnabled())
                getLogger().debug("--> ["+dataProcessor+"]");
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
            DataProcessor oldBehaviour = currentBehaviuor;
            currentBehaviuor = behaviour;
            logger = null;
            if (getLogger().isDebugEnabled())
                getLogger().debug("<-- ["+oldBehaviour+"]");
            if (behaviourStack!=null && behaviourStack.isEmpty())
                behaviourStack = null;
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

        @Override
        public String toString() {
            return "INTERNAL_STOP_MESSAGE";
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
    
    protected static class MessageFromFacade {
        public final Object message;
        public final DataProcessorFacade facade;

        public MessageFromFacade(Object message, DataProcessorFacade facade) {
            this.message = message;
            this.facade = facade;
        }

        @Override
        public String toString() {
            return "MESSAGE_FROM_FACADE: "+facade+" -> "+message;
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

        @Override
        public Object processData(Object message) throws Exception {
            if (terminated!=null)
                sendReplay(message);
            else if (message instanceof Unwatch)
                removeWatcher((Unwatch) message);
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
        
        private void removeWatcher(Unwatch unwatch) {
            if (watchers!=null)
                watchers.remove(unwatch.watcher);
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
    
    private static class ChildTerminated {
        private final DataProcessorFacade child;

        public ChildTerminated(DataProcessorFacade child) {
            this.child = child;
        }

        @Override
        public String toString() {
            return "CHILD_TERMINATED: "+child.getName();
        }
    }
    
    private static class Unwatch {
        private final DataProcessorFacade watcher;

        public Unwatch(DataProcessorFacade watcher) {
            this.watcher = watcher;
        }

        @Override
        public String toString() {
            return watcher+"UNWATCH";
        }
    }
    
    private class StopDataProcessor extends AbstractDataProcessorLogic {
        private final long timeout;
        private final RavenFutureImpl stopFuture;

        public StopDataProcessor(long timeout, RavenFutureImpl stopFuture) {
            this.timeout = timeout;
            this.stopFuture = stopFuture;
        }

        @Override
        public void postInit() {
            try {
                watch(getFacade());
                stop(timeout);
                getFacade().setReceiveTimeout(2*timeout);
            } catch (ExecutorServiceException ex) {
                stopFuture.setError(ex);
                getFacade().stop();
            }
        }

        @Override
        public void postStop() {
            unwatch(getFacade());
            if (!stopFuture.isDone()) 
                stopFuture.cancel(true);
        }


        @Override
        public Object processData(Object message) throws Exception {
            if (message == TIMEOUT_MESSAGE) {
                stopFuture.setError(new FutureTimeoutException());
                getFacade().stop();
                return VOID;
            } else if (message instanceof Terminated && ((Terminated)message).getFacade()==DataProcessorFacadeImpl.this) {
                stopFuture.set(true);
                getFacade().stop();
                return VOID;
            } else 
                return UNHANDLED;
        }
    }
    
    private class AskDataProcessor extends AbstractDataProcessorLogic {
        private final long askTimeout;
        private final RavenFutureImpl askFuture;
        private final Object message;
        
        public AskDataProcessor(Object message, long askTimeout, RavenFutureImpl askFuture) {
            this.askTimeout = askTimeout;
            this.askFuture = askFuture;
            this.message = message;
        }

        @Override
        public void postInit() {
            super.postInit();
            try {
                getFacade().setReceiveTimeout(askTimeout);
                if (!getFacade().sendTo(DataProcessorFacadeImpl.this, message))
                    throw new DataProcessorFacadeException("Sending message error");
            } catch (Exception ex) {
                askFuture.setError(ex);
                getFacade().stop();
            }
        }

        @Override
        public void postStop() {
            if (!askFuture.isDone())
                askFuture.cancel(true);            
        }

        @Override
        public Object processData(Object message) throws Exception {
            if      (message==TIMEOUT_MESSAGE)          askFuture.setError(new FutureTimeoutException());
            else if (message instanceof RavenFuture)    processFutureMessage((RavenFuture) message);
            else                                        askFuture.set(message);
            getFacade().stop();
            return VOID;
        }
        
        private void processFutureMessage(RavenFuture future) {
            future.onComplete(new FutureCallback<Object, Throwable>() {
                @Override public void onSuccess(Object result) {
                    askFuture.set(result);
                }
                @Override public void onError(Throwable error) {
                    askFuture.setError(error);
                }
                @Override public void onCanceled() {
                    askFuture.cancel(true);
                }
            });
            
        }
    }
}

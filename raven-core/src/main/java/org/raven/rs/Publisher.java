/*
 * Copyright 2016 Mikhail Titov.
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
package org.raven.rs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.RavenFuture;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.raven.dp.impl.Behaviour;
import org.raven.dp.impl.DataProcessorFacadeConfig;
import org.raven.ds.impl.UnsafeRingQueue;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *
 * @author Mikhail Titov
 */
public class Publisher<T> implements org.reactivestreams.Publisher<T> {
    public final static int DEFAULT_QUEUE_SIZE = 32;
    private final static String START_PRODUCER = "START_PRODUCER";
    private final static String PRODUCER_COMPLETED = "PRODUCER_COMPLETED";
    private final static String GET_SUBSCRIBERS = "GET_SUBSCRIBERS";
    private final static String REQUEST_DATA_FROM_PRODUCER = "REQUEST_DATA_FROM_PRODUCER";
    private final static String REQUEST_FROM_SUBSCRIBER = "REQUEST_FROM_SUBSCRIBER";
//    private final String STOP_PRODUCER = "STOP_PRODUCER";
    private final DataProcessorFacade dpf;

    public Publisher(String name, ExecutorService executor, Producer<T> producer, boolean asyncTransitter, Node owner, LoggerHelper logger, int messageQueueSize) {
        final Node _owner = owner==null? executor : owner;
        final String _name = name==null? "->" : name;
        final LoggerHelper _logger = logger==null? new LoggerHelper(_owner, "") : logger;        
        dpf = new DataProcessorFacadeConfig(_name, _owner, new PublisherDP(producer, messageQueueSize, asyncTransitter), executor, _logger).build();
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        dpf.send(new Subscribe(s));
    }
    
    public void start() {
        dpf.send(START_PRODUCER);
    }
    
    public void stop() {
        dpf.stop();
    }
    
    public RavenFuture askStop() {
        return dpf.askStop();
    }
    
    public RavenFuture watch() {
        return dpf.watch();
    }
    
    public RavenFuture<List<Subscriber<T>>, Throwable> getSubscribers() {
        return dpf.ask(GET_SUBSCRIBERS);
    }
    
    public static class PublisherDP<T> extends AbstractDataProcessorLogic {
        private final AtomicBoolean watingForRequestDataFromProducer;
        private final List<SubscriberQueue<T>> subscriberQueues;
        private final int messageQueueSize;
        private final static ErrorSubscription errorSubscription = new ErrorSubscription();
        private final UnsafeRingQueue<T> producerQueue;
        private final int maxProducerQueueSize;
        private final boolean asyncTransmitter;
        private final TransmitterImpl transmitter;
        private final Producer<T> producer;
        private final AtomicBoolean producerCompleted = new AtomicBoolean();
//        private LoggerHelper logger;
        private long waitingPacketsCnt;
        private Throwable error;

        public PublisherDP(Producer<T> producer, int messageQueueSize, boolean asyncTransmitter) {
            this.watingForRequestDataFromProducer = new AtomicBoolean();
            this.subscriberQueues = new ArrayList<>(2);
            this.messageQueueSize = messageQueueSize;
            this.maxProducerQueueSize = messageQueueSize;
            this.producerQueue = new UnsafeRingQueue<>(maxProducerQueueSize);
            this.waitingPacketsCnt = 0;
            this.asyncTransmitter = asyncTransmitter;
            this.transmitter = new TransmitterImpl();
            this.producer = producer;
        }

        @Override
        public void postInit() {
            become(initStage);
        }

        @Override
        public void postStop() {
            for (SubscriberQueue queue: subscriberQueues)
                if (error!=null)
                    queue.sendError(error);
                else {
                    if (getLogger().isDebugEnabled())
                        getLogger().debug("Completeting subscriber: "+queue.subscriber);
                    queue.sendComplete();
                }
        }

        @Override
        public Object processData(Object dataPackage) throws Exception {
            return null;
        }
        
        private final Behaviour subscriberMessageHandler = new Behaviour("SUBSCRIBER MESSAGES HANDLER") {
            @Override public Object processData(Object message) throws Exception {
                if (message instanceof Subscribe) {
                    //подписываем subscriber'а на рассылку
                    subscribeNew(((Subscribe)message).subscriber);
                    if (getLogger().isDebugEnabled())
                        getLogger().debug("Subscribed new subscriber: "+((Subscribe)message).subscriber);
                } else if (message == REQUEST_FROM_SUBSCRIBER) {
                    if (trySendMessagesToSubscribers() || producerQueue.isEmpty())
                        tryRequestDataFromProducer();
//                } else if (message instanceof SubscriberQueue.Request) {
//                    if (getLogger().isDebugEnabled())
//                        getLogger().debug(String.format("Subcriber (%s) requesting data", ((SubscriberQueue.Request)message).getQueue().subscriber));
//                    ((SubscriberQueue.Request)message).getQueue().sendMessages();
//                    tryToMoveMessagesToSubscriberQueues();
                } else if (message instanceof SubscriberQueue.Cancel) {
                    SubscriberQueue queue = ((SubscriberQueue.Cancel)message).getQueue();
                    if (subscriberQueues.remove(queue) && getLogger().isDebugEnabled())
                        getLogger().debug(String.format("Subcriber (%s) canceling subscription", ((SubscriberQueue.Cancel)message).getQueue().subscriber));
                } else if (message==GET_SUBSCRIBERS) {
                    if (subscriberQueues.isEmpty())
                        return Collections.EMPTY_LIST;
                    else {
                        ArrayList<Subscriber> res = new ArrayList<>(subscriberQueues.size());
                        for (SubscriberQueue queue: subscriberQueues)
                            res.add(queue.subscriber);
                        return res;
                    }
                } else return UNHANDLED;
                return VOID;
            }
        };
        
        private final Behaviour initStage = new Behaviour("Initialized") {
            @Override public Object processData(Object message) throws Exception {
                if (message==START_PRODUCER) {
                    if (getLogger().isDebugEnabled())
                        getLogger().debug("Publisher STARTED");
                    become(processingStage);
                    getContext().stash(REQUEST_DATA_FROM_PRODUCER);
                    getContext().unstashAll();
                    return VOID;
                } else
                    return UNHANDLED;
            }
        }.andThen(subscriberMessageHandler);
        
        private final Behaviour processingStage = subscriberMessageHandler.andThen("Processing", new Behaviour(null) {
            @Override public Object processData(Object message) throws Exception {
                if (message == PRODUCER_COMPLETED) {
                    //нужно дождаться когда все сообщения отправятся и затем остановиться
                    getContext().stash();
                    getContext().unstashAll();
                    become(stoppingStage);
                } else if (message instanceof ProducerError) {
                    error = ((ProducerError)message).error;
                    if (getLogger().isErrorEnabled())
                        getLogger().error("Error in producer. Stopping...", error);
                    become(stoppedStage);
                    getFacade().stop();
                } else if (message == REQUEST_DATA_FROM_PRODUCER) {
                    watingForRequestDataFromProducer.set(false);
                    requestDataFromProducer();
                } else
                    handleDataFromProducer((T) message);
                return VOID;
            }
        });
        
        private final Behaviour stoppingStage = new Behaviour("Stopping") {            
            @Override public Object processData(Object message) throws Exception {
                if (message == PRODUCER_COMPLETED) {
                    stopIfPossible();
                    return VOID;
                } else if (message == REQUEST_FROM_SUBSCRIBER) {
                    trySendMessagesToSubscribers();
                    stopIfPossible();
                    return VOID;
                } else
                    return UNHANDLED;
            }
            
            private void stopIfPossible() {
                if (producerQueue.isEmpty()) {
                    getFacade().stop();
                    become(stoppedStage);
                }
            }
        }.andThen(subscriberMessageHandler);

        private final Behaviour stoppedStage = subscriberMessageHandler.andThen("Stopped", new Behaviour("Stopped") {
            @Override public Object processData(Object dataPackage) throws Exception {
                return UNHANDLED;
            }
        });
        
        private void requestDataFromProducer() {            
            long delta = maxProducerQueueSize - waitingPacketsCnt;
            if (getLogger().isDebugEnabled())
                getLogger().debug(String.format("Requesting (%s) data packets from producer", delta));
            waitingPacketsCnt += delta;
            producer.request(transmitter, delta);
        }
        
        private boolean handleDataFromProducer(T data) {
            if (!producerQueue.push(data)) {
                if (getContext().getLogger().isErrorEnabled())
                    getContext().getLogger().error("Error processing data from producer because message PUBLISHER QUEUE IS FULL");
                this.error = new Exception("Error processing data from producer because message PUBLISHER QUEUE IS FULL");                
                getFacade().stop();
                return false;
            } else {
//                waitingPacketsCnt--;
                if (trySendMessagesToSubscribers())
                    tryRequestDataFromProducer();                
                return true;
            }
        }
        
        private boolean trySendMessagesToSubscribers() {
            final long messagesInProducerQueue = producerQueue.size();
            if (messagesInProducerQueue==0)
                return false;
            long maxMessagesToSend = -1;
            long allowedForSend;
            for (SubscriberQueue subs: subscriberQueues) {
                allowedForSend = subs.getAllowedForSend();
                if (maxMessagesToSend==-1)
                    maxMessagesToSend = allowedForSend;
                else if (allowedForSend<maxMessagesToSend)
                    maxMessagesToSend = allowedForSend;
                if (maxMessagesToSend==0)
                    break;
            }
            if (maxMessagesToSend > 0) {
                int i;
                final long messagesToSend = Math.min(messagesInProducerQueue, maxMessagesToSend);
                for (SubscriberQueue queue: subscriberQueues) 
                    for (i=0; i<messagesToSend; i++)
                        queue.send(producerQueue.peek(i));
                for (i=0; i<messagesToSend; i++)
                    producerQueue.pop();    
                waitingPacketsCnt -= messagesToSend;
                return true;                
            } else
                return false;
        }
        
        private void tryRequestDataFromProducer() {
            if (   !producerCompleted.get() 
                && producerQueue.size()<=producerQueue.getFreeSlots() //свободно больше половины слотов
                && watingForRequestDataFromProducer.compareAndSet(false, true)) //уменьшаем кол-во посылки подобных сообщений.
            {
                getFacade().send(REQUEST_DATA_FROM_PRODUCER);
            }            
        }
        
        private void subscribeNew(Subscriber subscriber) {
            for (SubscriberQueue queue: subscriberQueues)
                if (queue.subscriber==subscriber) {
                    subscriber.onSubscribe(errorSubscription);
                    subscriber.onError(new SubscriptionAlreadyExists());
                    return;
                }
            SubscriberQueue queue = new SubscriberQueue(messageQueueSize, subscriber);
            subscriberQueues.add(queue);
            queue.subscribe();
        }
        
        private class TransmitterImpl implements Transmitter<T>{

            @Override public void onNext(T data) {
                if (asyncTransmitter) getFacade().send(data);
                else handleDataFromProducer(data);
            }

            @Override public void onError(Throwable t) {
                getFacade().send(new ProducerError(t));
            }

            @Override public void onComplete() {
                if (producerCompleted.compareAndSet(false, true))
                    getFacade().send(PRODUCER_COMPLETED);
            }
        }
        
        private static class ErrorSubscription implements Subscription {
            @Override public void request(long n) { }
            @Override public void cancel() { }            
        }
        
        public class SubscriberQueue<T> {
            private final AtomicLong allowedForSend;
//            private final UnsafeRingQueue<T> messageQueue;
//            private final DataProcessorFacade publisherFacade;
//            private final Request request;
            private final Cancel cancel;
            private final Subscriber<T> subscriber;
            private final Subscr subcription;

            public SubscriberQueue(int queueSize, Subscriber<T> subscriber) {
//                this.messageQueue = new UnsafeRingQueue(queueSize);
                allowedForSend = new AtomicLong();
//                this.publisherFacade = publisher;
//                this.request = new Request();
                this.cancel = new Cancel();
                this.subscriber =  subscriber;
                this.subcription = new Subscr();
            }

            public void subscribe() {
                subscriber.onSubscribe(subcription);
            }

//            public boolean queueMessage(T message) {
//                return messageQueue.push(message);
//            }
//
//            public boolean isEmpty() {
//                return messageQueue.isEmpty();
//            }

            public void stop() {
                subcription.canceled.set(true);
            }
            
            public long getAllowedForSend() {
                return allowedForSend.get();
            }
            
            public void send(T message) {
                allowedForSend.decrementAndGet();
                subscriber.onNext(message);
            }

//            public void sendMessages() {
//                long readyForSend = Math.min(messageQueue.size(), allowedForSend.get());                    
//                if (readyForSend>0) {
//                    if (getLogger().isDebugEnabled())
//                        getLogger().debug(String.format("Pushing (%d) messages to subscriber (%s)", readyForSend, subscriber));
//                    for (long i=0; i<readyForSend; i++) 
//                        subscriber.onNext(messageQueue.pop());
//                    allowedForSend.addAndGet(-readyForSend);
//                }
//            }
//
            public void sendError(Throwable error) {
                subscriber.onError(error);
                stop();
            }

            public void sendComplete() {
                subscriber.onComplete();
                stop();
            }

//            public long getFreeSlots() {
//                return messageQueue.getFreeSlots();
//            }
//
//            public T getNext() {
//                return messageQueue.pop();
//            }

            private class Subscr implements Subscription {
                private final AtomicBoolean canceled = new AtomicBoolean();

                @Override public void request(long n) {
                    if (!canceled.get()) {
                        allowedForSend.addAndGet(n);
                        getFacade().send(REQUEST_FROM_SUBSCRIBER);
                    }
                }

                @Override public void cancel() {
                    if (canceled.compareAndSet(false, true))
                        getFacade().send(cancel);
                }
            }

//            private class Request {
//                public SubscriberQueue getQueue() {
//                    return SubscriberQueue.this;
//                }
//
//                @Override public String toString() {
//                    return "Request from subscriber: "+subscriber;
//                }
//            }
//            
            private class Cancel {
                public SubscriberQueue getQueue() {
                    return SubscriberQueue.this;
                }

            }
        }        
    };
    
    private static class ProducerError {
        private final Throwable error;

        public ProducerError(Throwable error) {
            this.error = error;
        }        
    }
        
    private static class Subscribe {
        private final Subscriber subscriber;

        public Subscribe(Subscriber subscriber) {
            this.subscriber = subscriber;
        }        

        @Override
        public String toString() {
            return "Subscribe";
        }
    }
}

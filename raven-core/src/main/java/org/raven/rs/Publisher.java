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
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.AbstractDataProcessorLogic;
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
    private final DataProcessorFacade dpf;
    private final Producer<T> producer;

    public Publisher(String name, ExecutorService executor, Producer<T> producer, Node owner, LoggerHelper logger, int messageQueueSize) {
        this.producer = producer;
        final Node _owner = owner==null? executor : owner;
        final String _name = name==null? "->" : name;
        final LoggerHelper _logger = logger==null? new LoggerHelper(_owner, "") : logger;
        dpf = new DataProcessorFacadeConfig(name, owner, new PublisherDP(messageQueueSize), executor, _logger).build();
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        dpf.send(new Subscribe(s));
    }
    
    public static class PublisherDP<T> extends AbstractDataProcessorLogic {
        private final List<SubscriberQueue<T>> subscriberQueues;
        private final int messageQueueSize;
        private final static ErrorSubscription errorSubscription = new ErrorSubscription();                

        public PublisherDP(int messageQueueSize) {
            this.subscriberQueues = new ArrayList<>(2);
            this.messageQueueSize = messageQueueSize;
        }

        @Override public Object processData(Object message) throws Exception {
            if (message instanceof Subscribe) {
                //подписываем subscriber'а на рассылку
                subscribeNew(((Subscribe)message).subscriber);
            } else if (message instanceof SubscriberQueue.Request) {
                ((SubscriberQueue.Request)message).getQueue().sendMessages();
            } else if (message instanceof SubscriberQueue.Cancel) {
                SubscriberQueue queue = ((SubscriberQueue.Cancel)message).getQueue();
                subscriberQueues.remove(queue);
            } else { //значит поступили данные
                
            }
            return null;
        }
        
        private void subscribeNew(Subscriber subscriber) {
            for (SubscriberQueue queue: subscriberQueues)
                if (queue.subscriber==subscriber) {
                    subscriber.onSubscribe(errorSubscription);
                    subscriber.onError(new SubscriptionAlreadyExists());
                    return;
                }
            SubscriberQueue queue = new SubscriberQueue(messageQueueSize, getFacade(), subscriber);
            subscriberQueues.add(queue);
            queue.subscribe();
        }
        
        private static class ErrorSubscription implements Subscription {
            @Override public void request(long n) { }
            @Override public void cancel() { }            
        }
    }
    
    public static class SubscriberQueue<T> {
        private final AtomicLong allowedForSend;
        private final UnsafeRingQueue<T> messageQueue;
        private final DataProcessorFacade publisherFacade;
        private final Request request;
        private final Cancel cancel;
        private final Subscriber<T> subscriber;
        private final Subscr subcription;

        public SubscriberQueue(int queueSize, DataProcessorFacade publisher, Subscriber<T> subscriber) {
            this.messageQueue = new UnsafeRingQueue(queueSize);
            allowedForSend = new AtomicLong();
            this.publisherFacade = publisher;
            this.request = new Request();
            this.cancel = new Cancel();
            this.subscriber =  subscriber;
            this.subcription = new Subscr();
        }
        
        public void subscribe() {
            subscriber.onSubscribe(subcription);
        }
                
        public boolean queueMessage(T message) {
            return messageQueue.push(message);
        }
        
        public void sendMessages() {
            long readyForSend = Math.min(messageQueue.size(), allowedForSend.get());
            if (readyForSend>0) {
                for (long i=0; i<readyForSend; i++) 
                    subscriber.onNext(messageQueue.pop());
                allowedForSend.addAndGet(-readyForSend);
            }
        }
        
        public long getFreeSlots() {
            return messageQueue.getFreeSlots();
        }
        
        public T getNext() {
            return messageQueue.pop();
        }
        
        private class Subscr implements Subscription {
            @Override
            public void request(long n) {
                allowedForSend.addAndGet(n);
                publisherFacade.send(request);
            }

            @Override
            public void cancel() {
                publisherFacade.send(cancel);
            }
        }
        
        private class Request {
            public SubscriberQueue getQueue() {
                return SubscriberQueue.this;
            }
        }
        private class Cancel {
            public SubscriberQueue getQueue() {
                return SubscriberQueue.this;
            }
            
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

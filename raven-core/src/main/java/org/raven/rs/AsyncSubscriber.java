/*
 * Copyright 2016 tim1.
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

import org.raven.dp.DataProcessorFacade;
import org.raven.dp.RavenFuture;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.raven.dp.impl.Behaviour;
import org.raven.dp.impl.DataProcessorFacadeConfig;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *
 * @author Mikhail Titov
 */
public class AsyncSubscriber<T> implements Subscriber<T> {
    private final static String SUBSCRIPTION_COMPLETED = "SUBSCRIPTION_COMPLETED";
    private final DataProcessorFacade dpf;
    
    public AsyncSubscriber(Consumer<T> consumer, ExecutorService executor, String name, Node owner, LoggerHelper logger, int messageQueueSize) {
        final Node _owner = owner==null? executor : owner;
        final String _name = name==null? "->" : name;
        final LoggerHelper _logger = logger==null? new LoggerHelper(_owner, "") : logger;        
        dpf = new DataProcessorFacadeConfig<>(_name, _owner, new SubscriberDP<>(consumer, messageQueueSize), executor, _logger).build();
    }

    @Override
    public void onSubscribe(Subscription s) {
        dpf.send(s);
    }

    @Override
    public void onNext(T t) {
        dpf.send(t);
    }

    @Override
    public void onError(Throwable t) {
        dpf.send(t);
    }

    @Override
    public void onComplete() {
        dpf.send(SUBSCRIPTION_COMPLETED);
    }
    
    public RavenFuture watch() {
        return dpf.watch();
    }
    
    private static class SubscriberDP<T> extends AbstractDataProcessorLogic {
        private final Consumer<T> consumer;
        private final long messageQueueSize;
        private final long requestTrigger; 
        private Subscription subscription;
        private long requestedMessages;

        public SubscriberDP(Consumer<T> consumer, int messageQueueSize) {
            this.consumer = consumer;
            this.messageQueueSize = messageQueueSize;
            this.requestedMessages = 0;
            this.requestTrigger = messageQueueSize / 2;
        }

        @Override
        public void postInit() {
            super.postInit();
            become(subscribing);
        }

        @Override
        public Object processData(Object dataPackage) throws Exception {
            return null;
        }
        
        public final Behaviour subscribing = new Behaviour("Subscribing") {            
            @Override public Object processData(Object message) throws Exception {
                if (message instanceof Subscription) {
                    subscription = (Subscription) message;
                    consumer.onSubscribe(new Subscr());
                    subscription.request(messageQueueSize);
                    requestedMessages = messageQueueSize;
                    become(processing);
                    return VOID;
                } else
                    return UNHANDLED;
            }
        };
        
        public final Behaviour processing = new Behaviour("Processing") {
            @Override public Object processData(Object message) throws Exception {
                if (message==SUBSCRIPTION_COMPLETED) {
                    consumer.onComplete();
                    become(stopped);
                    getFacade().stop();
                } else if (message instanceof Throwable) {
                    consumer.onError((Throwable)message);
                    getFacade().stop();
                } else {
                    if (--requestedMessages <= requestTrigger) {
                        long delta = messageQueueSize - requestedMessages;
                        if (getLogger().isDebugEnabled())
                            getLogger().debug(String.format("Requesting (%d) data packets from publisher", delta));
                        requestedMessages = messageQueueSize;
                        subscription.request(delta);
                    }
                    consumer.onNext((T)message);
                }
                return VOID;
            }
        };
        
        public final Behaviour stopped = new Behaviour("Stopped") {
            @Override public Object processData(Object dataPackage) throws Exception {
                return UNHANDLED;
            }
        };
        
        private class Subscr implements StreamSubscription {
            @Override public void cancel() {
                subscription.cancel();
                become(stopped);
                getFacade().stop();
            }
        }
    }
}

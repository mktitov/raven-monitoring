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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class PublisherTest extends RavenCoreTestCase {    

    @Before
    public void prepare() {
        testsNode.setLogLevel(LogLevel.TRACE);
    }
    
    @Test public void subscriptionTest(
            @Mocked final Producer<String> producer,
            @Mocked final Subscriber<String> subscriber
    ) throws Exception {
        testsNode.setLogLevel(LogLevel.TRACE);
        Publisher<String> publisher = new Publisher<>("Pub1", createExecutor(), producer, true, testsNode, null, 32);
        publisher.subscribe(subscriber);
        List<Subscriber<String>> subscribers = publisher.getSubscribers().get();
        assertNotNull(subscribers);
        assertEquals(1, subscribers.size());
        publisher.askStop().get();
        new VerificationsInOrder() {{
            subscriber.onSubscribe(withInstanceOf(Subscription.class));
            subscriber.onComplete();
        }};
    }
    
    @Test public void cancelSubscriptionInInitStageTest(
            @Mocked final Producer<String> producer,
            @Mocked final Subscriber<String> subscriber
    ) throws Exception {
        final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
        new Expectations() {{
            subscriber.onSubscribe(withInstanceOf(Subscription.class)); result = new Delegate() {
                public void onSubscribe(Subscription subcription) {
                    subscriptionRef.set(subcription);
                }
            };
        }};
        Publisher<String> publisher = new Publisher<>("Pub1", createExecutor(), producer, true, testsNode, null, 32);
        publisher.subscribe(subscriber);
        assertTrue(hasSubscriber(subscriber, publisher));
        subscriptionRef.get().cancel();
        assertFalse(hasSubscriber(subscriber, publisher));
        publisher.askStop().get();
    }
    
    @Test()
    public void completeChainTest(
            @Mocked final Producer<Integer> producer,
            @Mocked final Subscriber<Integer> subscriber,            
            @Mocked final Subscriber<Integer> subscriber2            
    ) throws Exception {
        final AtomicReference<Publisher<Integer>> publisherRef = new AtomicReference<>();
        new StrictExpectations() {{
            final AtomicInteger counter = new AtomicInteger();
            final AtomicInteger subscriber1Counter = new AtomicInteger();
            final AtomicInteger subscriber2Counter = new AtomicInteger(2);
            final AtomicReference<Subscription> subscription1 = new AtomicReference<>();
            subscriber.onSubscribe((Subscription) any); result = new Delegate() {
                public void onSubscribe(Subscription subscription) {
                    subscription1.set(subscription);
                    subscription.request(10);
                }
            };
            producer.request((Transmitter) any, 2); result = new Delegate<Integer>() {
                public void request(Transmitter<Integer> transmitter, long count) {
                    while (count-- > 0)
                        transmitter.onNext(counter.incrementAndGet());
                    publisherRef.get().subscribe(subscriber2);              
                    subscription1.get().cancel();
                }
            };
            subscriber.onNext(anyInt); result = new Delegate() {
                public void onNext(Integer num) {
                    assertEquals(subscriber1Counter.incrementAndGet(), num.intValue());
                }
            }; times = 2;
            subscriber2.onSubscribe((Subscription) any); result = new Delegate() {
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }
            };
            producer.request((Transmitter) any, 2); result = new Delegate<Integer>() {
                public void request(Transmitter<Integer> transmitter, long count) {
                    while (count-- > 0)
                        transmitter.onNext(counter.incrementAndGet());
                    transmitter.onComplete();
                }
            }; 
            subscriber2.onNext(anyInt); result = new Delegate() {
                public void onNext(Integer num) {
                    assertEquals(subscriber2Counter.incrementAndGet(), num.intValue());
                }
            }; times = 1;
            subscriber2.onNext(anyInt); result = new Delegate() {
                public void onNext(Integer num) {
                    assertEquals(subscriber2Counter.incrementAndGet(), num.intValue());
                }
            }; times = 1;
            subscriber2.onComplete();
        }};
        Publisher<Integer> publisher = new Publisher<>("Pub1", createExecutor(), producer, true, testsNode, null, 2);
        publisherRef.set(publisher);
        publisher.subscribe(subscriber);
        publisher.start();
        publisher.watch().getOrElse(false, 1, TimeUnit.SECONDS);
    }
    
    @Test
    public void asyncSubscriberTest(
            @Mocked final Producer<Integer> producer,
            @Mocked final Consumer<Integer> consumer1,
            @Mocked final Consumer<Integer> consumer2
    ) throws Exception {
        final AtomicInteger consumer1Counter = new AtomicInteger();
        final AtomicInteger consumer2Counter = new AtomicInteger();
        new Expectations() {{
            final AtomicInteger counter = new AtomicInteger();
            producer.request((Transmitter<Integer>)any, anyLong); result = new Delegate() {
                public void request(Transmitter<Integer> transmitter, long count) {
                    while (count-- > 0) {
                        if (counter.get() < 100) {
                            transmitter.onNext(counter.incrementAndGet());
                            if (counter.get()==100)
                                transmitter.onComplete();
                        }
                    }
                }
            }; minTimes = 1; maxTimes = 1000;
            consumer1.onSubscribe((StreamSubscription) any);
            consumer2.onSubscribe((StreamSubscription) any);
            consumer1.onNext(anyInt); result = new Delegate() {
                public void onNext(Integer val) {
                    assertEquals(consumer1Counter.incrementAndGet(), val.intValue());
                }
            }; maxTimes = 1000;
            consumer2.onNext(anyInt); result = new Delegate() {
                public void onNext(Integer val) {
                    assertEquals(consumer2Counter.incrementAndGet(), val.intValue());
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException ex) {
                    }
                }
            }; maxTimes = 1000;
            consumer1.onComplete();
            consumer2.onComplete();
        }};
        final ExecutorService executor = createExecutor();
        Publisher<Integer> publisher = new Publisher<>("Pub1", executor, producer, true, testsNode, null, 32);
        AsyncSubscriber<Integer> subscriber1 = new AsyncSubscriber<>(consumer1, executor, "Subs_fast", testsNode, null, 16);
        AsyncSubscriber<Integer> subscriber2 = new AsyncSubscriber<>(consumer2, executor, "Subs_slow", testsNode, null, 32);
        publisher.subscribe(subscriber1);
        publisher.subscribe(subscriber2);
        publisher.start();
        publisher.watch().getOrElse(false, 10000);
        subscriber1.watch().getOrElse(false, 1000);
        subscriber2.watch().getOrElse(false, 1000);
    }
    
    private <T> boolean hasSubscriber(Subscriber<T> subscriber, Publisher<T> publisher) throws Exception {
        List<Subscriber<T>> subscribers = publisher.getSubscribers().get();
        assertNotNull(subscribers);
        return subscribers.contains(subscriber);
    }
    
    private ExecutorService createExecutor() throws Exception {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
//        executor.setMaximumQueueSize(10);
        executor.setCorePoolSize(8);
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
//        executor.setMaximumPoolSize(50);
        assertTrue(executor.start());
        //warmup executor
        return executor;
    }
    
//    private <T> Subscriber createAsyncSubscriber (Subscriber<T> subscriber, ExecutorService executor) {
//        
//    } 
//    
//    private class AsyncSubscriber
    
}

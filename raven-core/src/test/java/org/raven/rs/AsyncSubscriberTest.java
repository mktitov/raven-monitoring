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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
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
public class AsyncSubscriberTest extends RavenCoreTestCase {
    
    @Before
    public void prepare() {
        testsNode.setLogLevel(LogLevel.TRACE);
    }
    
    @Test
    public void test(
            @Mocked final Subscription subscription,
            @Mocked final Consumer<Integer> consumer
    ) throws Exception {
        final AtomicReference<Subscriber<Integer>> subscriberRef = new AtomicReference<>();
        final AtomicInteger consumerCounter = new AtomicInteger();
        new Expectations() {{
            final AtomicInteger counter = new AtomicInteger();
            subscription.request(4l); result = new Delegate() {
                public void request(long count) {
                    while (count-- > 0) {
                        subscriberRef.get().onNext(counter.incrementAndGet());
                    }
                }
            };
            subscription.request(2l); result = new Delegate() {
                public void request(long count) {
                    while (count-- > 0) {
                        if (counter.get()<100) {
                            subscriberRef.get().onNext(counter.incrementAndGet());
                            if (counter.get()==100)
                                subscriberRef.get().onComplete();
                        }
                    }
                }
            }; times=50;
            consumer.onSubscribe((StreamSubscription) any);
            consumer.onNext(anyInt); result = new Delegate() {
                public void onNext(Integer val) {
                    assertEquals(consumerCounter.incrementAndGet(), val.intValue());
                }
            };
        }};
        AsyncSubscriber<Integer> subscriber = new AsyncSubscriber<>(consumer,  createExecutor(), "Cons", testsNode, null, 4);
        subscriberRef.set(subscriber);
        subscriber.onSubscribe(subscription);
        subscriber.watch().getOrElse(false, 1000);
        assertEquals(100, consumerCounter.get());
    }
    
    public void cancelTest(
            @Mocked final Subscription subscription,
            @Mocked final Consumer<Integer> consumer
    ) throws Exception {
        final AtomicReference<Subscriber<Integer>> subscriberRef = new AtomicReference<>();
        new Expectations() {{
            final AtomicReference<StreamSubscription> streamSubscription = new AtomicReference<>();
            final AtomicInteger counter = new AtomicInteger();
            subscription.request(4l); result = new Delegate() {
                public void request(long count) {
                    while (count-- > 0) {
                        subscriberRef.get().onNext(counter.incrementAndGet());
                    }
                }
            };
            consumer.onSubscribe((StreamSubscription) any); result = new Delegate() {
                public void onSubscribe(StreamSubscription subs) {
                    streamSubscription.set(subs);
                }
            };
            consumer.onNext(1); result = new Delegate() {
                public void onNext(Integer val) {
                    streamSubscription.get().cancel();
                }
            };
        }};
        AsyncSubscriber<Integer> subscriber = new AsyncSubscriber<>(consumer,  createExecutor(), "Cons", testsNode, null, 4);
        subscriberRef.set(subscriber);
        subscriber.onSubscribe(subscription);
        subscriber.watch().getOrElse(false, 1000);
    } 
    
    
    private ExecutorService createExecutor() throws Exception {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setCorePoolSize(8);
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        assertTrue(executor.start());
        return executor;
    }
    
}

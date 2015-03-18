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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.raven.ds.AskCallback;
import org.raven.ds.RavenFuture;

/**
 *
 * @author Mikhail Titov
 */
public class RavenFutureImpl<V> implements RavenFuture<V> {
    private final static int COMPUTING = 0;
    private final static int CANCELED = 1;
    private final static int DONE = 2;
    private final static int ERROR = 3;
            
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicInteger state = new AtomicInteger(COMPUTING);
    private volatile V result;
    private volatile Throwable error;
    
    private final AskCallback callback;

    public RavenFutureImpl() {
        this(null);
    }

    public RavenFutureImpl(AskCallback callback) {
        this.callback = callback;
    }

    protected void done() {
        if (callback!=null) {
            switch (state.get()) {
                case DONE : callback.onSuccess(result); break;
                case ERROR: callback.onError(error); break;
                case CANCELED: callback.onError(new FutureCanceledException());
            }
        }
    }

    public V getOrElse(V v) {
        try {
            return get();
        } catch (Exception ex) {
           return v;
        }
    }

    public V getOrElse(V v, long timeout, TimeUnit timeUnit) {
        try {
            return get(timeout, timeUnit);
        } catch (Exception ex) {
           return v;
        }
    }

    public V getOrElse(V v, long timeoutMs) {
        try {
            return get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
           return v;
        }
    }

    public boolean cancel(boolean bln) {
        if (state.compareAndSet(COMPUTING, CANCELED)) {
            latch.countDown();
            done();
            return true;
        } 
        return false;
    }

    public boolean isCancelled() {
        return state.get()==CANCELED;
    }

    public boolean isDone() {
        return state.get() > COMPUTING;
    }
    
    public void setError(Throwable error) {
        if (state.compareAndSet(COMPUTING, ERROR)) {
            this.error = error;
            latch.countDown();
            done();
        }
    }
    
    public void set(V value) {
        if (state.compareAndSet(COMPUTING, DONE)) {
            result = value;
            latch.countDown();
            done();
        }
    }

    public V get() throws InterruptedException, ExecutionException {
        latch.await();
        return getResult();
    }
    
    public V get(long l, TimeUnit tu) throws InterruptedException, ExecutionException, TimeoutException {
        if (latch.await(l, tu))
            return getResult();
        throw new TimeoutException();
    }

    private V getResult() throws InterruptedException, ExecutionException {
        switch (state.get()) {
            case DONE: return result;
            case CANCELED: throw new FutureCanceledException();
            case ERROR: throw new ExecutionException("Future computation error", error);
        }
        throw new InterruptedException("Unknown future state");
    }
}

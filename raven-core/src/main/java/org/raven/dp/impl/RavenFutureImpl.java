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
import org.raven.dp.FutureCanceledException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.raven.dp.FutureCallback;
import org.raven.dp.FutureCallbackWithTimeout;
import org.raven.dp.RavenFuture;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;

/**
 *
 * @author Mikhail Titov
 */
public class RavenFutureImpl<V> implements RavenFuture<V> {
    private final static int COMPUTING = 0;
    private final static int CANCELED = 1;
    private final static int DONE = 2;
    private final static int ERROR = 3;
//    private final static int TIMEOUT = 4;
            
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicInteger state = new AtomicInteger(COMPUTING);
    private volatile V result;
    private volatile Throwable error;
    
//    private final FutureCallback callback;
    private final ExecutorService executor;
    private List<FutureCallback> onCompleteCallbacks;

//    public RavenFutureImpl() {
//        this(null);
//    }
//
    public RavenFutureImpl(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public RavenFuture<V> onComplete(FutureCallback<V> callback) {
        if (isDone()) executeOnCompleteCallback(callback);
        else {
            addCallback(callback);
            if (isDone())
                executeCallbacks();                
        }
        return this;
    }

    @Override
    public RavenFuture<V> onComplete(long timeout, FutureCallbackWithTimeout<V> callback) {
        if (isDone()) executeOnCompleteCallback(callback);
        else onComplete(new CallbackWrapper(timeout, callback));
        return this;
    }

    private synchronized void addCallback(FutureCallback<V> callback) {
        if (onCompleteCallbacks==null)
            onCompleteCallbacks = new LinkedList();
        onCompleteCallbacks.add(callback);
    }
    
    private synchronized void executeCallbacks() {
        if (onCompleteCallbacks!=null) {
            for (FutureCallback callback: onCompleteCallbacks)
                executeOnCompleteCallback(callback);
            onCompleteCallbacks = null;
        }
    }
    
    private void executeOnCompleteCallback(final FutureCallback<V> callback) {
        executor.executeQuietly(new AbstractTask(executor, "Processing future onComplete") {
            @Override
            public void doRun() throws Exception {
                switch (state.get()) {
                    case DONE : callback.onSuccess(result); break;
                    case ERROR: callback.onError(error); break;
                    case CANCELED: callback.onCanceled(); break;
                }
            }
        });
    }
    
    protected void done() {
        executeCallbacks();
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
    
    private class CallbackWrapper<V> implements FutureCallbackWithTimeout<V> {
        private final FutureCallbackWithTimeout<V> callback;
        private final AtomicBoolean executed = new AtomicBoolean();
        private final TimeoutTask timeoutTask;

        public CallbackWrapper(long timeout, FutureCallbackWithTimeout<V> callback) {
            this.callback = callback;
            this.timeoutTask = new TimeoutTask(this);
            executor.executeQuietly(timeout, timeoutTask);
        }
                
        @Override
        public void onSuccess(V result) {
            if (executed.compareAndSet(false, true)) {
                timeoutTask.cancel();
                callback.onSuccess(result);
            }
        }

        @Override
        public void onError(Throwable error) {
            if (executed.compareAndSet(false, true)) {
                timeoutTask.cancel();
                callback.onError(error);
            }
        }

        @Override
        public void onCanceled() {
            if (executed.compareAndSet(false, true)) {
                timeoutTask.cancel();
                callback.onCanceled();
            }
        }

        @Override
        public void onTimeout() {
            if (executed.compareAndSet(false, true))
                callback.onTimeout();
        }
        
    }
    
    private class TimeoutTask extends AbstractTask {
        private final CallbackWrapper callback;

        public TimeoutTask(CallbackWrapper callback) {
            super(executor, "Wating for future onComplete timeout");
            this.callback = callback;
        }

        @Override
        public void doRun() throws Exception {
            callback.onTimeout();
        }
    }
}

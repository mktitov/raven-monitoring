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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.raven.dp.FutureCallback;
import org.raven.dp.FutureCallbackWithTimeout;
import org.raven.dp.RavenFuture;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;

/**
 *
 * @author Mikhail Titov
 */
public class CompletedFuture<V> implements RavenFuture<V, Throwable> {
    private final V value;
    private final ExecutorService executor;

    public CompletedFuture(V value, ExecutorService executor) {
        this.value = value;
        this.executor = executor;
    }

    @Override
    public V getOrElse(V v) {
        return value;
    }

    @Override
    public V getOrElse(V v, long timeout, TimeUnit timeUnit) {
        return value;
    }

    @Override
    public V getOrElse(V v, long timeoutMs) {
        return value;
    }

    @Override
    public RavenFuture<V, Throwable> onComplete(final FutureCallback<V, Throwable> callback) {
        executor.executeQuietly(new AbstractTask(executor, "Executing complete callback") {
            @Override public void doRun() throws Exception {
                callback.onSuccess(value);
            }
        });
        return this;
    }

    @Override
    public RavenFuture<V, Throwable> onComplete(long timeout, FutureCallbackWithTimeout<V, Throwable> callback) {
        onComplete(callback);
        return this;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return value;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return value;
    }
    
}

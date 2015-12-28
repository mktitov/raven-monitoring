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

import org.raven.dp.RavenFuture;
import org.raven.sched.ExecutorService;

/**
 *
 * @author Mikhail Titov
 */
public class RavenPromise<V, E extends Throwable> {
    private final Future future;

    public RavenPromise(ExecutorService executor) {
        this.future = new Future(executor);                
    }

    public RavenFuture<V, E> getFuture() {
        return future;
    }

    public void completeWithValue(V value) {
        future.set(value);
    }

    public void completeWithError(E error) {
        future.setError(error);
    }
    
    public boolean isCanceled() {
        return future.isCancelled();
    }
    
    private class Future extends RavenFutureImpl<V, E> {

        public Future(ExecutorService executor) {
            super(executor);
        }
    }
}

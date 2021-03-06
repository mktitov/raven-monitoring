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
package org.raven.dp;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Mikhail Titov
 */
public interface RavenFuture<V, E extends Throwable> extends Future<V> {
    public V getOrElse(V v);
    public V getOrElse(V v, long timeout, TimeUnit timeUnit);
    public V getOrElse(V v, long timeoutMs);
    public RavenFuture<V, E> onComplete(FutureCallback<V, E> callback);
    public RavenFuture<V, E> onComplete(long timeout, FutureCallbackWithTimeout<V, E> callback);
}

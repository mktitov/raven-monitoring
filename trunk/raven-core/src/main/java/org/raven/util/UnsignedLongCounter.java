/*
 * Copyright 2013 Mikhail Titov.
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

package org.raven.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Mikhail Titov
 */
public class UnsignedLongCounter {
    private final AtomicLong counter;
    private final long initialValue;

    public UnsignedLongCounter() {
        this(0);
    }

    public UnsignedLongCounter(long initialValue) {
        this.initialValue = initialValue;
        counter = new AtomicLong(initialValue);
    }
    
    public long getNext() {
        final long nextVal = counter.incrementAndGet();
        if (nextVal>=initialValue) return nextVal;
        else {
            synchronized(counter) {
                if (counter.get() >= initialValue) return counter.incrementAndGet();
                else {
                    counter.set(initialValue);
                    return initialValue;
                }
            }
        }
    }
}

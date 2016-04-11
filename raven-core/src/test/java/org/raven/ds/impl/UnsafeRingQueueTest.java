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
package org.raven.ds.impl;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class UnsafeRingQueueTest extends Assert {
    @Test
    public void test() {
        System.out.println("\n\n-----------------\nTESTING: ");
        UnsafeRingQueue<Long> queue = new UnsafeRingQueue<>(1);
        for (long i=0; i<(long)Integer.MAX_VALUE*2; i++) {
            queue.push(i);
            assertEquals(new Long(i), queue.pop());
            if (i==Integer.MAX_VALUE)
                System.out.println("i="+i);
        }
    }
}

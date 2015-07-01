/*
 * Copyright 2012 Mikhail Titov.
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
package org.raven.sched.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Mikhail Titov
 */
public class ZeroBlockingQueue<T> implements BlockingQueue<T> {

    public boolean add(T e) {
        return false;
    }

    public boolean offer(T e) {
        return false;
    }

    public void put(T e) throws InterruptedException {
    }

    public boolean offer(T e, long l, TimeUnit tu) throws InterruptedException {
        System.out.println(String.format("!!! timeout: %d, timeunit: %s", l, tu));
        return false;
    }

    public T take() throws InterruptedException {
        for (;;)
            Thread.sleep(1000);
    }

    public T poll(long l, TimeUnit tu) throws InterruptedException {
        tu.sleep(l);
        return null;
    }

    public int remainingCapacity() {
        return 0;
    }

    public boolean remove(Object o) {
        return false;
    }

    public boolean contains(Object o) {
        return false;
    }

    public int drainTo(Collection<? super T> clctn) {
        return 0;
    }

    public int drainTo(Collection<? super T> clctn, int i) {
        return 0;
    }

    public T remove() {
        return null;
    }

    public T poll() {
        return null;
    }

    public T element() {
        return null;
    }

    public T peek() {
        return null;
    }

    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return false;
    }

    public Iterator<T> iterator() {
        return null;
    }

    public Object[] toArray() {
        return null;
    }

    public <T> T[] toArray(T[] ts) {
        return null;
    }

    public boolean containsAll(Collection<?> clctn) {
        return false;
    }

    public boolean addAll(Collection<? extends T> clctn) {
        return false;
    }

    public boolean removeAll(Collection<?> clctn) {
        return false;
    }

    public boolean retainAll(Collection<?> clctn) {
        return false;
    }

    public void clear() {
    }
}

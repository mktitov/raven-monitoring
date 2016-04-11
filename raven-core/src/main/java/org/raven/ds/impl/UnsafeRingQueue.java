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

/**
 *
 * @author Mikhail Titov
 */
public class UnsafeRingQueue<E> {
    private final E[] data;
    private final long maxSize;
    private long readPos = 0;
    private long writePos = 0;

    public UnsafeRingQueue(int maxSize) {
        this.maxSize = maxSize;
        this.data = (E[])new Object[maxSize];
    }

    public boolean canPush() {
        return writePos-readPos < maxSize;
    }
    
    public boolean push(E element) {
        if (writePos-readPos >= maxSize)
            return false;
//        if (writePos+1<0) {
//            writePos = writePos%maxSize;
//            readPos =  readPos%maxSize;
//        }
        data[(int)((writePos++)%maxSize)] = element;
        return true; 
   }
    
    public boolean hasElement() {
        return readPos<writePos;
    }
    
    public E peek() {
        return hasElement()? data[(int)(readPos % maxSize)] : null;
    }
    
    public E pop() {
        return hasElement()? data[(int)(readPos++ % maxSize)] : null;
    }    
}

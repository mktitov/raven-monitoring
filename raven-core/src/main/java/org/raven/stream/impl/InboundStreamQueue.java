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
package org.raven.stream.impl;

import org.raven.ds.impl.UnsafeRingQueue;

/**
 *
 * @author Mikhail Titov
 */
public class InboundStreamQueue<T> {
    private final UnsafeRingQueue<ElementImpl> elemQueue;
    private final UnsafeRingQueue<ElementImpl> elemCache;
    
    public InboundStreamQueue(final int queueSize) {
        elemQueue = new UnsafeRingQueue<>(queueSize);
        elemCache = new UnsafeRingQueue<>(queueSize);
    }
    
    public boolean push(T data, long seqnum) {
        if (elemQueue.canPush()) {
            ElementImpl elem = elemCache.pop();
            if (elem==null) 
                elem = new ElementImpl();
            elem.init(data, seqnum);
            elemQueue.push(elem);
            return true;
        } else 
            return false;
    }
    
    public Element<T> peek() {
        return elemQueue.peek();
    }
    
    public Element<T> pop() {
        final ElementImpl elem =  elemQueue.pop();
        if (elem!=null)
            elemCache.push(elem);
        return elem;
    }
    
    public interface Element<D> {
        public D getData();
        public long getSeqnum();
        public InboundStreamQueue<D> getQueue();
    }    

    private class ElementImpl implements Element<T> {
        private T data;
        private long seqnum;
        
        public void init(T data, long seqnum) {
            this.data = data;
            this.seqnum = seqnum;
        }

        @Override
        public T getData() {
            return data;
        }

        @Override
        public long getSeqnum() {
            return seqnum;
        }

        @Override
        public InboundStreamQueue<T> getQueue() {
            return InboundStreamQueue.this;
        }
    }
}

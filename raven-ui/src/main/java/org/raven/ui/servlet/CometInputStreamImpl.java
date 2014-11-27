/*
 * Copyright 2014 tim.
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

package org.raven.ui.servlet;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.raven.ds.RingQueue;
import org.raven.ds.impl.RingQueueImpl;

/**
 *
 * @author Mikhail Titov
 */
public class CometInputStreamImpl extends CometInputStream {
    private final AtomicBoolean sourceClosed = new AtomicBoolean();
    private final RingQueue<ByteBuf> buffers;
    private volatile int readBytes = 0;

    public CometInputStreamImpl() {
        this(100);
    }
    
    public CometInputStreamImpl(int queueSize) {
        buffers = new RingQueueImpl<ByteBuf>(queueSize);
    }
    
    @Override
    public int read() throws IOException {
        if (sourceClosed.get() && !buffers.hasElement()) {
            return -1;
        }
        ByteBuf buf;
        try {
            while (true) {
                while ((buf=buffers.peek())==null && !sourceClosed.get()) {
                    synchronized(this) {
                        wait(100l);
                    }
                }
                if (buf!=null) {
                    if (buf.isReadable()) {
                        ++readBytes;
                        return buf.readUnsignedByte();
                    } else 
                        buffers.pop().release();
                } else if (sourceClosed.get()) {
                    return -1;
                }
            }
        } catch (InterruptedException e) {
            return -1;
        }
    }

    public boolean canPushBuffer() {
        return buffers.canPush();
    }

    public void pushBuffer(ByteBuf buf) {
        buffers.push(buf);
        synchronized(this) {
            notify();
        }
    }

    public void dataStreamClosed() {
        if (sourceClosed.compareAndSet(false, true))        
            synchronized(this) {
                notify();
            }
    }
    
}

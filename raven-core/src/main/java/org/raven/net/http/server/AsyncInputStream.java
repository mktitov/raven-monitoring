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
package org.raven.net.http.server;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.raven.rs.Transmitter;

/**
 *
 * @author Mikhail Titov 
 */
public class AsyncInputStream extends InputStream implements Transmitter<ByteBuf> {
    private final AtomicBoolean sourceClosed = new AtomicBoolean();
    private final BlockingQueue<ByteBuf> buffers;
    private final AtomicReference<IOException> errorRef = new AtomicReference<>();
    
    public AsyncInputStream() {
        this(100);
    }
    
    public AsyncInputStream(int queueSize) {
        buffers = new ArrayBlockingQueue<>(queueSize);
    }

    @Override
    public void onNext(ByteBuf data) {
        if (!sourceClosed.get() && !buffers.offer(data)) {
            if (errorRef.compareAndSet(null, new OverflowException()))
                sourceClosed.set(true);
        }
    }

    @Override
    public void onError(Throwable error) {
        if (errorRef.compareAndSet(null, new IOException(error)))
            sourceClosed.set(true);        
    }

    @Override
    public void onComplete() {
        sourceClosed.set(true);
    }

    @Override
    public int read() throws IOException {
        if (sourceClosed.get() && buffers.isEmpty()) {
            if (errorRef.get()!=null)
                throw errorRef.get();
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
                    if (buf.isReadable()) 
                        return buf.readUnsignedByte();
                    else 
                        buffers.poll().release();
                } else if (sourceClosed.get()) {
                    if (errorRef.get()!=null)
                        throw errorRef.get();
                    return -1;
                }
            }
        } catch (InterruptedException e) {
            return -1;
        }
    }
    
    public class OverflowException extends IOException {
    }
}

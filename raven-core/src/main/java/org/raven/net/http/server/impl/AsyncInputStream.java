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
package org.raven.net.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov 
 */
//TODO надо сделать поддержку back presure
public class AsyncInputStream extends InputStream {
    private final AtomicBoolean sourceClosed = new AtomicBoolean();
    private final BlockingQueue<ByteBuf> buffers;
    private final AtomicReference<IOException> errorRef = new AtomicReference<>();
    private final Channel channel;    
    private final int stopReadThreshold;
    private final LoggerHelper logger;
    
    public AsyncInputStream(Channel channel, LoggerHelper parentLogger) {
        this(channel, parentLogger, 100);
    }
    
    public AsyncInputStream(Channel channel, LoggerHelper parentLogger, int queueSize) {
        buffers = new ArrayBlockingQueue<>(queueSize);
        this.channel = channel;
        this.stopReadThreshold = queueSize / 2;
        this.logger = new LoggerHelper(parentLogger, "Request stream. ");
    }

    public void onNext(ByteBuf data) throws IOException {
        if (!sourceClosed.get() && !buffers.offer(data.retain())) {
            data.release();
            OverflowException ex = new OverflowException();
            if (errorRef.compareAndSet(null, ex)) 
                sourceClosed.set(true);
            if (logger.isErrorEnabled())
                logger.error("OVERFLOWED");
            throw ex;
        } else {
            if (buffers.remainingCapacity() <= stopReadThreshold && channel.isActive() && channel.config().isAutoRead()) {
                if (logger.isDebugEnabled())
                    logger.debug("Reached stopReadThreshold. Setting channel AUTO_READ to false (Waiting while response builder will read buffers from the stream)");
                channel.config().setAutoRead(false);
            }
        }
    }

    public void onError(Throwable error) {
        if (errorRef.compareAndSet(null, new IOException(error)))
            sourceClosed.set(true);        
    }

    public void onComplete() {
        sourceClosed.set(true);
    }
    
    public void forceComplete() {
        if (sourceClosed.get() && buffers.isEmpty())
            return;
        onComplete();
        int size = buffers.size();
        if (size>0) {
            ArrayList<ByteBuf> _buffers = new ArrayList<>(size);
            buffers.drainTo(_buffers);
            for (ByteBuf buf: _buffers)
                buf.release();
        }
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
                    else {
                        buffers.poll().release();
                        if (buffers.remainingCapacity() > stopReadThreshold && channel.isActive() && !channel.config().isAutoRead()) {
                            if (logger.isDebugEnabled())
                                logger.debug("Setting channel AUTO_READ to TRUE by sending ENABLE_AUTO_READ_EVENT to server handler");
                            channel.pipeline().fireUserEventTriggered(HttpServerHandler.ENABLE_AUTO_READ_EVENT);
                        }
                    }
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

        public OverflowException() {
            super("Request input stream overflowed");
        }        
    }
}

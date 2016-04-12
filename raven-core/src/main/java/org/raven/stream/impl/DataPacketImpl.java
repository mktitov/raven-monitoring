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

import org.raven.dp.DataProcessorFacade;
import org.raven.stream.Ack;
import org.raven.stream.DataPacket;
import org.raven.stream.DataPacketHolderMessage;
import org.raven.stream.Reject;

/**
 *
 * @author Mikhail Titov
 */
public class DataPacketImpl<T> implements DataPacket<T> {
    private final Ack<T> ack;
    private final Reject<T> reject;
    private final DataProcessorFacade owner;
    private final int queueId;
    private long seqnum;
    
    private T data;

    public DataPacketImpl(final DataProcessorFacade owner, final int queueId, final T data) {
        this.owner = owner;
        this.queueId = queueId;
        this.ack = new AckImpl();
        this.reject = new RejectImpl();
        this.data = data;
    }
    
    @Override
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }

    @Override
    public void ack() {
        owner.send(ack);
    }

    @Override
    public void reject() {
        owner.send(reject);
    }

    @Override
    public long getSeqNum() {
        return seqnum;
    }

    @Override
    public void setSeqNum(long seqnum) {
        this.seqnum = seqnum;
    }
    
    private class Resp implements DataPacketHolderMessage<T> {
        @Override public DataPacket<T> getDataPacket() {
            return DataPacketImpl.this;
        }

        @Override public int getQueueId() {
            return DataPacketImpl.this.queueId;
        }        
    }

    private class AckImpl extends Resp implements Ack<T> { } 
    private class RejectImpl extends Resp implements Reject<T> { }
}

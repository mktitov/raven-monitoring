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
import org.raven.ds.impl.UnsafeRingQueue;
import org.raven.stream.Ack;
import org.raven.stream.Consts;
import org.raven.stream.DataPacket;
import org.raven.stream.ReceiverReady;
import org.raven.stream.Reject;

/**
 *
 * @author Mikhail Titov
 */
public class OutboundStreamQueue<T> {
    public enum Status {READY, WAITING_ACK, WAITING_RECEIVER_READY, INVALID};
    
    private final UnsafeRingQueue<DataPacket<T>> packetQueue;
    private final UnsafeRingQueue<DataPacket<T>> packetCache;
//    private final UnsafeRingQueue<DataPacket<T>> unconfirmedPackets;
    private final DataProcessorFacade sender;
    private final DataProcessorFacade receiver;
    private final int maxUnconfirmed;
    private final int queueId;
    
    private Status status;
    private long successCache;
    private long unsuccessCache;
    private long reuseCount;
    private long packetsCreationCount;
    private long seqnum;
    private boolean completing;
    private int unconfirmedCount;
    
    public OutboundStreamQueue(final DataProcessorFacade sender, final DataProcessorFacade receiver, 
            final int queueId, final int size, final int maxUnconfirmed) 
    {
        this.sender = sender;
        this.receiver = receiver;
        this.queueId = queueId;
        this.maxUnconfirmed = maxUnconfirmed;
        packetQueue = new UnsafeRingQueue<>(size);
        packetCache = new UnsafeRingQueue<>(size);
        status = Status.READY;
        this.unconfirmedCount = 0;
        this.seqnum = 1;
    };
    
    public Status getStatus() {
        return status;
    }

    public long getSuccessCache() {
        return successCache;
    }

    public long getUnsuccessCache() {
        return unsuccessCache;
    }

    public long getReuseCount() {
        return reuseCount;
    }

    public long getPacketsCreationCount() {
        return packetsCreationCount;
    }
    
    public boolean canSend() {
        return !completing && status!=Status.INVALID && packetQueue.canPush();
    }
    
    public boolean send(T data) {
        if (!canSend())
            return false;
        DataPacket<T> packet = packetCache.pop();
        if (packet==null) {
            packet = new DataPacketImpl<>(sender, queueId, data);
            packetsCreationCount++;
        } else {
            packet.setData(data);
            reuseCount++;
        }
        if (packetQueue.push(packet)) {
            packet.setSeqNum(seqnum++);
            trySendToReceiver();
            return true;
        } else {
            status = Status.INVALID;
            return false;
        }        
    }
    
    public boolean sendComplete() {
        if (status==Status.INVALID) 
            return false;
        if (!completing) {
            completing = true;
            trySendToReceiver();
        }
        return true;
    }
    
    private void trySendToReceiver() {
        if (status==Status.READY || (status==Status.WAITING_ACK && unconfirmedCount<maxUnconfirmed)) {
            DataPacket<T> packet = packetQueue.peek(unconfirmedCount);
            if (packet!=null) {
//                if (receiver.send(packet)) {
//                    status = Status.WAITING_ACK;
//                    
//                } else {
//                    
//                }
                status = receiver.send(packet)? Status.WAITING_ACK : Status.INVALID;
                unconfirmedCount++;
            } else if (completing) {
                if (!sender.sendTo(receiver, Consts.COMPLETE_MESSAGE)) 
                    status = Status.INVALID;
            }
        }
    }
    
    /**
     * Returns <b>false</b> if received(processing) an invalid ack message
     * @param ack acknowledge message
     */
    public boolean processAckEvent(Ack<T> ack) {
        final DataPacket<T> packet = ack.getDataPacket();
        final DataPacket<T> expectedPacket = packetQueue.pop();
        if (packet!=expectedPacket) 
            return false;
        packet.setData(null);
        if (packetCache.push(packet)) 
            successCache++;
        else
            unsuccessCache++;
        unconfirmedCount--;
        status = unconfirmedCount==0? Status.READY : Status.WAITING_ACK;
        trySendToReceiver();
        return true;
    }
    
    public boolean processRejectEvent(Reject<T> reject) {
        final DataPacket<T> packet = reject.getDataPacket();
        final DataPacket<T> expectedPacket = packetQueue.peek();
        if (packet!=expectedPacket) {
            return false;            
        }
        status = Status.WAITING_RECEIVER_READY;
        unconfirmedCount = 0;
        return true;
    }
    
    public void processReceiverReadyEvent(ReceiverReady receiverReady) {
        status = Status.READY;
        trySendToReceiver();
    }
}

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.raven.dp.DataProcessorFacade;
import org.raven.stream.Ack;
import org.raven.stream.Consts;
import org.raven.stream.DataPacket;
import org.raven.stream.ReceiverReady;
import org.raven.stream.Reject;
import static org.raven.stream.impl.OutboundStreamQueue.Status.*;
/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class ReceiverQueueTest extends Assert {
    
    @Test
    public void successSendOne(
            final @Mocked DataProcessorFacade sender,
            final @Mocked DataProcessorFacade receiver
    ) throws Exception {
        final AtomicReference<DataPacket> packet = new AtomicReference<>();
        final OutboundStreamQueue<String> queue = new OutboundStreamQueue<>(sender, receiver, 1, 2, 1);
        new Expectations() {{
            receiver.send(any); result = new Delegate() {
                public boolean send(DataPacket data) {
                    assertNotNull(data);
                    packet.set(data);
                    return true;
                }
            }; times = 2;
            
            sender.send(any); result = new Delegate() {
                public boolean send(Ack ack) {
                    queue.processAckEvent(ack);
                    return true;
                };
            }; times = 2;
            sender.sendTo(receiver, Consts.COMPLETE_MESSAGE);
        }};
        assertEquals(READY, queue.getStatus());
        assertTrue(queue.send("test1"));
        assertEquals(WAITING_ACK, queue.getStatus());
        assertTrue(queue.send("test2"));
        assertEquals(WAITING_ACK, queue.getStatus());
        assertFalse(queue.send("test3"));
        assertEquals(WAITING_ACK, queue.getStatus());
        
        assertNotNull(packet.get());
        assertEquals("test1", packet.get().getData());
        packet.get().ack();
        assertEquals(WAITING_ACK, queue.getStatus());
        
        assertNotNull(packet.get());
        assertEquals("test2", packet.get().getData());
        packet.get().ack();
        assertEquals(READY, queue.getStatus());
        queue.sendComplete();
        assertFalse(queue.send("test4"));        
    }
    
    @Test
    public void sendManyAtOnce(
            final @Mocked DataProcessorFacade sender,
            final @Mocked DataProcessorFacade receiver
    ) throws Exception {
        final List<DataPacket> packets = new ArrayList<>();
        final OutboundStreamQueue<String> queue = new OutboundStreamQueue<>(sender, receiver, 1, 3, 2);
        new Expectations() {{
            receiver.send(any); result = new Delegate() {
                public boolean send(DataPacket data) {
                    assertNotNull(data);
                    packets.add(data);
                    return true;
                }
            }; times = 3;
            
            sender.send(any); result = new Delegate() {
                public boolean send(Ack ack) {
                    queue.processAckEvent(ack);
                    return true;
                };
            }; times = 1;
        }};
        assertEquals(READY, queue.getStatus());
        assertTrue(queue.send("test1"));
        assertEquals(WAITING_ACK, queue.getStatus());
        assertTrue(queue.send("test2"));
        assertEquals(WAITING_ACK, queue.getStatus());
        assertTrue(queue.send("test3"));
        assertEquals(WAITING_ACK, queue.getStatus());
        assertFalse(queue.send("test4"));
        
        assertEquals(2, packets.size());
        checkDataPacket(packets.get(0), "test1");
        checkDataPacket(packets.get(1), "test2");
        packets.get(0).ack();
        assertEquals(3, packets.size());
        checkDataPacket(packets.get(2), "test3");
        assertTrue(queue.send("test4"));                
    }
    
    @Test
    public void rejectTest(
            final @Mocked DataProcessorFacade sender,
            final @Mocked DataProcessorFacade receiver,
            final @Mocked ReceiverReady readyEvent
    ) throws Exception {
        final OutboundStreamQueue<String> queue = new OutboundStreamQueue<>(sender, receiver, 1, 1, 1);
        final List<DataPacket> packets = new ArrayList<>();
        final Delegate addPacketDelegate = new Delegate() {
                public boolean send(DataPacket data) {
                    assertNotNull(data);
                    packets.add(data);
                    return true;
                };
        };
        new StrictExpectations(){{
            receiver.send(any); result = addPacketDelegate;
            sender.send((Reject)any); result = new Delegate() {
                public boolean send(Reject reject) {
                    assertTrue(queue.processRejectEvent(reject));
                    return true;
                }
            };
            receiver.send(any); result = addPacketDelegate; 
            sender.send(any); result = new Delegate() {
                public boolean send(Ack ack) {
                    assertTrue(queue.processAckEvent(ack));
                    return true;
                }
            };
            receiver.send(any); result = addPacketDelegate; 
        }};
        assertEquals(READY, queue.getStatus());
        assertTrue(queue.send("test"));
        assertFalse(queue.send("test2"));
        assertEquals(WAITING_ACK, queue.getStatus());
        assertEquals(1, packets.size());
        
        packets.get(0).reject();
        assertEquals(WAITING_RECEIVER_READY, queue.getStatus());
        assertFalse(queue.send("test2"));
        queue.processReceiverReadyEvent(readyEvent);
        assertEquals(2, packets.size());
        assertFalse(queue.send("test2"));
        assertEquals(WAITING_ACK, queue.getStatus());        
        checkDataPacket(packets.get(1), "test");
        
        packets.get(1).ack();
        assertEquals(READY, queue.getStatus());
        assertTrue(queue.send("test2"));
        assertEquals(3, packets.size());
        checkDataPacket(packets.get(2), "test2");
    }
    
    @Test
    public void dataPacketReuseTest(
            final @Mocked DataProcessorFacade sender,
            final @Mocked DataProcessorFacade receiver
    ) throws Exception {
        final OutboundStreamQueue<String> queue = new OutboundStreamQueue<>(sender, receiver, 1, 1, 1);
        final List<DataPacket> packets = new ArrayList<>();
        new Expectations(){{
            receiver.send(any); result = new Delegate() {
                public boolean send(DataPacket data) {
                    assertNotNull(data);
                    packets.add(data);
                    return true;
                }
            }; times = 2;
            
            sender.send(any); result = new Delegate() {
                public boolean send(Ack ack) {
                    queue.processAckEvent(ack);
                    return true;
                };
            }; times = 1;            
        }};
        assertTrue(queue.send("test"));
        assertEquals(1, packets.size());
        checkDataPacket(packets.get(0), "test");
        packets.get(0).ack();
        assertTrue(queue.send("test1"));
        assertEquals(2, packets.size());
        checkDataPacket(packets.get(1), "test1");
        assertSame(packets.get(0), packets.get(1));
    }
    
    private void checkDataPacket(DataPacket packet, String data) {
        assertNotNull(packet);
        assertEquals(data, packet.getData());
    }
    
}

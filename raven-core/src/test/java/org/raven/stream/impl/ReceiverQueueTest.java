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

import java.util.concurrent.atomic.AtomicReference;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.raven.dp.DataProcessorFacade;
import org.raven.stream.Ack;
import org.raven.stream.Consts;
import org.raven.stream.DataPacket;
import static org.raven.stream.impl.ReceiverQueue.Status.*;
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
        final ReceiverQueue<String> queue = new ReceiverQueue<>(sender, receiver, 1, 2, 1);
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
        
//        new Verifications() {{
//            DataPacket<String> packet;
//            receiver.send(packet = withCapture());
//            assertNotNull(packet);
//            assertEquals("test1", packet.getData());
//        }};
    }
    
    @Test
    public void sendManyAtOnce(
            final @Mocked DataProcessorFacade sender,
            final @Mocked DataProcessorFacade receiver
    ) throws Exception {
        final AtomicReference<DataPacket> packet = new AtomicReference<>();
        final ReceiverQueue<String> queue = new ReceiverQueue<>(sender, receiver, 1, 2, 2);
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
    
}

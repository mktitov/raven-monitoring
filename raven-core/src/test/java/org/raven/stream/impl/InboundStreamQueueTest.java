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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Mikhail Titov
 */
public class InboundStreamQueueTest {
    
    @Test
    public void test() {
        InboundStreamQueue<String> queue = new InboundStreamQueue<>(1);
        assertTrue(queue.push("test", 1));
        assertFalse(queue.push("test2", 2));
        InboundStreamQueue.Element<String> elem = queue.peek();
        checkElement(elem, "test", 1, queue);
        //
        assertFalse(queue.push("test2", 2));
        assertSame(elem, queue.pop());
        assertTrue(queue.push("test2", 2));
        InboundStreamQueue.Element<String> elem2 = queue.pop();
        checkElement(elem, "test2", 2, queue);
        assertSame(elem, elem2);
    }
    
    private void checkElement(InboundStreamQueue.Element elem, Object data, long seqnum, InboundStreamQueue queue) {
        assertNotNull(elem);
        assertEquals(data, elem.getData());
        assertEquals(seqnum, elem.getSeqnum());
        assertSame(queue, elem.getQueue());
    }
}

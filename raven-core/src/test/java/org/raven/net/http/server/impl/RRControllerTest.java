/*
 * Copyright 2016 Mikhail Titov .
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
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Mikhail Titov
 */
public class RRControllerTest {
    
    @Test
    public void testHttpHeaders() {
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.add("Content-Type", "text/plain");
        assertEquals("text/plain", headers.get("Content-Type"));
        assertEquals("text/plain", headers.get("Content-TYPE"));
    }
    
    @Test
    public void testEmptyBuffer() {
        ByteBuf buf = Unpooled.EMPTY_BUFFER;
        System.out.println("buf class: "+buf.getClass().getName());
        System.out.println("refCnt: "+buf.refCnt());
        buf.release();
        System.out.println("refCnt: "+buf.refCnt());
        buf.release();
        System.out.println("refCnt: "+buf.refCnt());
    }
}

/*
 * Copyright 2014 Mikhail Titov.
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
import io.netty.buffer.Unpooled;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class CometInputStreamTest extends Assert {
    private byte[] result;
    
    @Test(timeout = 10000)
    public void test() throws Exception {
        byte[] data = new byte[]{1,2,3,4};
        final CometInputStreamImpl stream = new CometInputStreamImpl(2);
        
        Thread reader = new Thread() {
            @Override public void run() {
                try {
                    result = IOUtils.toByteArray(stream);
                } catch (IOException ex) {
                    fail();
                }
            }
        };
        assertTrue(stream.canPushBuffer());
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{1,2});
        assertEquals(2, buf.getByte(buf.writerIndex()-1));
        while (buf.isReadable())
            System.out.println("BUF DATA: "+buf.readByte());
        stream.pushBuffer(Unpooled.wrappedBuffer(new byte[]{1,2}));
        assertTrue(stream.canPushBuffer());
        stream.pushBuffer(Unpooled.wrappedBuffer(new byte[]{3,4}));
        assertFalse(stream.canPushBuffer());
        reader.start();
        Thread.sleep(500);
        stream.dataStreamClosed();
        reader.join();
        assertArrayEquals(data, result);
    }
}

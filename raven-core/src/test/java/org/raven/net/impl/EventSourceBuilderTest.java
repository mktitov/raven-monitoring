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

package org.raven.net.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.BindingNames;
import org.raven.auth.UserContext;
import org.raven.ds.DataContext;
import org.raven.net.EventSourceChannel;
import org.raven.net.Request;
import org.raven.net.Response;
import org.raven.net.ResponseContext;

import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class EventSourceBuilderTest extends RavenCoreTestCase {
    private PushDataSource ds;
    private DataCollector collector;
    private EventSourceBuilder eventsSource;
    private UserContext user;
    private ResponseContext responseContext;
    private boolean flushed = false;
    private byte[] writtenBytes;
    
    @Before
    public void prepare() {
        ds = new PushDataSource();
        ds.setName("events data source");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        eventsSource = new EventSourceBuilder();
        eventsSource.setName("eventsSource");
        testsNode.addAndSaveChildren(eventsSource);
        eventsSource.setDataSource(ds);
        assertTrue(eventsSource.start());
        
        collector = new DataCollector();
        collector.setName("new channel events receiver");
        testsNode.addAndSaveChildren(collector);
        collector.setDataSource(eventsSource);
        assertTrue(collector.start());
    }
    
    @Test
    public void channelCreationTest() throws Exception {
        IMocksControl mocks = createControl();
        trainChannelCreation(mocks);
        mocks.replay();
        
        Object response = eventsSource.buildResponseContent(user, responseContext);
        assertSame(Response.MANAGING_BY_BUILDER, response);
        assertTrue(flushed);
        
        assertEquals(1, collector.getDataListSize());
        Object obj = collector.getDataList().get(0);
        assertTrue(obj instanceof EventSourceChannel);
        DataContext context = collector.getLastDataContext();
        assertNotNull(context);
        assertSame(obj, context.getAt(BindingNames.CHANNEL_BINDING));
        
        mocks.verify();
    }

    @Test
    public void sendMessageTest() throws Exception {
        IMocksControl mocks = createControl();
        trainChannelCreation(mocks);
        expect(responseContext.getResponseStream()).andReturn(new TestOutputStream());
        mocks.replay();
        
        eventsSource.buildResponseContent(user, responseContext);
        ds.pushData("test");
        assertNotNull(writtenBytes);
        assertEquals("data: test\n\n", new String(writtenBytes, "utf-8"));
        
        mocks.verify();        
    }
    
    @Test
    public void sendMessageForSelectedChannel() throws Exception {
        IMocksControl mocks = createControl();
        trainChannelCreation(mocks);
        mocks.replay();
       
        eventsSource.buildResponseContent(user, responseContext);
        
        IMocksControl mocks2 = createControl();
        trainChannelCreation(mocks2);
        expect(responseContext.getResponseStream()).andReturn(new TestOutputStream());
        
        eventsSource.buildResponseContent(user, responseContext);
        
        
        
        mocks2.verify();
        mocks.verify();        
    }
    
    @Test
    public void asyncMessageSendTest() throws Exception {
        
    }
    
    private void trainChannelCreation(IMocksControl mocks) throws IOException {
        responseContext = mocks.createMock(ResponseContext.class);
        trainToStringMocks(mocks, responseContext);
        Map<String, String> headers = mocks.createMock(Map.class);
        expect(responseContext.getHeaders()).andReturn(headers);
        expect(headers.put("Content-Type", "text/event-stream")).andReturn(null);
        expect(responseContext.getResponseStream()).andReturn(new TestOutputStream());
        responseContext.closeChannel();
        expectLastCall().anyTimes();
    }
    
    private void trainToStringMocks(IMocksControl mocks, ResponseContext context) {
        Request request = mocks.createMock(Request.class);
        user = mocks.createMock(UserContext.class);
        expect(context.getRequest()).andReturn(request).anyTimes();
        expect(request.getRemoteAddr()).andReturn("client-host").anyTimes();
        expect(user.getLogin()).andReturn("testUser").anyTimes();
    }
    
    private class TestOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void write(byte[] b) throws IOException {
            writtenBytes = b;
        }                

        @Override
        public void flush() throws IOException {
            flushed = true;
        }
    }
}

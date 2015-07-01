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
import java.util.Collection;
import java.util.Map;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.BindingNames;
import static org.raven.RavenUtils.*;
import org.raven.auth.UserContext;
import org.raven.ds.DataContext;
import org.raven.ds.impl.DataContextImpl;
import org.raven.log.LogLevel;
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
        eventsSource.setLogLevel(LogLevel.TRACE);
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
        TestOutputStream responseStream = new TestOutputStream();
        trainChannelCreation(mocks, responseStream);
        mocks.replay();
        
        Object response = eventsSource.buildResponseContent(user, responseContext);
        assertSame(Response.MANAGING_BY_BUILDER, response);
        assertTrue(responseStream.flushed);
        Collection<EventSourceChannel> channels = eventsSource.getChannels();
        assertNotNull(channels);
        assertEquals(1, channels.size());
        EventSourceChannel channel = channels.iterator().next();
        assertNotNull(channel);
        assertSame(user, channel.getUser());
        assertSame(responseContext.getRequest(), channel.getRequest());
        
        
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
        TestOutputStream responseStream = new TestOutputStream();
        trainChannelCreation(mocks, responseStream);
        expect(responseContext.getResponseStream()).andReturn(responseStream);
        mocks.replay();
        
        eventsSource.buildResponseContent(user, responseContext);
        ds.pushData("test");
        assertTrue(responseStream.flushed);
        assertNotNull(responseStream.writtenBytes);
        assertEquals("data: test\n\n", new String(responseStream.writtenBytes, "utf-8"));
        
        mocks.verify();        
    }
    
    @Test
    public void sendMessageInMapTest() throws Exception {
        IMocksControl mocks = createControl();
        TestOutputStream responseStream = new TestOutputStream();
        trainChannelCreation(mocks, responseStream);
        expect(responseContext.getResponseStream()).andReturn(responseStream);
        mocks.replay();
        
        eventsSource.setUseChannelSelector(true);
        eventsSource.getAttr(EventSourceBuilder.CHANNEL_SELECTOR_ATTR).setValue("data instanceof java.util.Map");
        eventsSource.buildResponseContent(user, responseContext);
        ds.pushData(asMap(pair(EventSourceBuilder.MESSAGE_KEY, "test")));
        assertTrue(responseStream.flushed);
        assertNotNull(responseStream.writtenBytes);
        assertEquals("data: test\n\n", new String(responseStream.writtenBytes, "utf-8"));
        
        mocks.verify();        
    }
    
    @Test
    public void sendMessageForSelectedChannel() throws Exception {
        IMocksControl mocks = createControl();
        TestOutputStream responseStream = new TestOutputStream();
        trainChannelCreation(mocks, responseStream);
        mocks.replay();
       
        eventsSource.buildResponseContent(user, responseContext);
        
        IMocksControl mocks2 = createControl();
        TestOutputStream responseStream2 = new TestOutputStream();
        trainChannelCreation(mocks2, responseStream2);
        expect(responseContext.getResponseStream()).andReturn(responseStream2);
        mocks2.replay();
        
        eventsSource.buildResponseContent(user, responseContext);
        DataContext context = new DataContextImpl();
        assertTrue(collector.getLastData() instanceof EventSourceChannel);
        context.putAt(BindingNames.CHANNEL_BINDING, collector.getLastData());
        responseStream.reset();
        responseStream2.reset();
        ds.pushData("test", context);
        
        assertNull(responseStream.writtenBytes);
        assertFalse(responseStream.flushed);
        assertNotNull(responseStream2.writtenBytes);
        assertTrue(responseStream2.flushed);
        assertEquals("data: test\n\n", new String(responseStream2.writtenBytes, "utf-8"));
        
        mocks2.verify();
        mocks.verify();        
    }
    
    @Test
    public void channelSelectorTest() throws Exception {
        IMocksControl mocks = createControl();
        TestOutputStream responseStream = new TestOutputStream();
        trainChannelCreation(mocks, responseStream);
        expect(user.getName()).andReturn("User1");
        mocks.replay();
       
        eventsSource.buildResponseContent(user, responseContext);
        
        IMocksControl mocks2 = createControl();
        TestOutputStream responseStream2 = new TestOutputStream();
        trainChannelCreation(mocks2, responseStream2);
        expect(responseContext.getResponseStream()).andReturn(responseStream2);
        expect(user.getName()).andReturn("User2");
        mocks2.replay();
        
        eventsSource.buildResponseContent(user, responseContext);
        DataContext context = new DataContextImpl();
        context.putAt("param1", "val1");
        responseStream.reset();
        responseStream2.reset();
        
        eventsSource.getAttr("channelSelector").setValue("channel.user.name=='User2' && data=='test' && context['param1']=='val1'");
        eventsSource.setUseChannelSelector(true);
        ds.pushData("test", context);
        
        assertNull(responseStream.writtenBytes);
        assertFalse(responseStream.flushed);
        assertNotNull(responseStream2.writtenBytes);
        assertTrue(responseStream2.flushed);
        assertEquals("data: test\n\n", new String(responseStream2.writtenBytes, "utf-8"));
        
        mocks2.verify();
        mocks.verify();        
    }
    
    @Test
    public void asyncMessageSendTest() throws Exception {
        
    }
    
    private void trainChannelCreation(IMocksControl mocks, OutputStream responseStream) throws IOException {
        responseContext = mocks.createMock(ResponseContext.class);
        trainToStringMocks(mocks, responseContext);
        Map<String, String> headers = mocks.createMock(Map.class);
        responseContext.addListener(eventsSource);
        expect(responseContext.getHeaders()).andReturn(headers);
        expect(headers.put("Content-Type", "text/event-stream")).andReturn(null);
        expect(responseContext.getResponseStream()).andReturn(responseStream);
        responseContext.closeChannel();
        expectLastCall().anyTimes();
    }
    
    private void trainToStringMocks(IMocksControl mocks, ResponseContext context) {
        Request request = mocks.createMock(Request.class);
        user = mocks.createMock(UserContext.class);
        expect(context.getRequest()).andReturn(request).anyTimes();
        expect(request.getRemoteAddr()).andReturn("client-host").anyTimes();
        expect(request.getRemotePort()).andReturn(123).anyTimes();
        expect(user.getLogin()).andReturn("testUser").anyTimes();
    }
    
    private class TestOutputStream extends OutputStream {
        private boolean flushed = false;
        private byte[] writtenBytes = null;
        
        public void reset() {
            flushed = false;
            writtenBytes = null;
        }
        
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

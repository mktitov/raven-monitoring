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

import java.io.Flushable;
import java.io.OutputStream;
import java.util.Map;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.auth.UserContext;
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
        trainToStringMocks(mocks);
        ResponseContext responseContext = mocks.createMock(ResponseContext.class);
        Map<String, String> headers = mocks.createMock(Map.class);
        Flushable responseStream = mocks.createMock(Flushable.class);
        expect(responseContext.getHeaders()).andReturn(headers);
        headers.put("Content-Type", "text/event-stream");
        expect(responseContext.getResponseStream()).andReturn(responseStream);
        responseStream.flush();
        mocks.replay();
        
        Response response = eventsSource.buildResponse(user, responseContext);
        assertSame(Response.MANAGING_BY_BUILDER, response);
        
        assertEquals(1, collector.getDataListSize());
        Object obj = collector.getDataList().get(0);
        assertTrue(obj instanceof EventSourceChannel);
        
        mocks.verify();
    }
    
    private void trainToStringMocks(IMocksControl mocks) {
        Request request = mocks.createMock(Request.class);
        user = mocks.createMock(UserContext.class);
        expect(request.getRemoteAddr()).andReturn("client-host").anyTimes();
        expect(user.getName()).andReturn("testUser").anyTimes();
    }
}

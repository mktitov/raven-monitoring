/*
 *  Copyright 2009 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.net.http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
@Ignore
public class HttpResponseHandlerNodeTest extends RavenCoreTestCase
{
    private HttpRequestNode responseHandler;

    @Before
    public void prepare()
    {
        responseHandler = new HttpRequestNode();
        responseHandler.setName("response handler");
        tree.getRootNode().addAndSaveChildren(responseHandler);
        
    }

    @Test
    public void nullResponseTest() throws Exception
    {
        HttpResponse response = createMock(HttpResponse.class);
        expect(response.getEntity()).andReturn(null);
        replay(response);

        responseHandler.setUri("/test");

        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> responseMap = new HashMap<String, Object>();
        Map<String, Object> requestMap = new HashMap<String, Object>();
        requestMap.put(HttpSessionNode.HEADERS, new HashMap<String, String>());
        requestMap.put(HttpSessionNode.PARAMS, new HashMap<String, String>());
        params.put(HttpSessionNode.RESPONSE, responseMap);
        params.put(HttpSessionNode.REQUEST, requestMap);
        responseMap.put(HttpSessionNode.RESPONSE_RESPONSE, response);
        responseHandler.getNodeAttribute(HttpResponseHandlerNode.PROCESS_RESPONSE_ATTR).setValue("response.response");

        assertSame(response, responseHandler.processResponse(params));

        verify(response);
    }

    @Test
    public void binaryResponseTest() throws Exception
    {
        responseHandler.setResponseContentType(ResponseContentType.BINARY);

        HttpResponse response = createMock(HttpResponse.class);
        InputStream is = new ByteArrayInputStream(new byte[]{0, 1});
        HttpEntity entity = createMock(HttpEntity.class);
        expect(response.getEntity()).andReturn(entity);
        expect(entity.getContent()).andReturn(is);
        entity.consumeContent();
        replay(response, entity);

        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> responseMap = new HashMap<String, Object>();
        params.put(HttpSessionNode.RESPONSE, responseMap);
        responseMap.put(HttpSessionNode.RESPONSE_RESPONSE, response);
        responseHandler.getNodeAttribute(HttpResponseHandlerNode.PROCESS_RESPONSE_ATTR).setValue("response.content");

        assertSame(is, responseHandler.processResponse(params));

        verify(response, entity);
    }

    @Test
    public void textResponseTest() throws Exception
    {
        responseHandler.setResponseContentType(ResponseContentType.TEXT);

        HttpResponse response = createMock(HttpResponse.class);
        InputStream is = new ByteArrayInputStream("тест".getBytes("utf-8"));
        HttpEntity entity = createMock(HttpEntity.class);
        expect(response.getEntity()).andReturn(entity);
        expect(entity.getContent()).andReturn(is);
        entity.consumeContent();
        replay(response, entity);

        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> responseMap = new HashMap<String, Object>();
        params.put(HttpSessionNode.RESPONSE, responseMap);
        responseMap.put(HttpSessionNode.RESPONSE_RESPONSE, response);
        responseHandler.getNodeAttribute(HttpResponseHandlerNode.PROCESS_RESPONSE_ATTR).setValue("response.content");

        assertEquals("тест", responseHandler.processResponse(params));

        verify(response, entity);
    }

    @Test
    public void jsonResponseTest() throws Exception
    {
        responseHandler.setResponseContentType(ResponseContentType.JSON);

        HttpResponse response = createMock(HttpResponse.class);
        InputStream is = new ByteArrayInputStream("{\"prop\":\"тест\"}".getBytes("utf-8"));
        HttpEntity entity = createMock(HttpEntity.class);
        expect(response.getEntity()).andReturn(entity);
        expect(entity.getContent()).andReturn(is);
        entity.consumeContent();
        replay(response, entity);

        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> responseMap = new HashMap<String, Object>();
        params.put(HttpSessionNode.RESPONSE, responseMap);
        responseMap.put(HttpSessionNode.RESPONSE_RESPONSE, response);
        responseHandler.getNodeAttribute(HttpResponseHandlerNode.PROCESS_RESPONSE_ATTR).setValue("response.content.prop");

        assertEquals("тест", responseHandler.processResponse(params));

        verify(response, entity);
    }

    @Test
    public void xmlResponseTest() throws Exception
    {
        responseHandler.setResponseContentType(ResponseContentType.XML);

        HttpResponse response = createMock(HttpResponse.class);
        InputStream is = new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><prop>тест</prop>".getBytes("utf-8"));
        HttpEntity entity = createMock(HttpEntity.class);
        expect(response.getEntity()).andReturn(entity);
        expect(entity.getContent()).andReturn(is);
        entity.consumeContent();
        replay(response, entity);

        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> responseMap = new HashMap<String, Object>();
        params.put(HttpSessionNode.RESPONSE, responseMap);
        responseMap.put(HttpSessionNode.RESPONSE_RESPONSE, response);
        responseHandler.getNodeAttribute(HttpResponseHandlerNode.PROCESS_RESPONSE_ATTR).setValue("response.content.text()");

        assertEquals("тест", responseHandler.processResponse(params));

        verify(response, entity);
    }

    @Test
    public void htmlResponseTest() throws Exception
    {
        responseHandler.setResponseContentType(ResponseContentType.HTML);

        HttpResponse response = createMock(HttpResponse.class);
        InputStream is = new ByteArrayInputStream("<html><body><br>тест</body></html>".getBytes("utf-8"));
        HttpEntity entity = createMock(HttpEntity.class);
        expect(response.getEntity()).andReturn(entity);
        expect(entity.getContent()).andReturn(is);
        entity.consumeContent();
        replay(response, entity);

        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> responseMap = new HashMap<String, Object>();
        params.put(HttpSessionNode.RESPONSE, responseMap);
        responseMap.put(HttpSessionNode.RESPONSE_RESPONSE, response);
        responseHandler.getNodeAttribute(HttpResponseHandlerNode.PROCESS_RESPONSE_ATTR).setValue("response.content.body.toString()");

        assertEquals("тест", responseHandler.processResponse(params));

        verify(response, entity);
    }
}
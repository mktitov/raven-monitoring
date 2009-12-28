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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class HttpRequestNodeTest extends RavenCoreTestCase
{
    private HttpRequestNode requestNode;

    @Before
    public void prepare()
    {
        requestNode = new HttpRequestNode();
        requestNode.setName("requestNode");
        tree.getRootNode().addAndSaveChildren(requestNode);
        requestNode.setHost("host");
    }

    @Test
    public void test() throws Exception
    {
        HttpResponse response = createMock(HttpResponse.class);
        expect(response.getEntity()).andReturn(null);
        replay(response);

        requestNode.setUri("/test");
        requestNode.getNodeAttribute(HttpRequestNode.PROCESS_RESPONSE_ATTR).setValue(
                "request.uri+='_test'; request.params.p1='v1'; request.params.p2='v2'; request.headers.h1='v2'");
        assertTrue(requestNode.start());

        Map params = createParams(response);
        Object res = requestNode.processResponse(params);
        assertNotNull(res);
        assertTrue(res instanceof HttpGet);
        Map requestMap = (Map) params.get(HttpSessionNode.REQUEST);

        assertEquals("host", requestMap.get(HttpSessionNode.HOST));
        assertEquals(new Integer(80), requestMap.get(HttpSessionNode.PORT));
        
        HttpGet req = (HttpGet) res;
        String uri = req.getURI().toString();
        assertTrue("/test_test?p1=v1&p2=v2".equals(uri) || "/test_test?p2=v2&p1=v1".equals(uri));
        assertEquals("v2", req.getFirstHeader("h1").getValue());

        verify(response);
    }

    @Test
    public void requestParamsEncodingTest() throws Exception
    {
        HttpResponse response = createMock(HttpResponse.class);
        expect(response.getEntity()).andReturn(null);
        replay(response);

        requestNode.setUri("/test");
        requestNode.getNodeAttribute(HttpRequestNode.PROCESS_RESPONSE_ATTR).setValue("request.params.p1=' '");
        assertTrue(requestNode.start());

        Object res = requestNode.processResponse(createParams(response));
        assertNotNull(res);
        assertTrue(res instanceof HttpGet);

        HttpGet req = (HttpGet) res;
        String uri = req.getURI().toString();
        assertEquals("/test?p1=+", req.getURI().toString());

        verify(response);
    }

    @Test
    public void xWwwFormUrlEncodedTest() throws Exception
    {
        HttpResponse response = createMock(HttpResponse.class);
        expect(response.getEntity()).andReturn(null);
        replay(response);

        requestNode.setRequestContentType(RequestContentType.X_WWW_FORM_URLENCODED);
        requestNode.setRequestType(RequestType.POST);
        requestNode.setUri("/test");
        requestNode.getNodeAttribute(HttpRequestNode.PROCESS_RESPONSE_ATTR).setValue("request.params.p1='test'");
        assertTrue(requestNode.start());

        Object res = requestNode.processResponse(createParams(response));
        assertNotNull(res);
        assertTrue(res instanceof HttpPost);

        HttpPost req = (HttpPost) res;
        String uri = req.getURI().toString();
        assertEquals("/test", req.getURI().toString());
        assertEquals("application/x-www-form-urlencoded", req.getFirstHeader("Content-Type").getValue());
        HttpEntity entity = req.getEntity();
        assertNotNull(entity);
        String text = IOUtils.toString(entity.getContent(), "utf-8");
        assertEquals("p1=test", text);

        verify(response);
    }

    private Map<String, Object> createParams(HttpResponse response)
    {
        Map<String, Object> params = new HashMap<String, Object>();
        Map responseMap = new HashMap();
        responseMap.put(HttpSessionNode.RESPONSE_RESPONSE, response);
        params.put(HttpSessionNode.RESPONSE, responseMap);

        Map requestMap = new HashMap();
        requestMap.put(HttpSessionNode.HEADERS, new HashMap());
        requestMap.put(HttpSessionNode.PARAMS, new HashMap());
        params.put(HttpSessionNode.REQUEST, requestMap);

        return params;
    }
}
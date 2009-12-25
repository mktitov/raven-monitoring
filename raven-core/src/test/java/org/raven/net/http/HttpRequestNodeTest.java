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
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
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

        Map<String, Object> params = new HashMap<String, Object>();
        Map responseMap = new HashMap();
        responseMap.put(HttpSessionNode.RESPONSE_RESPONSE, response);
        params.put(HttpSessionNode.RESPONSE, responseMap);

        Object res = requestNode.processResponse(params);
        assertNotNull(res);
        assertTrue(res instanceof HttpGet);
        
        HttpGet req = (HttpGet) res;
        String uri = req.getURI().toString();
        assertTrue("/test_test?p1=v1&p2=v2".equals(uri) || "/test_test?p2=v2&p1=v1".equals(uri));
        assertEquals("v2", req.getFirstHeader("h1").getValue());

        verify(response);
    }
}
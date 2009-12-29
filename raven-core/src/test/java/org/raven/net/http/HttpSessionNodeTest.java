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

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class HttpSessionNodeTest extends RavenCoreTestCase
{
    private Server server;
    private HttpSessionNode session;
    private PushDataSource datasource;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        server = new Server(9999);

        datasource = new PushDataSource();
        datasource.setName("datasource");
        tree.getRootNode().addAndSaveChildren(datasource);
        assertTrue(datasource.start());

        session = new HttpSessionNode();
        session.setName("http session");
        tree.getRootNode().addAndSaveChildren(session);
        session.setHost("localhost");
        session.setPort(9999);
        session.setDataSource(datasource);
        assertTrue(session.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(session);
        assertTrue(collector.start());
        
    }

    @After
    @Override
    public void finalize() throws Exception
    {
        if (server!=null && server.isStarted())
            server.stop();
    }

    @Test
    public void test() throws Exception
    {
        createRequest("request", "/test", RequestContentType.NONE, null);
        createResponse("response handler", ResponseContentType.TEXT, "response.content", null);

        Handler handler = new Handler1();
        server.setHandler(handler);
        server.start();
        
        datasource.pushData("test");
        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("response", data.get(0));
    }

    @Test
    public void dataTest() throws Exception
    {
        createRequest("request", "/dummy", RequestContentType.NONE, "request.uri='/'+data");
        createResponse("response handler", ResponseContentType.TEXT, "data", null);

        Handler handler = new Handler1();
        server.setHandler(handler);
        server.start();

        String reqData = "test";
        datasource.pushData(reqData);
        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertSame(reqData, data.get(0));
    }

    @Test
    public void skipDataTest() throws Exception
    {
        createRequest("request", "/test", RequestContentType.NONE, null);
        createResponse("response handler", ResponseContentType.TEXT, "SKIP_DATA", null);

        Handler handler = new Handler1();
        server.setHandler(handler);
        server.start();

        datasource.pushData("test");
        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(0, data.size());
    }

    @Test
    public void errorHandlerTest() throws Exception
    {
        session.setErrorHandler("response.response.statusLine.statusCode");

        createRequest("request", "/test", RequestContentType.NONE, null);
        createResponse("response handler", ResponseContentType.TEXT, "data", HttpServletResponse.SC_OK);

        Handler1 handler = new Handler1();
        handler.setResponseStatus(HttpServletResponse.SC_BAD_REQUEST);
        server.setHandler(handler);
        server.start();

        datasource.pushData("test");
        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals(new Integer(400), data.get(0));
    }

    @Test
    public void errorHandlerSkipDataTest() throws Exception
    {
        session.setErrorHandler("SKIP_DATA");

        createRequest("request", "/test", RequestContentType.NONE, null);
        createResponse("response handler", ResponseContentType.TEXT, "data", HttpServletResponse.SC_OK);

        Handler1 handler = new Handler1();
        handler.setResponseStatus(HttpServletResponse.SC_BAD_REQUEST);
        server.setHandler(handler);
        server.start();

        datasource.pushData("test");
        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(0, data.size());
    }

    @Test
    public void authTest() throws Exception
    {
        
    }

    private HttpResponseHandlerNode createResponse(
            String nodeName, ResponseContentType responseType, String processResponseScript, Integer responseStatusCode)
    {
        HttpResponseHandlerNode response = new HttpResponseHandlerNode();
        response.setName(nodeName);
        session.addAndSaveChildren(response);
        response.setResponseContentType(responseType);
        response.setProcessResponse(processResponseScript);
        response.setExpectedResponseStatusCode(responseStatusCode);
        assertTrue(response.start());

        return response;
    }

    private HttpRequestNode createRequest(
            String name, String uri, RequestContentType contentType, String processResponseScript)
    {
        HttpRequestNode request = new HttpRequestNode();
        request.setName(name);
        session.addAndSaveChildren(request);
        request.setUri(uri);
        request.setRequestContentType(contentType);
        request.setProcessResponse(processResponseScript);
        assertTrue(request.start());

        return request;
    }

    private class Handler1 extends AbstractHandler
    {
        private int responseStatus = HttpServletResponse.SC_OK;

        public int getResponseStatus() {
            return responseStatus;
        }

        public void setResponseStatus(int responseStatus) {
            this.responseStatus = responseStatus;
        }

        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException
        {
            assertEquals("/test", target);
            baseRequest.setHandled(true);
            response.setContentType("text/plain");
            response.setStatus(responseStatus);
            response.getWriter().write("response");
        }
    }
}
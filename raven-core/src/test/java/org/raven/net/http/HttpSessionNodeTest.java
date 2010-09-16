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
import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.raven.ds.DataContext;
import org.raven.ds.impl.DataContextImpl;
import org.raven.expr.impl.IfNode;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;

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
    private ExecutorServiceNode executor;

    @Before
    public void prepare()
    {
        server = new Server(9999);

        datasource = new PushDataSource();
        datasource.setName("datasource");
        tree.getRootNode().addAndSaveChildren(datasource);
        assertTrue(datasource.start());

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        assertTrue(executor.start());

        session = new HttpSessionNode();
        session.setName("http session");
        tree.getRootNode().addAndSaveChildren(session);
        session.setHost("localhost");
        session.setPort(9999);
        session.setDataSource(datasource);
        session.setMaxHandlersCount(1);
        session.setHandleDataInSeparateThread(Boolean.FALSE);
        session.setExecutor(executor);
        session.setLogLevel(LogLevel.TRACE);
        assertTrue(session.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(session);
        assertTrue(collector.start());
    }

    @After
    @Override
    public void finalize() throws Throwable
    {
        if (server!=null && server.isStarted())
            server.stop();
        super.finalize();
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
    public void maxErrorsTest() throws Exception
    {
        session.setErrorHandler("response.response.statusLine.statusCode");
        session.setMaxPercentOfErrors(1);
        session.setCheckMaxPercentOfErrorsAfter(5);

        createRequest("request", "/test", RequestContentType.NONE, null);
        createResponse("response handler", ResponseContentType.TEXT, "data", HttpServletResponse.SC_OK);

        Handler1 handler = new Handler1();
        handler.setResponseStatus(HttpServletResponse.SC_BAD_REQUEST);
        server.setHandler(handler);
        server.start();

        DataContext context = new DataContextImpl();
        for (int i=0; i<10; i++)
            datasource.pushData("test", context);
        datasource.pushData(null, context);
        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(7, data.size());
        for (int i=0; i<6; ++i)
            assertEquals(new Integer(400), data.get(i));
        assertNull(data.get(6));
    }

    @Test
    public void maxErrorsTest2() throws Exception
    {
        session.setErrorHandler("response.response.statusLine.statusCode");
        session.setMaxPercentOfErrors(12);
        session.setCheckMaxPercentOfErrorsAfter(5);

        createRequest("request", "/test", RequestContentType.NONE, null);
        createResponse("response handler", ResponseContentType.TEXT, "data", HttpServletResponse.SC_OK);

        Handler1 handler = new Handler1();
        server.setHandler(handler);
        server.start();

        handler.setResponseStatus(HttpServletResponse.SC_OK);
        DataContext context = new DataContextImpl();
        for (int i=0; i<8; i++)
            datasource.pushData("test", context);
        handler.setResponseStatus(HttpServletResponse.SC_BAD_REQUEST);
        for (int i=0; i<10; i++)
            datasource.pushData("test", context);
        datasource.pushData(null, context);
        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(11, data.size());
        for (int i=0; i<10; ++i)
            assertEquals(i<8? "test" : new Integer(400), data.get(i));
        assertNull(data.get(10));
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
        createRequest("request", "/test", RequestContentType.NONE, "SKIP_DATA");
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
        session.setAuthSchema(AuthSchema.BASIC);
        session.setUsername("user");
        session.setPassword("pwd");
        
        createRequest("request", "/test", RequestContentType.NONE, null);
        createResponse("response handler", ResponseContentType.TEXT, "response.content", null);

        Handler1 handler = new Handler1();
        handler.setUseAuth(true);
        server.setHandler(handler);
        server.start();

        datasource.pushData("test");
        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("response", data.get(0));
    }

    @Test
    public void newSessionTest() throws Exception
    {
        createRequest("request", "/test", RequestContentType.NONE, null);
        IfNode if1 = createCondition("if1", "isNewSession");
        HttpResponseHandlerNode responseHandler = createResponse(
                if1, "response handler", ResponseContentType.TEXT, "'newSession'", null);
        IfNode if2 = createCondition("if2", "!isNewSession");
        responseHandler = createResponse(
                if2, "response handler 2", ResponseContentType.TEXT, "'oldSession'", null);

        Handler handler = new Handler1();
        server.setHandler(handler);
        server.start();

        datasource.pushData("test");
        assertArrayEquals(new Object[]{"newSession"}, collector.getDataList().toArray());

        collector.getDataList().clear();
        datasource.pushData("test");
        assertArrayEquals(new Object[]{"oldSession"}, collector.getDataList().toArray());
    }

    private HttpResponseHandlerNode createResponse(
            String nodeName, ResponseContentType responseType, String processResponseScript, Integer responseStatusCode)
    {
        return createResponse(session, nodeName, responseType, processResponseScript, responseStatusCode);
    }

    private HttpResponseHandlerNode createResponse(
            Node owner, String nodeName, ResponseContentType responseType, String processResponseScript
            , Integer responseStatusCode)
    {
        HttpResponseHandlerNode response = new HttpResponseHandlerNode();
        response.setName(nodeName);
        owner.addAndSaveChildren(response);
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

    private IfNode createCondition(String name, String condition) throws Exception
    {
        IfNode node = new IfNode();
        node.setName(name);
        session.addAndSaveChildren(node);
        node.setUsedInTemplate(Boolean.FALSE);
        node.getNodeAttribute(IfNode.EXPRESSION_ATTRIBUTE).setValue(condition);
        assertTrue(node.start());

        return node;
    }

    private class Handler1 extends AbstractHandler
    {
        private int responseStatus = HttpServletResponse.SC_OK;
        private boolean useAuth = false;

        public int getResponseStatus() {
            return responseStatus;
        }

        public void setResponseStatus(int responseStatus) {
            this.responseStatus = responseStatus;
        }

        public boolean isUseAuth() {
            return useAuth;
        }

        public void setUseAuth(boolean useAuth) {
            this.useAuth = useAuth;
        }

        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException
        {
            if (useAuth)
            {
                String requestAuth = request.getHeader("Authorization");
                if (requestAuth==null)
                {
                    response.setHeader(
                            "WWW-Authenticate", "BASIC realm=\"Auth test\"");
                    response.sendError(response.SC_UNAUTHORIZED);
                    return;
                }
                else
                {
                    String userAndPath = new String(Base64.decodeBase64(
                            requestAuth.substring(6).getBytes()));
                    String elems[] = userAndPath.split(":");
                    if (elems.length!=2
                        || !"user".equals(elems[0])
                        || !"pwd".equals(elems[1]))
                    {
                        response.sendError(response.SC_FORBIDDEN);
                        return;
                    }
                }
            }
            assertEquals("/test", target);
            baseRequest.setHandled(true);
            response.setContentType("text/plain");
            response.setStatus(responseStatus);
            response.getWriter().write("response");
        }
    }
}
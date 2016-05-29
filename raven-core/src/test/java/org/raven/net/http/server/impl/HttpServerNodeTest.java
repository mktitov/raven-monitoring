/*
 * Copyright 2016 Mikhail Titov.
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

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import io.netty.handler.codec.http.HttpHeaders;
import java.nio.charset.Charset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.raven.auth.impl.AccessRight;
import org.raven.auth.impl.AnonymousLoginServiceNode;
import org.raven.auth.impl.LoginManagerNode;
import org.raven.log.LogLevel;
import org.raven.net.impl.SimpleResponseBuilder;
import org.raven.prj.Projects;
import org.raven.prj.impl.ProjectNode;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class HttpServerNodeTest extends RavenCoreTestCase {
    private ExecutorServiceNode executor;
    private HttpServerNode httpServer;
    private WebClient webclient;
    private Projects projects;
    
    @Before
    public void prepare() {
        executor = new ExecutorServiceNode();
        executor.setName("Executor service");
        testsNode.addAndSaveChildren(executor);        
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        assertTrue(executor.start());
        
        httpServer = new HttpServerNode();
        httpServer.setName("HTTP server");
        testsNode.addAndSaveChildren(httpServer);
        httpServer.setExecutor(executor);
        httpServer.setPort(7777);
        httpServer.setLogLevel(LogLevel.TRACE);
        projects = tree.getProjectsNode();
        
        webclient = new WebClient(); 
        webclient.setThrowExceptionOnFailingStatusCode(false);
    }
    
    @After
    public void finishTest() {
        httpServer.stop();
//        if (webclient!=null)
//            webclient.
    }
    
    @Test
    public void startTest() throws InterruptedException {
        assertTrue(httpServer.start());
        Thread.sleep(30000);
        httpServer.stop();
        Thread.sleep(1000);
    }
    
    @Test
    public void invalidPathTest() throws Exception {
        assertTrue(httpServer.start()); 
        //
        HtmlPage p = webclient.getPage("http://localhost:7777");
        assertEquals(404, p.getWebResponse().getStatusCode());
        assertTrue(p.asText().contains("Context (/) unavailable"));
        
        p = webclient.getPage("http://localhost:7777/a");
        assertEquals(404, p.getWebResponse().getStatusCode());
        assertTrue(p.asText().contains("Context (/a) unavailable"));
        
        p = webclient.getPage("http://localhost:7777/sri/a");
        assertEquals(404, p.getWebResponse().getStatusCode());
        assertTrue(p.asText().contains("Context (sri/a) unavailable. Can't resolve (a) path element"));
        
        p = webclient.getPage("http://localhost:7777/projects/test");
        assertEquals(404, p.getWebResponse().getStatusCode());
        assertTrue(p.asText().contains("Context (projects/test) unavailable. Can't resolve (test) path element"));        
    }
    
    @Test(timeout = 5000l)
    public void helloWorldTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent("'Hello World!'");
        assertTrue(builder.start());
        
        //do test
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        TextPage p = webclient.getPage("http://localhost:7777/projects/hello/world");
        assertNotNull(p);
        assertEquals(200, p.getWebResponse().getStatusCode());
        assertEquals("text/plain", p.getWebResponse().getContentType());
        assertEquals("12", p.getWebResponse().getResponseHeaderValue(HttpHeaders.Names.CONTENT_LENGTH));
        assertEquals("Hello World!", p.getContent());        
    }
    
    @Test(timeout = 5000l)
    public void chunkedEncodingTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent(
                  "response.headers['Content-Type']='text/plain';"
                + "w = response.responseWriter;"
                + "w << 'Hello'; w.flush(); w << ' World!';" //using flush explicitly sending 'Hello' by network 
                + "ALREADY_COMPOSED");
        assertTrue(builder.start());
        
        //do test
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        TextPage p = webclient.getPage("http://localhost:7777/projects/hello/world");
        assertNotNull(p);
        assertEquals(200, p.getWebResponse().getStatusCode());
        assertEquals("text/plain", p.getWebResponse().getContentType());
//        assertEquals("12", p.getWebResponse().getResponseHeaderValue(HttpHeaders.Names.CONTENT_LENGTH));
        assertEquals("Hello World!", p.getContent());        
    }
    
    @Test(timeout = 5000l)
    public void outputBufferOverflowTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent("response.headers['Content-Type']='text/plain'; "
                + "w = response.responseStream;"
                + "w.write((1..9).join().bytes);"
                + "ALREADY_COMPOSED");
        assertTrue(builder.start());
        
        //do test
        httpServer.setResponseStreamBufferSize(4); //size 4 bytes, so to send '123456789' we need 3 chunks
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        TextPage p = webclient.getPage("http://localhost:7777/projects/hello/world");
        assertNotNull(p);
        assertEquals(200, p.getWebResponse().getStatusCode());
        assertEquals("text/plain", p.getWebResponse().getContentType());
        assertEquals("123456789", p.getContent());        
    }
    
    @Test(timeout = 10000l)
    public void keepAliveTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent("'test'");
        assertTrue(builder.start());
        
        //do test
        httpServer.setKeepAliveTimeout(5000l);
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "keep-alive");        
        TextPage p = webclient.getPage("http://localhost:7777/projects/hello/world");
        assertNotNull(p);
        assertEquals(200, p.getWebResponse().getStatusCode());
        assertEquals("text/plain", p.getWebResponse().getContentType());
        assertEquals("test", p.getContent());        
        
        p = webclient.getPage("http://localhost:7777/projects/hello/world");
        assertNotNull(p);
        assertEquals(200, p.getWebResponse().getStatusCode());
        assertEquals("text/plain", p.getWebResponse().getContentType());
        assertEquals("test", p.getContent());        
        
        assertEquals(1l, httpServer.getConnectionsCount().get());
        assertEquals(1l, httpServer.getActiveConnectionsCount().get());
        Thread.sleep(4000);
        assertEquals(1l, httpServer.getActiveConnectionsCount().get());
        Thread.sleep(3000);
        assertEquals(0l, httpServer.getActiveConnectionsCount().get());
    }
    
    @Test(timeout = 5000l)
    public void closeByTimeoutKeepAliveConnectionTest() throws Exception {
        
    }
    
    @Test(timeout = 5000l)
    public void redirectTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent("redirect('http://localhost:7777/world/hello')");
        assertTrue(builder.start());
        
        //do test
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        webclient.setRedirectEnabled(false);
        TextPage p = webclient.getPage("http://localhost:7777/projects/hello/world");
        assertNotNull(p);
        assertEquals(307, p.getWebResponse().getStatusCode());
        assertEquals("http://localhost:7777/world/hello", p.getWebResponse().getResponseHeaderValue(HttpHeaders.Names.LOCATION));
    }
    
    
    
    private SimpleResponseBuilder createProjectAndBuilder() throws Exception {
        //Prepare the test
        ProjectNode project = new ProjectNode();
        project.setName("hello");
        projects.addAndSaveChildren(project);
        assertTrue(project.start());
        
        //adding anonymous authenticator
        AnonymousLoginServiceNode loginService = new AnonymousLoginServiceNode();
        loginService.setName("anonym");
        project.getNode(LoginManagerNode.NAME).addAndSaveChildren(loginService);
        loginService.setExecutor(executor);
        assertTrue(loginService.start());
        
        //adding response builder node
        SimpleResponseBuilder builder = new SimpleResponseBuilder();
        builder.setName("world");
        project.getWebInterface().addAndSaveChildren(builder);
        builder.setResponseContentCharset(Charset.forName("utf-8"));
        builder.setResponseContentType("text/plain");
        builder.setMinimalAccessRight(AccessRight.NONE);
        builder.setLoginService(loginService);
        builder.setLogLevel(LogLevel.TRACE);
        return builder;
    }
}

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

import com.gargoylesoftware.htmlunit.FormEncodingType;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import io.netty.handler.codec.http.HttpHeaderDateFormat;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.raven.TestScheduler;
import org.raven.auth.impl.AccessRight;
import org.raven.auth.impl.AnonymousLoginServiceNode;
import org.raven.auth.impl.LoginManagerNode;
import org.raven.cache.TemporaryFileManagerNode;
import org.raven.log.LogLevel;
import org.raven.net.http.server.Protocol;
import org.raven.net.impl.FileResponseBuilder;
import org.raven.net.impl.SimpleResponseBuilder;
import org.raven.prj.Projects;
import org.raven.prj.impl.ProjectNode;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Mikhail Titov
 */
//@RunWith(Parameterized.class)
public class HttpServerNodeTest extends RavenCoreTestCase {
    private ExecutorServiceNode executor;
    private HttpServerNode httpServer;
    private WebClient webclient;
    private Projects projects;
    private AnonymousLoginServiceNode loginService;
    
    private Protocol proto;
    
    @Parameterized.Parameters
    public static Collection<Object[]> PARAMS() {
        return Arrays.asList(new Object[]{Protocol.HTTP}, new Object[]{Protocol.HTTPS});
    }

    public HttpServerNodeTest() {
        proto = Protocol.HTTP;
    }

//    public HttpServerNodeTest(Protocol proto) {
//        this.proto = proto;
//    }    
    
    @Before
    public void prepare() throws Exception {
        executor = new ExecutorServiceNode();
        executor.setName("Executor service");
        testsNode.addAndSaveChildren(executor);        
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        assertTrue(executor.start());
        
        TestScheduler scheduler = new TestScheduler();
        scheduler.setName("scheduler");
        testsNode.addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());

        TemporaryFileManagerNode manager = new TemporaryFileManagerNode();
        manager.setName("manager");
        tree.getRootNode().addAndSaveChildren(manager);
        manager.setDirectory("target/");
        manager.setScheduler(scheduler);
        assertTrue(manager.start());
        
        httpServer = new HttpServerNode();
        httpServer.setName("HTTP server");
        testsNode.addAndSaveChildren(httpServer);
        httpServer.setExecutor(executor);
        httpServer.setPort(7777);
        httpServer.setTempFileManager(manager);
        httpServer.setUploadedFilesTempDir("target/upload_tmp");
        httpServer.setLogLevel(LogLevel.TRACE);
//        if (proto==Protocol.HTTPS) {
//            httpServer.setProtocol(proto);
//            httpServer.setKeystorePassword("test123");
//            httpServer.setKeystorePath("src/test/resources/self_signed_keystore.jks");
//        }
        projects = tree.getProjectsNode();        
        webclient = new WebClient(); 
        webclient.setThrowExceptionOnFailingStatusCode(false);
        webclient.setUseInsecureSSL(true);
    }
    
    public void prepareHttps() {
        httpServer.setProtocol(Protocol.HTTPS);
        httpServer.setKeystorePassword("test123");
        httpServer.setKeystorePath("src/test/resources/self_signed_keystore2.jks");
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
//        Thread.sleep(30000);
        httpServer.stop();
//        Thread.sleep(1000);
    }
    
    @Test
    public void httpsStartTest() throws Exception {
        prepareHttps();
        startTest();
    }
    
    @Test
    public void invalidPathTest() throws Exception {
        assertTrue(httpServer.start()); 
        //
        HtmlPage p = webclient.getPage(formUrl("localhost:7777"));
        assertEquals(404, p.getWebResponse().getStatusCode());
        assertTrue(p.asText().contains("Context (/) unavailable"));
        
        p = webclient.getPage(formUrl("localhost:7777/a"));
        assertEquals(404, p.getWebResponse().getStatusCode());
        assertTrue(p.asText().contains("Context (/a) unavailable"));
        
        p = webclient.getPage(formUrl("localhost:7777/sri/a"));
        assertEquals(404, p.getWebResponse().getStatusCode());
        assertTrue(p.asText().contains("Context (sri/a) unavailable. Can't resolve (a) path element"));
        
        p = webclient.getPage(formUrl("localhost:7777/projects/test"));
        assertEquals(404, p.getWebResponse().getStatusCode());
        assertTrue(p.asText().contains("Context (projects/test) unavailable. Can't resolve (test) path element"));        
    }
    
    @Test
    public void httpsInvalidPathTest() throws Exception {
        prepareHttps();
        invalidPathTest();
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
        TextPage p = webclient.getPage(formUrl("localhost:7777/projects/hello/world"));
        assertNotNull(p);
        assertEquals(200, p.getWebResponse().getStatusCode());
        assertEquals("text/plain", p.getWebResponse().getContentType());
        assertEquals("12", p.getWebResponse().getResponseHeaderValue(HttpHeaders.Names.CONTENT_LENGTH));
        assertEquals("Hello World!", p.getContent());        
    }
    
    @Test(timeout = 5000l)
    public void httpsHelloWorldTest() throws Exception {
        prepareHttps();
        helloWorldTest();
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
    
    @Test(timeout=10000)
    public void buildResponseTimeoutTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent("Thread.sleep(2100); 'Hello World!'");
        assertTrue(builder.start());
        
        //do test
        httpServer.setDefaultResponseBuildTimeout(1000L);
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        HtmlPage p = webclient.getPage("http://localhost:7777/projects/hello/world");
        assertNotNull(p);
        assertEquals(500, p.getWebResponse().getStatusCode());
        assertEquals("text/html", p.getWebResponse().getContentType());
        assertTrue(p.asText().contains("Response generation timeout"));
        Thread.sleep(2000L);
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
    
    @Test
    public void notModifiedTest() throws Exception {
        FileResponseBuilder builder = createProjectAndFileBuilder();
        builder.setLastModified(System.currentTimeMillis()-10000);
        builder.getFile().setDataString("Hello world!");
        assert(builder.start());

        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        webclient.addRequestHeader(HttpHeaders.Names.IF_MODIFIED_SINCE, HttpHeaderDateFormat.get().format(new Date()));        
        webclient.setRedirectEnabled(false);        
        WebResponse resp = makeRequest("http://localhost:7777/projects/hello/file");
        assertEquals(HttpResponseStatus.NOT_MODIFIED.code(), resp.getStatusCode());
        
        webclient.addRequestHeader(HttpHeaders.Names.IF_MODIFIED_SINCE, HttpHeaderDateFormat.get().format(
                new Date(System.currentTimeMillis()-20_000)));        
        resp = makeRequest("http://localhost:7777/projects/hello/file");
        assertEquals(200, resp.getStatusCode());
        assertEquals("Hello world!", resp.getContentAsString());
    }
    
    @Test
    public void noContentTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent("null");
        assertTrue(builder.start());
        
        //do test
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        webclient.setRedirectEnabled(false);
        WebResponse resp =makeRequest("http://localhost:7777/projects/hello/world");
        assertNotNull(resp);
        assertEquals(HttpResponseStatus.NO_CONTENT.code(), resp.getStatusCode());
        assertEquals("", resp.getContentAsString());
    }
    
    @Test
    public void queryParametersTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent(""
                + "assert request.params.p1=='v1';"
                + "assert request.params.p2=='v2';"
                + "'ok'");
        assertTrue(builder.start());
        
        //do test
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        WebRequest request = new WebRequest(new URL("http://localhost:7777/projects/hello/world"));
        request.setEncodingType(FormEncodingType.URL_ENCODED);        
        request.setHttpMethod(HttpMethod.GET);
        request.setRequestParameters(Arrays.asList(
                new NameValuePair("p1", "v1"),
                new NameValuePair("p2", "v2")
        ));
        TextPage page = webclient.getPage(request);
        assertNotNull(page);
        assertEquals(200, page.getWebResponse().getStatusCode());
        assertEquals("ok", page.getContent());
    }
    
    @Test
    public void formUrlEncodedTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent(""
                + "assert request.params.p1=='v1';"
                + "assert request.params.p2=='v2';"
                + "'ok'");
        assertTrue(builder.start());
        
        //do test
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        WebRequest request = new WebRequest(new URL("http://localhost:7777/projects/hello/world"));
        request.setEncodingType(FormEncodingType.URL_ENCODED);        
        request.setHttpMethod(HttpMethod.POST);
        request.setRequestParameters(Arrays.asList(
                new NameValuePair("p1", "v1"),
                new NameValuePair("p2", "v2")
        ));
        TextPage page = webclient.getPage(request);
        assertNotNull(page);
        assertEquals(200, page.getWebResponse().getStatusCode());
        assertEquals("ok", page.getContent());
    }
    
    @Test
    public void multipartFormDataTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent(""
                + "assert request.params.p1=='v1';"
                + "assert request.params.p2=='v2';"
                + "'ok'");
        assertTrue(builder.start());
        
        //do test
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        WebRequest request = new WebRequest(new URL("http://localhost:7777/projects/hello/world"));
        request.setEncodingType(FormEncodingType.MULTIPART);
        request.setHttpMethod(HttpMethod.POST);
        request.setRequestParameters(Arrays.asList(
                new NameValuePair("p1", "v1"),
                new NameValuePair("p2", "v2")
        ));
        TextPage page = webclient.getPage(request);
        assertNotNull(page);
        assertEquals(200, page.getWebResponse().getStatusCode());
        assertEquals("ok", page.getContent());
    }
    
    @Test
    public void multipartFileDataTest() throws Exception {
        FileResponseBuilder fileBuilder = createProjectAndFileBuilder();
        fileBuilder.setResponseContentType("text/html");
        fileBuilder.getFile().setDataString(
                "<html><body><form name='form' action='world' enctype='multipart/form-data' method='post'>"
                        + "<input type='file' name='file1'>"
                        + "<input type='text' name='field1'>"
                        + "<input type='submit' id='submitForm'>"
                        + "</form></body></html>");
        assertTrue(fileBuilder.start());
        
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent(""
                + "assert request.params.field1=='v1';"
                + "assert request.params.file1 instanceof javax.activation.DataSource;"
                + "assert request.params.file1.inputStream.text=='Hello World!';"
                + "'ok'");
        assertTrue(builder.start());
        
        //do test
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        HtmlPage page = webclient.getPage("http://localhost:7777/projects/hello/file");
        assertNotNull(page);
        assertEquals(200, page.getWebResponse().getStatusCode());
        HtmlForm form = page.getFormByName("form");
        HtmlFileInput fileInput = form.getInputByName("file1");
        fileInput.setValueAttribute("target/file_to_upload.txt");
        fileInput.setContentType("text/plain");
        fileInput.setData("Hello World!".getBytes());
        
        HtmlTextInput textInput = form.getInputByName("field1");
        textInput.setValueAttribute("v1");
        //
        TextPage resPage = (TextPage) form.getElementById("submitForm").click();
        assertNotNull(resPage);
        assertEquals(200, resPage.getWebResponse().getStatusCode());
        assertEquals("ok", resPage.getContent());
    }
    
    @Test(timeout = 5_000L)
    public void responseContentReadTest() throws Exception{
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent(""
                + "assert request.contentReader.text=='Test';"
                + "'ok'");
        assertTrue(builder.start());
        
        //do test
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        WebRequest request = new WebRequest(new URL("http://localhost:7777/projects/hello/world"), HttpMethod.POST);
        request.setRequestBody("Test");
        request.setAdditionalHeader("Content-Type", "text/plain");
        TextPage page = webclient.getPage(request);
        assertNotNull(page);
        assertEquals(200, page.getWebResponse().getStatusCode());
        assertEquals("ok", page.getContent());
    }
    
    @Test(timeout = 30_000L)
    public void responseContentReadWithBackPressure() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setBuildTimeout(25_000L);
        builder.setResponseContentType("text/plain");
        builder.setResponseContent(""
                + "Thread.sleep(5000); "
                + "def counter=0;"
                + "request.content.eachByte{counter++};"
                + "''+counter");
        assertTrue(builder.start());
        
        //do test
        assertTrue(httpServer.start());
        
        //creating big file 5M
        int size = 5*1024*1024;
        byte[] buf = new byte[size];
        for (int i=0; i<size; i++)
            buf[i] = (byte)32;
        //streaming file to http server as application/octet-stream
        PostMethod httpPost = new PostMethod("http://localhost:7777/projects/hello/world");
        httpPost.setRequestEntity(new ByteArrayRequestEntity(buf, "application/octet-stream"));
        HttpClient client = new HttpClient();
        client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
        client.executeMethod(httpPost);
        assertEquals(""+size, httpPost.getResponseBodyAsString());
    }
    
    private WebResponse makeRequest(String url) throws IOException {
        return webclient.loadWebResponse(new WebRequest(new URL(url)));
    }
    
    private SimpleResponseBuilder createProjectAndBuilder() throws Exception {
        ProjectNode project = createProject();
        
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
    
    private FileResponseBuilder createProjectAndFileBuilder() throws Exception {
        ProjectNode project = createProject();
        
        FileResponseBuilder builder = new FileResponseBuilder();
        builder.setName("file");
        project.getWebInterface().addAndSaveChildren(builder);
        builder.setResponseContentType("text/plain");
        builder.setLogLevel(LogLevel.TRACE);
        builder.setMinimalAccessRight(AccessRight.NONE);
        builder.setLoginService(loginService);
        return builder;
    }
    
//    private FileResponseBuilder createProjectAndFileBuilder(String content, String contentType) throws Exception {
//        ProjectNode project = createProject();
//        
//        FileResponseBuilder builder = new FileResponseBuilder();
//        builder.setName("file");
//        project.getWebInterface().addAndSaveChildren(builder);
//        builder.setResponseContentType(contentType);
//        builder.setLogLevel(LogLevel.TRACE);
//        builder.setMinimalAccessRight(AccessRight.NONE);
//        builder.setLoginService(loginService);
//        return builder;
//    }
    
    private ProjectNode createProject() throws Exception {
        ProjectNode project = (ProjectNode) projects.getNode("hello");
        if (project!=null)
            return project;
        project = new ProjectNode();
        project.setName("hello");
        projects.addAndSaveChildren(project);
        assertTrue(project.start());
        
        //adding anonymous authenticator
        loginService = new AnonymousLoginServiceNode();
        loginService.setName("anonym");
        project.getNode(LoginManagerNode.NAME).addAndSaveChildren(loginService);
        loginService.setExecutor(executor);
        assertTrue(loginService.start());
        return project;
    }
    
    private String formUrl(String path) {
        return (httpServer.getProtocol()==Protocol.HTTP? "http://" : "https://")+path;
    }
}

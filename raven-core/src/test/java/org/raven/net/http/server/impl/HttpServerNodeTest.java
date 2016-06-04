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

import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
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
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
import org.raven.auth.impl.AuthenticateByExpression;
import org.raven.auth.impl.AuthenticatorsNode;
import org.raven.auth.impl.BasicAuthenticator;
import org.raven.auth.impl.LoginServiceNode;

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
    private ProjectNode project;
    private AnonymousLoginServiceNode loginService;
    private TemporaryFileManagerNode tempFileManager;
    
    private Protocol proto;
    
    public HttpServerNodeTest() {
        proto = Protocol.HTTP;
    }
    
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

        tempFileManager = new TemporaryFileManagerNode();
        tempFileManager.setName("manager");
        tree.getRootNode().addAndSaveChildren(tempFileManager);
        tempFileManager.setDirectory("target/");
        tempFileManager.setScheduler(scheduler);
        assertTrue(tempFileManager.start());
        
        httpServer = new HttpServerNode();
        httpServer.setName("HTTP server");
        testsNode.addAndSaveChildren(httpServer);
        httpServer.setExecutor(executor);
        httpServer.setPort(7777);
        httpServer.setTempFileManager(tempFileManager);
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
    public void helloWorldDefaultCharsetTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent("'Hello World!'");
        builder.setResponseContentCharset(null);
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
        TextPage p = webclient.getPage(formUrl("localhost:7777/projects/hello/world"));
        assertNotNull(p);
        assertEquals(200, p.getWebResponse().getStatusCode());
        assertEquals("text/plain", p.getWebResponse().getContentType());
//        assertEquals("12", p.getWebResponse().getResponseHeaderValue(HttpHeaders.Names.CONTENT_LENGTH));
        assertEquals("Hello World!", p.getContent());        
    }
    
    @Test(timeout = 5000l)
    public void httpsChunkedEncodingTest() throws Exception {
        prepareHttps();
        chunkedEncodingTest();
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
        TextPage p = webclient.getPage(formUrl("localhost:7777/projects/hello/world"));
        assertNotNull(p);
        assertEquals(200, p.getWebResponse().getStatusCode());
        assertEquals("text/plain", p.getWebResponse().getContentType());
        assertEquals("123456789", p.getContent());        
    }
    
    @Test(timeout = 5000l)
    public void httpsOutputBufferOverflowTest() throws Exception {
        prepareHttps();
        outputBufferOverflowTest();
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
        TextPage p = webclient.getPage(formUrl("localhost:7777/projects/hello/world"));
        assertNotNull(p);
        assertEquals(200, p.getWebResponse().getStatusCode());
        assertEquals("text/plain", p.getWebResponse().getContentType());
        assertEquals("test", p.getContent());        
        
        p = webclient.getPage(formUrl("localhost:7777/projects/hello/world"));
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
    
    @Test(timeout = 10000l)
    public void httpsKeepAliveTest() throws Exception {
        prepareHttps();
        keepAliveTest();
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
        HtmlPage p = webclient.getPage(formUrl("localhost:7777/projects/hello/world"));
        assertNotNull(p);
        assertEquals(500, p.getWebResponse().getStatusCode());
        assertEquals("text/html", p.getWebResponse().getContentType());
        assertTrue(p.asText().contains("Response generation timeout"));
        Thread.sleep(2000L);
    }
    
    @Test(timeout = 10_000L)
    public void httpsBuildResponseTimeoutTest() throws Exception {
        prepareHttps();
        buildResponseTimeoutTest();
    }
        
    @Test(timeout = 5000l)
    public void redirectTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent("redirect('"+formUrl("localhost:7777/world/hello")+"')");
        assertTrue(builder.start());
        
        //do test
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        webclient.setRedirectEnabled(false);
        TextPage p = webclient.getPage(formUrl("localhost:7777/projects/hello/world"));
        assertNotNull(p);
        assertEquals(307, p.getWebResponse().getStatusCode());
        assertEquals(formUrl("localhost:7777/world/hello"), p.getWebResponse().getResponseHeaderValue(HttpHeaders.Names.LOCATION));
    }
    
    @Test(timeout = 5_000l)
    public void httpsRedirectTest() throws Exception {
        prepareHttps();
        redirectTest();
    }
    
    @Test(timeout = 5_000L)
    public void redirectToHttpsTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent("'ok'");
        builder.setRequireSSL(Boolean.TRUE);
        assertTrue(builder.start());
        
        HttpServerNode httpsServer = createHttpsServer();        
        assertTrue(httpsServer.start());
        httpServer.setHttpsServerNode(httpsServer);
        assertTrue(httpsServer.start());
        assertTrue(httpServer.start());
        
        webclient.setRedirectEnabled(true);
        TextPage p = webclient.getPage(formUrl("localhost:7777/projects/hello/world"));
        assertNotNull(p);
        assertEquals(200, p.getWebResponse().getStatusCode());
        assertEquals("ok", p.getContent());
    }
    
    @Test(timeout = 5_000L)
    public void redirectToHttpsWithoutHttpsServer() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent("'ok'");
        builder.setRequireSSL(Boolean.TRUE);
        assertTrue(builder.start());
        
        assertTrue(httpServer.start());
        
        webclient.setRedirectEnabled(true);
        HtmlPage p = webclient.getPage(formUrl("localhost:7777/projects/hello/world"));
        assertNotNull(p);
        assertEquals(500, p.getWebResponse().getStatusCode());
        assertTrue(p.asText().contains("Can't automaticly redirect to https resource"));
    }
    
    @Test(timeout = 5_000l)
    public void notModifiedTest() throws Exception {
        FileResponseBuilder builder = createProjectAndFileBuilder();
        builder.setLastModified(System.currentTimeMillis()-10000);
        builder.getFile().setDataString("Hello world!");
        assert(builder.start());

        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        webclient.addRequestHeader(HttpHeaders.Names.IF_MODIFIED_SINCE, HttpHeaderDateFormat.get().format(new Date()));        
        webclient.setRedirectEnabled(false);        
        WebResponse resp = makeRequest(formUrl("localhost:7777/projects/hello/file"));
        assertEquals(HttpResponseStatus.NOT_MODIFIED.code(), resp.getStatusCode());
        
        webclient.addRequestHeader(HttpHeaders.Names.IF_MODIFIED_SINCE, HttpHeaderDateFormat.get().format(
                new Date(System.currentTimeMillis()-20_000)));        
        resp = makeRequest(formUrl("localhost:7777/projects/hello/file"));
        assertEquals(200, resp.getStatusCode());
        assertEquals("Hello world!", resp.getContentAsString());
    }
    
    @Test(timeout = 5_000L)
    public void httpsNotModifiedTest() throws Exception {
        prepareHttps();;
        notModifiedTest();
    }
    
    @Test(timeout = 5_000L)
    public void noContentTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();
        builder.setResponseContentType("text/plain");
        builder.setResponseContent("null");
        assertTrue(builder.start());
        
        //do test
        assertTrue(httpServer.start());
        webclient.addRequestHeader("Connection", "close");        
        webclient.setRedirectEnabled(false);
        WebResponse resp =makeRequest(formUrl("localhost:7777/projects/hello/world"));
        assertNotNull(resp);
        assertEquals(HttpResponseStatus.NO_CONTENT.code(), resp.getStatusCode());
        assertEquals("", resp.getContentAsString());
    }
    
    @Test(timeout = 5_000L)
    public void httpsNoContentTest() throws Exception {
        prepareHttps();
        noContentTest();
    }
    
    @Test(timeout = 5_000L)
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
        WebRequest request = new WebRequest(new URL(formUrl("localhost:7777/projects/hello/world")));
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
    
    @Test(timeout = 5_000L)
    public void httpsQueryParametersTest() throws Exception {
        prepareHttps();
        queryParametersTest();
    }
    
    @Test(timeout = 5_000L)
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
        WebRequest request = new WebRequest(new URL(formUrl("localhost:7777/projects/hello/world")));
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
    
    @Test(timeout = 5_000L)
    public void httpsFormUrlEncodedTest() throws Exception {
        prepareHttps();
        formUrlEncodedTest();
    }
    
    @Test(timeout = 5_000L)
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
        WebRequest request = new WebRequest(new URL(formUrl("localhost:7777/projects/hello/world")));
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
    
    @Test(timeout = 5_000L)
    public void httpsMultipartFormDataTest() throws Exception {
        prepareHttps();
        multipartFormDataTest();
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
        HtmlPage page = webclient.getPage(formUrl("localhost:7777/projects/hello/file"));
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
    public void httpsMultipartFileDataTest() throws Exception {
        prepareHttps();
        multipartFileDataTest();
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
        WebRequest request = new WebRequest(new URL(formUrl("localhost:7777/projects/hello/world")), HttpMethod.POST);
        request.setRequestBody("Test");
        request.setAdditionalHeader("Content-Type", "text/plain");
        TextPage page = webclient.getPage(request);
        assertNotNull(page);
        assertEquals(200, page.getWebResponse().getStatusCode());
        assertEquals("ok", page.getContent());
    }
    
    @Test(timeout = 5_000L)
    public void httpsResponseContentReadTest() throws Exception {
        prepareHttps();
        responseContentReadTest();
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
        HttpPost post = new HttpPost(formUrl("localhost:7777/projects/hello/world"));
        post.setEntity(new ByteArrayEntity(buf, ContentType.APPLICATION_OCTET_STREAM));
        HttpClient client = createHttpClient();
        HttpResponse resp = client.execute(post);
        assertEquals(""+size, EntityUtils.toString(resp.getEntity()));                
    }
    
    @Test(timeout = 30_000L)
    public void httpsResponseContentReadWithBackPressure() throws Exception {
        prepareHttps();
        responseContentReadWithBackPressure();
    }
    
    @Test
    public void successAuthTest() throws Exception {
        SimpleResponseBuilder builder = createProjectAndBuilder();        
        LoginServiceNode loginService = addLoginService(project);
        builder.setLoginService(loginService);
        builder.setResponseContent("'ok'");
        assertTrue(builder.start());
        
        assertTrue(httpServer.start());
        
        webclient.setRedirectEnabled(true);
        HtmlPage page = webclient.getPage(formUrl("localhost:7777/projects/hello/world"));
        assertEquals(401, page.getWebResponse().getStatusCode());
        assertTrue(page.asText().contains("401 (Unauthorized)"));
        assertNotNull(page.getWebResponse().getResponseHeaderValue(HttpHeaders.Names.WWW_AUTHENTICATE));
        //
        webclient.setThrowExceptionOnFailingStatusCode(false);
        DefaultCredentialsProvider userProv = new DefaultCredentialsProvider();
        userProv.addCredentials("user1", "321");
        webclient.setCredentialsProvider(userProv);
        page = webclient.getPage(formUrl("localhost:7777/projects/hello/world"));
        assertEquals(401, page.getWebResponse().getStatusCode());
        assertTrue(page.asText().contains("401 (Unauthorized)"));
        assertNotNull(page.getWebResponse().getResponseHeaderValue(HttpHeaders.Names.WWW_AUTHENTICATE));
        //
        userProv.addCredentials("user1", "123");
        TextPage okPage = webclient.getPage(formUrl("localhost:7777/projects/hello/world"));
        assertEquals(200, okPage.getWebResponse().getStatusCode());
        assertEquals("ok", okPage.getContent());
    }
    
    private HttpClient createHttpClient() throws Exception {
        if (httpServer.getProtocol()==Protocol.HTTPS) {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                @Override public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                @Override public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            }};
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustAllCerts, null);

            SSLSocketFactory sf = new SSLSocketFactory(context, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
            schemeRegistry.register(new Scheme("https", 443, sf));

            BasicClientConnectionManager cm = new BasicClientConnectionManager(schemeRegistry);

            return new DefaultHttpClient(cm);            
        } else
            return new DefaultHttpClient();
    }
    
    private WebResponse makeRequest(String url) throws IOException {
        return webclient.loadWebResponse(new WebRequest(new URL(url)));
    }
    
    private SimpleResponseBuilder createProjectAndBuilder() throws Exception {
        project = createProject();
        
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
    
    private LoginServiceNode addLoginService(ProjectNode project) {
        LoginServiceNode loginService = new LoginServiceNode("main");
        project.getNode(LoginManagerNode.NAME).addAndSaveChildren(loginService);
        loginService.setExecutor(executor);
        assertTrue(loginService.start());
        //
        BasicAuthenticator authNode = new BasicAuthenticator();
        authNode.setName("user1");
        loginService.getNode(AuthenticatorsNode.NAME).addAndSaveChildren(authNode);
        authNode.setPassword("123");
        assertTrue(authNode.start());
        return loginService;
    }
    
    private HttpServerNode createHttpsServer() {
        HttpServerNode httpsServer = new HttpServerNode();
        httpsServer.setName("HTTPS server");
        testsNode.addAndSaveChildren(httpsServer);
        httpsServer.setExecutor(executor);
        httpsServer.setPort(7443);
        httpsServer.setTempFileManager(tempFileManager);
        httpsServer.setUploadedFilesTempDir("target/upload_tmp");
        httpsServer.setLogLevel(LogLevel.TRACE);
        httpsServer.setProtocol(Protocol.HTTPS);
        httpsServer.setKeystorePassword("test123");
        httpsServer.setKeystorePath("src/test/resources/self_signed_keystore.jks");
        return httpsServer;
    }
}

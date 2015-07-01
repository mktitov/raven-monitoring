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

import groovy.lang.Writable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.BindingNames;
import org.raven.net.NetworkResponseService;
import org.raven.test.BindingsContainer;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodeError;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.raven.ds.impl.AsyncDataPipe;
import org.raven.net.Request;
import org.raven.net.Response;
import org.raven.net.ResponseContext;
import org.raven.net.ResponsePromise;
import org.raven.net.ResponseReadyCallback;
import org.raven.net.Result;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class SimpleResponseBuilderTest extends RavenCoreTestCase {
    private SimpleResponseBuilder builder;
    private Node sriRootNode;    
    private IMocksControl mocks;
    private PushDataSource ds;
    private DataCollector collector;
    
    @Before
    public void prepare() {
        NetworkResponseService respService = registry.getService(NetworkResponseService.class);
        sriRootNode = respService.getNetworkResponseServiceNode();
        mocks = createControl();
    }
    
    @Test
    public void stringPathTest() {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", "path('sri/test')");
        ResponseContext responcetContext = trainResponseContext();
        mocks.replay();
        assertEquals("/raven/sri/test", respBuilder.buildResponseContent(null, responcetContext));
        mocks.verify();
    }
    
    @Test
    public void nodePathTest() {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", "path(node.parent)");
        ResponseContext responcetContext = trainResponseContext();
        mocks.replay();
        assertEquals("/raven/sri/group", respBuilder.buildResponseContent(null, responcetContext));
        mocks.verify();
    }
    
    @Test
    public void renderStaticTest() throws Exception {
        FileResponseBuilder fileBuilder = createFileBuilder(sriRootNode, "file", "text/html");
        fileBuilder.getFile().setDataString("static");
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", "render(node.parent.parent.getNode('file'))");
        ResponseContext responseContext = trainResponseContext();
        mocks.replay();
        Object resp = respBuilder.buildResponseContent(null, responseContext);
        assertNotNull(resp);
        assertTrue(resp instanceof DataFile);        
        assertEquals("static", IOUtils.toString(((DataFile)resp).getDataReader()));
        mocks.verify();
    }
    
    @Test
    public void renderWithStatusCodeTest() throws Exception {
        FileResponseBuilder fileBuilder = createFileBuilder(sriRootNode, "file", "text/html");
        fileBuilder.getFile().setDataString("static");
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", "render(201,node.parent.parent.getNode('file'))");
        ResponseContext responseContext = trainResponseContext();
        mocks.replay();
        Object resp = respBuilder.buildResponseContent(null, responseContext);
        assertNotNull(resp);
        assertTrue(resp instanceof Result);
        Result result = (Result) resp;
        assertEquals(201, result.getStatusCode());
        assertTrue(result.getContent() instanceof DataFile);
        assertEquals("static", IOUtils.toString(((DataFile)result.getContent()).getDataReader()));
        mocks.verify();
    }
    
    @Test
    public void renderWithParamsTest() throws Exception {
        FileResponseBuilder fileBuilder = createFileBuilder(sriRootNode, "file", FileResponseBuilder.GSP_MIME_TYPE);
        fileBuilder.getFile().setDataString("$p-$rootPath-${node.name}");
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(
                group, "builder", "render(node.parent.parent.getNode('file'), [p:'param'])");
        ResponseContext responseContext = trainResponseContext();
        mocks.replay();
        Object resp = respBuilder.buildResponseContent(null, responseContext);
        assertNotNull(resp);
        assertTrue(resp instanceof Writable);        
        assertEquals("param-/raven-file", resp.toString());
        mocks.verify();
    }
    
    @Test
    public void stringRedirectTest() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", "redirect('http://test')");
        ResponseContext responcetContext = trainResponseContext();
        mocks.replay();
        Object res = respBuilder.buildResponseContent(null, responcetContext);
        assertNotNull(res);
        assertTrue(res instanceof RedirectResult);
        assertEquals("http://test", ((RedirectResult)res).getUrl());
        mocks.verify();        
    }
    
    @Test
    public void nodeRedirectTest() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", "redirect(node)");
        ResponseContext responcetContext = trainResponseContext();
        mocks.replay();
        Object res = respBuilder.buildResponseContent(null, responcetContext);
        assertNotNull(res);
        assertTrue(res instanceof RedirectResult);
        assertEquals("/raven/sri/group/builder", ((RedirectResult)res).getUrl());
        mocks.verify();        
    }
    
    @Test
    public void resultTest() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", "result('test')");
        ResponseContext responcetContext = trainResponseContext();
        mocks.replay();
        Object res = respBuilder.buildResponseContent(null, responcetContext);
        assertNotNull(res);
        assertTrue(res instanceof Result);
        Result result = (Result) res;
        assertEquals(HttpServletResponse.SC_OK, result.getStatusCode());
        assertEquals("test", result.getContent());
        mocks.verify();        
    }
    
    @Test
    public void resultWithStatusTest() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", "result(400, 'test')");
        ResponseContext responcetContext = trainResponseContext();
        mocks.replay();
        Object res = respBuilder.buildResponseContent(null, responcetContext);
        assertNotNull(res);
        assertTrue(res instanceof Result);
        Result result = (Result) res;
        assertEquals(400, result.getStatusCode());
        assertEquals("test", result.getContent());
        mocks.verify();        
    }
    
    @Test
    public void throwHttpErrorTest() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", "{->throwHttpError('error')}()");
        ResponseContext responceContext = trainResponseContext();
        mocks.replay();
        Object res = respBuilder.buildResponseContent(null, responceContext);
        assertNotNull(res);
        assertTrue(res instanceof Result);
        Result result = (Result) res;
        assertEquals(400, result.getStatusCode());
        assertEquals("text/plain", result.getContentType());
        assertEquals("error", result.getContent());
        mocks.verify();        
    }
    
    @Test
    public void throwHttpErrorTest2() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", "{->throwHttpError(500,'error')}()");
        ResponseContext responceContext = trainResponseContext();
        mocks.replay();
        Object res = respBuilder.buildResponseContent(null, responceContext);
        assertNotNull(res);
        assertTrue(res instanceof Result);
        Result result = (Result) res;
        assertEquals(500, result.getStatusCode());
        assertEquals("text/plain", result.getContentType());
        assertEquals("error", result.getContent());
        mocks.verify();        
    }
    
    @Test
    public void throwHttpErrorTest3() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", "{->throwHttpError(500,'text/html','error')}()");
        ResponseContext responceContext = trainResponseContext();
        mocks.replay();
        Object res = respBuilder.buildResponseContent(null, responceContext);
        assertNotNull(res);
        assertTrue(res instanceof Result);
        Result result = (Result) res;
        assertEquals(500, result.getStatusCode());
        assertEquals("text/html", result.getContentType());
        assertEquals("error", result.getContent());
        mocks.verify();        
    }
    
    @Test(timeout = 2000)
    public void sendDataAsyncTest() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", 
                "res = 'o'; sendDataAsync(node.$ds, node.$consumer, 'test'){\n"
               + "  res+'k'\n"
               + "}"
        );
        prepareSendAsyncTests(respBuilder);
        ResponseContext responseContext = trainResponseContext();
        Map headers = mocks.createMock("headers", Map.class);
        expect(responseContext.getHeaders()).andReturn(headers);        
        mocks.replay();
        
        Object res = respBuilder.buildResponseContent(null, responseContext);
        checkPromise(res, "ok", false);
        mocks.verify();                
    }
    
    @Test(timeout = 2000)
    public void sendDataAsyncTest2() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", 
                "sendDataAsync(node.$ds, node.$consumer, 'test'){initiator->\n"
               + "  initiator.path\n"
               + "}"
        );
        prepareSendAsyncTests(respBuilder);
        ResponseContext responseContext = trainResponseContext();
        Map headers = mocks.createMock("headers", Map.class);
        expect(responseContext.getHeaders()).andReturn(headers);        
        mocks.replay();
        
        Object res = respBuilder.buildResponseContent(null, responseContext);
        checkPromise(res, collector.getPath(), false);
        mocks.verify();                
    }
    
    @Test(timeout = 2000)
    public void sendDataAsyncTest3() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", 
                "sendDataAsync(node.$ds, node.$consumer, 'test'){initiator, data->\n"
               + "  initiator.path+data\n"
               + "}"
        );
        prepareSendAsyncTests(respBuilder);
        ResponseContext responseContext = trainResponseContext();
        Map headers = mocks.createMock("headers", Map.class);
        expect(responseContext.getHeaders()).andReturn(headers);        
        mocks.replay();
        
        Object res = respBuilder.buildResponseContent(null, responseContext);
        checkPromise(res, collector.getPath()+"test", false);
        mocks.verify();                
    }
    
    @Test(timeout = 2000)
    public void sendDataAsyncTest4() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", 
                "ctx = createDataContext()\n"
               + "sendDataAsync(node.$ds, node.$consumer, ctx, 'test'){\n"
               + "  'ok'\n"
               + "}"
        );
        prepareSendAsyncTests(respBuilder);
        ResponseContext responseContext = trainResponseContext();
        Map headers = mocks.createMock("headers", Map.class);
        expect(responseContext.getHeaders()).andReturn(headers);        
        mocks.replay();
        
        Object res = respBuilder.buildResponseContent(null, responseContext);
        checkPromise(res, "ok", false);
        mocks.verify();                
    }
    
    @Test(timeout = 2000)
    public void sendDataAsyncTest5() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", 
                "ctx = createDataContext()\n"
               +"sendDataAsync(node.$ds, node.$consumer, ctx, 'test'){initiator->\n"
               + "  initiator.path\n"
               + "}"
        );
        prepareSendAsyncTests(respBuilder);
        ResponseContext responseContext = trainResponseContext();
        Map headers = mocks.createMock("headers", Map.class);
        expect(responseContext.getHeaders()).andReturn(headers);        
        mocks.replay();
        
        Object res = respBuilder.buildResponseContent(null, responseContext);
        checkPromise(res, collector.getPath(), false);
        mocks.verify();                
    }    
    
    @Test(timeout = 2000)
    public void sendDataAsyncTest6() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", 
                "ctx = createDataContext()\n"
               +"sendDataAsync(node.$ds, node.$consumer, ctx, 'test'){initiator, data->\n"
               + "  initiator.path+data\n"
               + "}"
        );
        prepareSendAsyncTests(respBuilder);
        ResponseContext responseContext = trainResponseContext();
        Map headers = mocks.createMock("headers", Map.class);
        expect(responseContext.getHeaders()).andReturn(headers);        
        mocks.replay();
        
        Object res = respBuilder.buildResponseContent(null, responseContext);
        checkPromise(res, collector.getPath()+"test", false);
        mocks.verify();                
    }
    
    @Test(timeout = 2000)
    public void sendDataAsyncErrorTest() throws Exception {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", 
                "res = 'o'; sendDataAsync(node.$ds, node.$consumer, 'test'){\n"
               + "  throw new Exception('error')"
               + "}"
        );
        prepareSendAsyncTests(respBuilder);
        ResponseContext responseContext = trainResponseContext();
        mocks.replay();
        
        Object res = respBuilder.buildResponseContent(null, responseContext);
        checkPromise(res, "error", true);
        mocks.verify();                
    }
    
    private void checkPromise(Object res, Object expectContent, boolean waitForError) throws InterruptedException {
        assertNotNull(res);
        assertTrue(res instanceof ResponsePromise);
        final AtomicReference responseRes = new AtomicReference();
        final AtomicReference<Throwable> responseErr = new AtomicReference();
        ((ResponsePromise)res).onComplete(new ResponseReadyCallback() {
            public void onSuccess(Response response) {
                responseRes.set(response);
            }
            public void onError(Throwable e) {
                responseErr.set(e);
            }
        });
        while (responseRes.get()==null && responseErr.get()==null) 
            Thread.currentThread().sleep(5);
        if (!waitForError) {
            assertTrue(responseRes.get() instanceof Response);
            Response response = (Response) responseRes.get();
            assertEquals(expectContent, response.getContent());
        } else {
            assertNotNull(responseErr.get());
            assertEquals(expectContent, responseErr.get().getMessage());
        }        
    }
    
    private void prepareSendAsyncTests(SimpleResponseBuilder builder ) throws Exception {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setCorePoolSize(4);
        assertTrue(executor.start());
        
        ds = new PushDataSource();
        ds.setName("ds");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        AsyncDataPipe pipe = new AsyncDataPipe();        
        pipe.setName("pipe");
        testsNode.addAndSaveChildren(pipe);
        pipe.setDataSource(ds);
        pipe.setHandleDataInSeparateThread(Boolean.TRUE);
        pipe.setExecutor(executor);
        assertTrue(pipe.start());
        
        collector = new DataCollector();
        collector.setName("collector");
        testsNode.addAndSaveChildren(collector);
        collector.setDataSource(pipe);
        assertTrue(collector.start());
        
        addNodeRefAttr(builder, "ds", ds);
        addNodeRefAttr(builder, "consumer", collector);
    }
    
    private void addNodeRefAttr(Node owner, String name, Node ref) throws Exception {
        NodeAttributeImpl attr = new NodeAttributeImpl(name, Node.class, ref.getPath(), null);
        attr.setOwner(owner);
        attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        attr.init();
        owner.addAttr(attr);
    }
    
    private BindingsContainer createGroup() throws NodeError {
        BindingsContainer group = new BindingsContainer();
        group.setName("group");
        sriRootNode.addAndSaveChildren(group);
        assertTrue(group.start());
        group.addBinding(BindingNames.ROOT_PATH, "/raven");
        return group;
    }
    
    private SimpleResponseBuilder createBuilder(Node owner, String name, String script) {
        SimpleResponseBuilder responseBuilder = new SimpleResponseBuilder();
        responseBuilder.setName("builder");
        owner.addAndSaveChildren(responseBuilder);
        responseBuilder.setResponseContentType("text/html");
        responseBuilder.setResponseContent(script);
        assertTrue(responseBuilder.start());        
        return responseBuilder;
    }
    
    private FileResponseBuilder createFileBuilder(Node owner, String nodeName, String contentType) throws DataFileException {
        FileResponseBuilder fileBuilder = new FileResponseBuilder();
        fileBuilder.setName(nodeName);
        owner.addAndSaveChildren(fileBuilder);
        fileBuilder.getFile().setMimeType(contentType);
        fileBuilder.setResponseContentType("text/html");
        return fileBuilder;
    }
    
    private ResponseContext trainResponseContext() {
        ResponseContext respContext = mocks.createMock(ResponseContext.class);
        Request request = mocks.createMock(Request.class);
        expect(respContext.getRequest()).andReturn(request).atLeastOnce();
        expect(request.getRootPath()).andReturn("/raven").atLeastOnce();
        return respContext;
    }   
}

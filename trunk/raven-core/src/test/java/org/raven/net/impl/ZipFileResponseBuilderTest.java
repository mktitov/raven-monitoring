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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.net.NetworkResponseService;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import static org.junit.Assert.assertTrue;
import org.raven.TestScheduler;
import org.raven.cache.TemporaryFileManagerNode;
import org.raven.log.LogLevel;
import org.raven.net.ContextUnavailableException;
import org.raven.net.Request;
import org.raven.net.Response;
import org.raven.net.ResponseContext;

/**
 *
 * @author Mikhail Titov
 */
public class ZipFileResponseBuilderTest extends RavenCoreTestCase {
    private ZipFileResponseBuilder builder;
    private Node sriRootNode;
    private IMocksControl mocks;
    private Request request;
    private Map params;
    
    @Before
    public void prepare() throws Exception {
        NetworkResponseService respService = registry.getService(NetworkResponseService.class);
        sriRootNode = respService.getNetworkResponseServiceNode();
        builder = new ZipFileResponseBuilder();
        builder.setName("builder");
        sriRootNode.addAndSaveChildren(builder);
        File zipFile = new File("src/test/conf/test.zip");
        assertTrue(zipFile.exists());
        builder.getFile().setDataStream(new FileInputStream(zipFile));
        builder.setLogLevel(LogLevel.TRACE);
        assertTrue(builder.start());
        mocks = createControl();
    }
    
    @Test(expected = ContextUnavailableException.class)
    public void nullSubcontextTest() throws Exception {
        ResponseContext respconseContext = trainResponseContext();
        expect(params.get(NetworkResponseServiceNode.SUBCONTEXT_PARAM)).andReturn(null);
        mocks.replay();
        builder.buildResponseContent(null, respconseContext);
        mocks.verify();
    }
    
    @Test(expected = ContextUnavailableException.class)
    public void entryNotFoundTest() throws Exception {
        ResponseContext respconseContext = trainResponseContext();
        expect(params.get(NetworkResponseServiceNode.SUBCONTEXT_PARAM)).andReturn("unknown");
        mocks.replay();
        builder.buildResponseContent(null, respconseContext);
        mocks.verify();
    }
    
    @Test
    public void successTest() throws Exception {
        ResponseContext respconseContext = trainResponseContext();
        expect(params.get(NetworkResponseServiceNode.SUBCONTEXT_PARAM)).andReturn("folder/2.txt");
        Map headers = mocks.createMock(Map.class);
        expect(respconseContext.getHeaders()).andReturn(headers);
        mocks.replay();
        Object res = builder.buildResponseContent(null, respconseContext);
        assertNotNull(res);
        assertTrue(res instanceof Response);
        Response response = (Response) res;
        assertEquals("text/plain", response.getContentType());
        Object content = response.getContent();
        assertNotNull(content);
        assertTrue(content instanceof InputStream);
        assertEquals("file number 2", IOUtils.toString((InputStream)content));
        mocks.verify();        
    }
    
    private void execSuccessTest(IMocksControl mocks) throws Exception {
        ResponseContext respconseContext = trainResponseContext(mocks);
        expect(params.get(NetworkResponseServiceNode.SUBCONTEXT_PARAM)).andReturn("folder/2.txt");
        Map headers = mocks.createMock(Map.class);
        expect(respconseContext.getHeaders()).andReturn(headers);
        mocks.replay();
        Object res = builder.buildResponseContent(null, respconseContext);
        assertNotNull(res);
        assertTrue(res instanceof Response);
        Response response = (Response) res;
        assertEquals("text/plain", response.getContentType());
        Object content = response.getContent();
        assertNotNull(content);
        assertTrue(content instanceof File);
        assertEquals("file number 2", IOUtils.toString(((File)content).toURI().toURL()));
        mocks.verify();        
    }
    
    @Test
    public void successWithTempFileManagerTest() throws Exception {
        TestScheduler scheduler = new TestScheduler();
        scheduler.setName("scheduler");
        testsNode.addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());
        
        TemporaryFileManagerNode tempManager = new TemporaryFileManagerNode();
        tempManager.setName("temp file manager");
        testsNode.addAndSaveChildren(tempManager);
        tempManager.setDirectory("target/");
        tempManager.setScheduler(scheduler);
        assertTrue(tempManager.start());
        builder.setTemporaryFileManager(tempManager);
        execSuccessTest(mocks);
//        final AtomicBoolean hasError = new AtomicBoolean();
//        for (int i=0; i<100; i++)
//            new Thread() {
//                @Override public void run() {
//                    try {
//                        execSuccessTest(createControl());
//                    } catch (Exception ex) {
//                        hasError.set(true);
//                        Logger.getLogger(ZipFileResponseBuilderTest.class.getName()).log(Level.SEVERE, null, ex);
//                        builder.getLogger().error("\n!!!ERROR", ex);
//                    }
//                }
//            }.start();
//        Thread.sleep(5000);
//        assertFalse(hasError.get());
    }
    
    @Test
    public void baseDirTest() throws Exception {
        testBaseDir("folder");
    }
    
    @Test
    public void baseDirTest2() throws Exception {
        testBaseDir("folder/");
    }
    
    private void testBaseDir(String folder) throws Exception {
        ResponseContext respconseContext = trainResponseContext();
        expect(params.get(NetworkResponseServiceNode.SUBCONTEXT_PARAM)).andReturn("2.txt");
        Map headers = mocks.createMock(Map.class);
        expect(respconseContext.getHeaders()).andReturn(headers);
        mocks.replay();
        builder.setBaseDir(folder);
        Object res = builder.buildResponseContent(null, respconseContext);
        assertNotNull(res);
        assertTrue(res instanceof Response);
        Response response = (Response) res;
        assertEquals("text/plain", response.getContentType());
        Object content = response.getContent();
        assertNotNull(content);
        assertTrue(content instanceof InputStream);
        assertEquals("file number 2", IOUtils.toString((InputStream)content));
        mocks.verify();
        
    }
    private ResponseContext trainResponseContext() {
        return trainResponseContext(mocks);
    }
    
    private ResponseContext trainResponseContext(IMocksControl mocks) {
        ResponseContext ctx = mocks.createMock(ResponseContext.class);
        request = mocks.createMock(Request.class);
        params = mocks.createMock(Map.class);
        
        expect(ctx.getRequest()).andReturn(request).atLeastOnce();
        expect(request.getServicePath()).andReturn("sri").atLeastOnce();
        expect(request.getContextPath()).andReturn("builder").atLeastOnce();
        expect(request.getParams()).andReturn(params);
        return ctx;
    }
}

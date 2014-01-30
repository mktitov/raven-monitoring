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
import org.apache.commons.io.IOUtils;
import static org.junit.Assert.assertTrue;
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
import org.raven.net.RedirectResult;
import org.raven.net.Request;
import org.raven.net.ResponseContext;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;

/**
 *
 * @author Mikhail Titov
 */
public class SimpleResponseBuilderTest extends RavenCoreTestCase {
    private SimpleResponseBuilder builder;
    private Node sriRootNode;    
    private IMocksControl mocks;
    
    @Before
    public void prepare() {
        NetworkResponseService respService = registry.getService(NetworkResponseService.class);
        sriRootNode = respService.getNetworkResponseServiceNode();
        mocks = createControl();
    }
    
    @Test
    public void stringPathTest() {
        BindingsContainer group = createGroup();
        SimpleResponseBuilder respBuilder = createBuilder(group, "builder", "path('test')");
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
        assertEquals("/raven/sri/group/", respBuilder.buildResponseContent(null, responcetContext));
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
        assertEquals("/raven/sri/group/builder/", ((RedirectResult)res).getUrl());
        mocks.verify();        
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

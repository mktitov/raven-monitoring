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
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.BindingNames;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.net.NetworkResponseService;
import org.raven.net.Outputable;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.BindingsContainer;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
import org.raven.tree.Node;
import org.raven.tree.NodeError;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class FileResponseBuilderTest extends RavenCoreTestCase {
    
    private final static String NODE_NAME = "file response";
    private FileResponseBuilder builder;
    private Node sriRootNode;
    private ExecutorServiceNode executor;
    
    @Before
    public void prepare() throws Exception {
        NetworkResponseService respService = registry.getService(NetworkResponseService.class);
        sriRootNode = respService.getNetworkResponseServiceNode();
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setCorePoolSize(50);
        executor.setMaximumPoolSize(50);
        assertTrue(executor.start());
        builder = createBuilder(NODE_NAME, "text/html");
        builder.setExecutor(executor);
    }
    
    @Test
    public void simpleFileTest() throws Exception {
        byte[] data = "test".getBytes();
        assertNull(builder.doGetLastModified());
        long curTime = System.currentTimeMillis();
        builder.getFile().setDataStream(new ByteArrayInputStream(data));
        Long lastModified = builder.doGetLastModified();
        assertNotNull(lastModified);
        assertTrue(lastModified >= curTime);
        assertTrue(builder.start());
        Object resp = builder.buildResponseContent(null, null);
        assertNotNull(resp);
        assertTrue(resp instanceof DataFile);
        byte[] respData = IOUtils.toByteArray(((DataFile)resp).getDataStream());
        assertArrayEquals(data, respData);
    }
    
    @Test
    public void templateTest() throws Exception {
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        assertNull(builder.doGetLastModified());
        builder.getFile().setDataString("${node.name}");
        assertNull(builder.doGetLastModified());
        assertTrue(builder.start());
        Object resp = builder.buildResponseContent(null, null);
        assertNotNull(resp);
        assertTrue(resp instanceof Writable);
        assertEquals(NODE_NAME, resp.toString());
        Template template = builder.getResponseTemplate();
        assertNotNull(template);
        
        assertEquals(NODE_NAME, builder.buildResponseContent(null, null).toString());
        assertSame(template, builder.getResponseTemplate());
        
        builder.getFile().setFilename("changed file name");
        assertEquals(NODE_NAME, builder.buildResponseContent(null, null).toString());
        assertSame(template, builder.getResponseTemplate());
        
        Long lastModified = builder.getLastModified();
        builder.getFile().setMimeType("text/html");
        assertEquals(lastModified, builder.getLastModified());
        assertEquals(lastModified, builder.doGetLastModified());
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        assertEquals(lastModified, builder.getLastModified());
        assertEquals(NODE_NAME, builder.buildResponseContent(null, null).toString());
        assertNotSame(template, builder.getResponseTemplate());
        template = builder.getResponseTemplate();
        
        builder.getFile().setDataString("${node.name}");
        assertEquals(NODE_NAME, builder.buildResponseContent(null, null).toString());
        assertNotSame(template, builder.getResponseTemplate());
    }
    
    @Test
    public void executeScriptFromTemplateTest() throws Exception {
        NodeAttributeImpl attr = new NodeAttributeImpl("script", String.class, "param1", null);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setOwner(testsNode);
        attr.init();
        testsNode.addAttr(attr);
        
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        builder.getFile().setDataString("${node.tree.getNode('"+testsNode.getPath()+"').$script(param1:'test')}");
//        builder.setExtendsTemplate(rootBuilder);
        assertTrue(builder.start());
        
        assertEquals("test", builder.buildResponseContent(null, null).toString());
    }

    @Test
    public void extendsTemplateTest() throws Exception {
        FileResponseBuilder rootBuilder = createBuilder("root", FileResponseBuilder.GSP_MIME_TYPE);
        rootBuilder.getFile().setDataString("${node.name}-${body()}");
        assertTrue(rootBuilder.start());
        
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        builder.getFile().setDataString("${node.name}");
        builder.setExtendsTemplate(rootBuilder);
        assertTrue(builder.start());
        
        assertEquals("root-"+NODE_NAME, builder.buildResponseContent(null, null).toString());
    }

    @Test
    public void emptyTemplateExtendsAnotherTemplateTest() throws Exception {
        FileResponseBuilder rootBuilder = createBuilder("root", FileResponseBuilder.GSP_MIME_TYPE);
        rootBuilder.getFile().setDataString("${node.name}-${body()}");
        assertTrue(rootBuilder.start());
        
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        builder.getFile().setDataString("");
        builder.setExtendsTemplate(rootBuilder);
        assertTrue(builder.start());
        
        assertEquals("root-", builder.buildResponseContent(null, null).toString());
    }

    @Test
    public void extendsTemplateWithParamsTest() throws Exception {
        FileResponseBuilder rootBuilder = createBuilder("root", FileResponseBuilder.GSP_MIME_TYPE);
        rootBuilder.getFile().setDataString("$title-${body()}");
        assertTrue(rootBuilder.start());
        
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        builder.getFile().setDataString("${node.name}");
        builder.setExtendsTemplate(rootBuilder);
        builder.getAttr("extendsTemplateParams").setValue("[title:'template title']");
        assertTrue(builder.start());
        
        assertEquals("template title-"+NODE_NAME, builder.buildResponseContent(null, null).toString());
    }

    @Test
    public void extendsTemplateWithBodyParamsTest() throws Exception {
        FileResponseBuilder rootBuilder = createBuilder("root", FileResponseBuilder.GSP_MIME_TYPE);
        rootBuilder.getFile().setDataString("${node.name}-${body(p:'test')}");
        assertTrue(rootBuilder.start());
        
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        builder.getFile().setDataString("${p}");
        builder.setExtendsTemplate(rootBuilder);
        assertTrue(builder.start());
        
        assertEquals("root-test", builder.buildResponseContent(null, null).toString());
    }
    
    @Test
    public void includeFileTest() throws Exception {
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        builder.getFile().setDataString("(${include(node.parent.getNode('include-builder'))})");
        assertTrue(builder.start());
        
        FileResponseBuilder includeBuilder = createBuilder("include-builder", "text/html");
        includeBuilder.getFile().setDataString("test");
        assertTrue(includeBuilder.start());
        
        assertEquals("(test)", builder.buildResponseContent(null, null).toString());
    }
    
    @Test
    public void includeTemplateTest() throws Exception {
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        builder.getFile().setDataString("(${include(node.parent.getNode('include-builder'))})");
        assertTrue(builder.start());
        
        FileResponseBuilder includeBuilder = createBuilder("include-builder", FileResponseBuilder.GSP_MIME_TYPE);
        includeBuilder.getFile().setDataString("${node.name}");
        assertTrue(includeBuilder.start());
        
        assertEquals("(include-builder)", builder.buildResponseContent(null, null).toString());
    }
    
    @Test
    public void includeTemplateWithParamsTest() throws Exception {
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        builder.getFile().setDataString("(${include(node.parent.getNode('include-builder'), [p:'test'])})");
        assertTrue(builder.start());
        
        FileResponseBuilder includeBuilder = createBuilder("include-builder", FileResponseBuilder.GSP_MIME_TYPE);
        includeBuilder.getFile().setDataString("${p}");
        assertTrue(includeBuilder.start());
        
        assertEquals("(test)", builder.buildResponseContent(null, null).toString());
    }
    
    @Test
    public void stringPathTest() throws Exception {
        BindingsContainer group = createGroup();
        
        FileResponseBuilder resp = createBuilder(group, "test", FileResponseBuilder.GSP_MIME_TYPE);
        resp.getFile().setDataString("${path('sri/group/test')}");
        assertTrue(resp.start());
        
        assertEquals("/raven/sri/group/test", resp.buildResponseContent(null, null).toString());
    }
    
    @Test
    public void nodePathTest() throws Exception {
        BindingsContainer group = createGroup();
        
        FileResponseBuilder resp = createBuilder(group, "test", FileResponseBuilder.GSP_MIME_TYPE);
        resp.getFile().setDataString("${path(node.parent)}");
        assertTrue(resp.start());
        
        assertEquals("/raven/sri/group", resp.buildResponseContent(null, null).toString());
    }

    private BindingsContainer createGroup() throws NodeError {
        BindingsContainer group = new BindingsContainer();
        group.setName("group");
        sriRootNode.addAndSaveChildren(group);
        assertTrue(group.start());
        group.addBinding(BindingNames.ROOT_PATH, "/raven");
        return group;
    }
    
    @Test(timeout = 2000)
    public void transformerTest() throws Exception {
        int tasksCount = executor.getExecutingTaskCount();
        byte[] data = "test".getBytes();
        assertNull(builder.doGetLastModified());
        long curTime = System.currentTimeMillis();
        builder.getFile().setDataStream(new ByteArrayInputStream(data));
        
        TestContentTransformer transformer = new TestContentTransformer();
        transformer.setName("t1");
        builder.addAndSaveChildren(transformer);
        assertTrue(transformer.start());
        
        assertTrue(builder.start());
        Object resp = builder.buildResponseContent(null, null);
        assertNotNull(resp);
        assertTrue(resp instanceof Outputable);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ((Outputable)resp).outputTo(buf);
        assertArrayEquals("Super test".getBytes(), buf.toByteArray());
        assertEquals(1, tasksCount);
    }
    
//    @Test(timeout = 2000)
    @Test()
    public void templateWithTransformerTest() throws Exception {
        int tasksCount = executor.getExecutingTaskCount();
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        builder.getFile().setDataString("${node.name}");
        builder.getFile().setEncoding(Charset.forName("utf-8"));
//        buildet
        assertTrue(builder.start());
        
        TestContentTransformer transformer = new TestContentTransformer();
        transformer.setName("t1");
        builder.addAndSaveChildren(transformer);
        assertTrue(transformer.start());             
        
        Object resp = builder.buildResponseContent(null, null);
        assertNotNull(resp);
        assertTrue(resp instanceof Outputable);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ((Outputable)resp).outputTo(buf);
        assertArrayEquals(("Super "+NODE_NAME).getBytes(), buf.toByteArray());
        assertEquals(1, tasksCount);
    }

    @Test
    public void extendsTemplateWithTransformerTest() throws Exception {
        FileResponseBuilder rootBuilder = createBuilder("root", FileResponseBuilder.GSP_MIME_TYPE);
        rootBuilder.getFile().setDataString("${node.name}-${body()}!");
        
        assertTrue(rootBuilder.start());
        
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        builder.getFile().setEncoding(Charset.forName("utf-8"));
        builder.getFile().setDataString("${node.name}");
        builder.setExtendsTemplate(rootBuilder);
        assertTrue(builder.start());
        
        TestContentTransformer transformer = new TestContentTransformer();
        transformer.setName("t1");
        builder.addAndSaveChildren(transformer);
        assertTrue(transformer.start());             
        
        assertEquals("root-Super "+NODE_NAME+"!", builder.buildResponseContent(null, null).toString());
    }
    
    @Test
    public void exceptionTest() throws Exception {
//        Object res = new SimpleTemplateEngine().createTemplate("\n\n$a\n123").make();
        StringReader reader = new StringReader("Тест");
        Map bindings = new HashMap();
        bindings.put("reader", reader);
        Object res = new SimpleTemplateEngine().createTemplate("($reader)").make(bindings);
        System.out.println("RES: "+res.toString());
    }

    private FileResponseBuilder createBuilder(String nodeName, String contentType) throws DataFileException {
        return createBuilder(sriRootNode, nodeName, contentType);
    }
    
    private FileResponseBuilder createBuilder(Node owner, String nodeName, String contentType) throws DataFileException {
        FileResponseBuilder fileBuilder = new FileResponseBuilder();
        fileBuilder.setName(nodeName);
        owner.addAndSaveChildren(fileBuilder);
        fileBuilder.getFile().setMimeType(contentType);
        fileBuilder.setResponseContentType("text/html");
        return fileBuilder;
    }
}
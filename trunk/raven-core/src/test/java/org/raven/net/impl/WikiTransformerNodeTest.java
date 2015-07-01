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

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.raven.BindingNames;
import org.raven.net.NetworkResponseService;
import org.raven.net.Outputable;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.BindingsContainer;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodePathResolver;
import org.raven.tree.impl.ContainerNode;

/**
 *
 * @author Mikhail Titov
 */
public class WikiTransformerNodeTest extends RavenCoreTestCase {
    private FileResponseBuilder builder;
    private Node sriRootNode;
    private WikiTransformerNode transformer;
    
    @Before
    public void prepare() throws Exception {
        NetworkResponseService respService = registry.getService(NetworkResponseService.class);
        sriRootNode = respService.getNetworkResponseServiceNode();
        NodePathResolver pathResolver = registry.getService(NodePathResolver.class);
        assertNotNull(pathResolver);
        
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setCorePoolSize(50);
        executor.setMaximumPoolSize(50);
        assertTrue(executor.start());
        
        ContainerNode images = new ContainerNode("images");
        sriRootNode.addAndSaveChildren(images);
        assertTrue(images.start());
        
        BindingsContainer wikiRootNode = new BindingsContainer();
        wikiRootNode.setName("wiki");
        sriRootNode.addAndSaveChildren(wikiRootNode);
        assertTrue(wikiRootNode.start());
        wikiRootNode.addBinding(BindingNames.ROOT_PATH, "/raven");
        
//        group.addBinding(BindingNames.PATH_BINDING, new PathClosure(testsNode, "/raven", pathResolver, testsNode));
        
        builder = new FileResponseBuilder();
        builder.setName("builder");
        wikiRootNode.addAndSaveChildren(builder);
        builder.setResponseContentType("text/html");
        builder.setExecutor(executor);
        builder.getFile().setEncoding(Charset.forName("utf-8"));
        
        transformer = new WikiTransformerNode();        
        transformer.setName("wiki");        
        builder.addAndSaveChildren(transformer);
        transformer.setImageBase(images);
        transformer.setLinkBase(wikiRootNode);
    }
    
    @Test
    public void simpleTest() throws Exception{
        assertTrue(transformer.start());
        
        builder.getFile().setDataString("'''hello''' world!");
        assertTrue(builder.start());
        
        Object resp = builder.buildResponseContent(null, null);
        assertNotNull(resp);
        assertTrue(resp instanceof Outputable);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ((Outputable)resp).outputTo(buf);
        System.out.println("!!! ["+new String(buf.toByteArray(), "utf-8")+"]");
        assertEquals("\n<p><b>hello</b> world!</p>", new String(buf.toByteArray(), "utf-8"));
    }
    
    @Test
    public void imageTest() throws Exception {
        assertTrue(transformer.start());
        
        builder.getFile().setDataString("[[File:folder1/test.png|link=|]]");
        assertTrue(builder.start());
        
        Object resp = builder.buildResponseContent(null, null);
        assertNotNull(resp);
        assertTrue(resp instanceof Outputable);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ((Outputable)resp).outputTo(buf);
        System.out.println("!!! ["+new String(buf.toByteArray(), "utf-8")+"]");
        assertEquals("\n<p><img src=\"/raven/sri/images/folder1/test.png\" width=\"220\" />\n</p>", 
                new String(buf.toByteArray(), "utf-8"));
    }
    
    @Test
    public void linkTest() throws Exception {
        assertTrue(transformer.start());
        
        builder.getFile().setDataString("[[hello]] world!");
        assertTrue(builder.start());
        
        Object resp = builder.buildResponseContent(null, null);
        assertNotNull(resp);
        assertTrue(resp instanceof Outputable);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ((Outputable)resp).outputTo(buf);
        System.out.println("!!! ["+new String(buf.toByteArray(), "utf-8")+"]");
        assertEquals("\n<p><a href=\"/raven/sri/wiki/Hello\" title=\"hello\">hello</a> world!</p>", 
                new String(buf.toByteArray(), "utf-8"));
        
    }
    
    @Test
    public void templateTest() throws Exception {
        assertTrue(transformer.start());
        
        builder.getFile().setDataString("{{hello}} world!");
        assertTrue(builder.start());
        
        Object resp = builder.buildResponseContent(null, null);
        assertNotNull(resp);
        assertTrue(resp instanceof Outputable);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ((Outputable)resp).outputTo(buf);
        System.out.println("!!! ["+new String(buf.toByteArray(), "utf-8")+"]");
//        assertEquals("\n<p><a href=\"/raven/sri/wiki/Hello\" title=\"hello\">hello</a> world!</p>", 
//                new String(buf.toByteArray(), "utf-8"));
        
    }
}

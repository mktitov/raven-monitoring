/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.net.impl;

import java.util.Map;
import java.util.zip.GZIPInputStream;
import javax.activation.DataSource;
import org.apache.commons.io.IOUtils;
import static org.junit.Assert.assertTrue;
import org.raven.tree.NodeAttribute;
import org.junit.Before;
import org.junit.Test;
import org.raven.TestScheduler;
import org.raven.cache.TemporaryFileManagerNode;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.GzipContentTransformer;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class ViewableObjectsMessagePartNodeTest extends RavenCoreTestCase {
    private TestScheduler scheduler;
    private ExecutorServiceNode executor;
    private TemporaryFileManagerNode manager;
    private ViewableObjectsMessagePartNode part;
    private TestViewable source;
    private MailWriterNode mailer;

    @Before
    public void prepare()
    {
        scheduler = new TestScheduler();
        scheduler.setName("scheduler");
        testsNode.addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());
        
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setCorePoolSize(50);
        executor.setMaximumPoolSize(50);
        assertTrue(executor.start());

        manager = new TemporaryFileManagerNode();
        manager.setName("manager");
        testsNode.addAndSaveChildren(manager);
        manager.setDirectory("target/");
        manager.setScheduler(scheduler);
        assertTrue(manager.start());        
        
        source = new TestViewable();
        source.setName("source");
        tree.getRootNode().addAndSaveChildren(source);
        assertTrue(source.start());

        mailer = new MailWriterNode();
        mailer.setName("mailer");
        tree.getRootNode().addAndSaveChildren(mailer);

        part = new ViewableObjectsMessagePartNode();
        part.setName("part");
        mailer.addAndSaveChildren(part);
        part.setContentType("test");
        part.setSource(source);
        
    }

    @Test
    public void syncSourceRefreshAttributes() throws Exception
    {
        source.addRefreshAttribute(new NodeAttributeImpl("attr1", String.class, "v1", "d1"));
        assertTrue(part.start());
        NodeAttribute attr = part.getAttr("attr1");
        checkAttribute(attr, "v1", "d1", String.class);

        attr.setValue("v1 updated");
        source.addRefreshAttribute(new NodeAttributeImpl("attr2", Integer.class, 1, "d2"));
        part.stop();
        assertTrue(part.start());
        checkAttribute(part.getAttr("attr1"), "v1 updated", "d1", String.class);
        checkAttribute(part.getAttr("attr2"), 1, "d2", Integer.class);

        source.removeRefreshAttribute("attr1");
        part.stop();
        assertTrue(part.start());
        assertNull(part.getAttr("attr1"));
    }

    @Test
    public void getContent() throws Exception
    {
        source.addRefreshAttribute(new NodeAttributeImpl("attr1", String.class, "v1", "d1"));
        assertTrue(part.start());
        Object obj = part.getContent(new DataContextImpl());
        assertNotNull(obj);
        assertTrue(obj instanceof String);
        assertEquals(
                "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\"/>"
                + "<style>table { border:2px solid; border-collapse: collapse; }th { border:2px solid; }td { border:1px solid; }</style>"
                + "</head><body><div>v1</div></body></html>"
                , obj);
    }
    
    @Test
    public void getContentWithTemporaryFileManager() throws Exception {
        part.setTemporaryFileManager(manager);
        part.setUseTemporaryFileManager(Boolean.TRUE);
        source.addRefreshAttribute(new NodeAttributeImpl("attr1", String.class, "v1", "d1"));
        assertTrue(part.start());
        Object obj = part.getContent(new DataContextImpl());
        assertNotNull(obj);
        assertTrue(obj instanceof DataSource);        
        assertEquals(
                "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\"/>"
                + "<style>table { border:2px solid; border-collapse: collapse; }th { border:2px solid; }td { border:1px solid; }</style>"
                + "</head><body><div>v1</div></body></html>"
                , IOUtils.toString(((DataSource)obj).getInputStream(), mailer.getContentEncoding()));
    }

    @Test
    public void getContentWithContentTrandformers() throws Exception {
        part.setTemporaryFileManager(manager);
        part.setExecutor(executor);
        GzipContentTransformer gzip = new GzipContentTransformer();
        gzip.setName("gzip");
        part.addAndSaveChildren(gzip);
        assertTrue(gzip.start());
        source.addRefreshAttribute(new NodeAttributeImpl("attr1", String.class, "v1", "d1"));
        assertTrue(part.start());
        Object obj = part.getContent(new DataContextImpl());
        assertNotNull(obj);
        assertTrue(obj instanceof DataSource);
        GZIPInputStream gunzip = new GZIPInputStream(((DataSource)obj).getInputStream());
        assertEquals(
                "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\"/>"
                + "<style>table { border:2px solid; border-collapse: collapse; }th { border:2px solid; }td { border:1px solid; }</style>"
                + "</head><body><div>v1</div></body></html>"
                , IOUtils.toString(gunzip, mailer.getContentEncoding()));
    }

    @Test
    public void refreshAttributesAttrTest() throws Exception
    {
        NodeAttributeImpl attr = new NodeAttributeImpl("test", String.class, "test value", null);
        attr.setOwner(part);
        attr.init();
        part.addAttr(attr);
        attr.save();

        part.setRefreshAttributes("test");
        assertTrue(part.start());
        assertNotNull(part.getAttr("test"));
        part.getContent(new DataContextImpl());

        Map<String, NodeAttribute> refAttrs = source.getLastSendedRefAttrs();
        assertNotNull(refAttrs);
        assertNotNull(refAttrs.get("test"));
        assertEquals("test value", refAttrs.get("test").getValue());
    }

    private void checkAttribute(NodeAttribute attr, Object val, String desc, Class type)
    {
        assertNotNull(attr);
        assertEquals(val, attr.getRealValue());
        assertEquals(desc, attr.getDescription());
        assertEquals(ViewableObjectsMessagePartNode.SOURCE_ATTR, attr.getParentAttribute());
        assertEquals(type, attr.getType());
    }
}
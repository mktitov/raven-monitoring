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
import org.raven.tree.NodeAttribute;
import org.junit.Before;
import org.junit.Test;
import org.raven.ds.impl.DataContextImpl;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class ViewableObjectsMessagePartNodeTest extends RavenCoreTestCase
{
    private ViewableObjectsMessagePartNode part;
    private TestViewable source;
    private MailWriterNode mailer;

    @Before
    public void prepare()
    {
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
        NodeAttribute attr = part.getNodeAttribute("attr1");
        checkAttribute(attr, "v1", "d1", String.class);

        attr.setValue("v1 updated");
        source.addRefreshAttribute(new NodeAttributeImpl("attr2", Integer.class, 1, "d2"));
        part.stop();
        assertTrue(part.start());
        checkAttribute(part.getNodeAttribute("attr1"), "v1 updated", "d1", String.class);
        checkAttribute(part.getNodeAttribute("attr2"), 1, "d2", Integer.class);

        source.removeRefreshAttribute("attr1");
        part.stop();
        assertTrue(part.start());
        assertNull(part.getNodeAttribute("attr1"));
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
    public void refreshAttributesAttrTest() throws Exception
    {
        NodeAttributeImpl attr = new NodeAttributeImpl("test", String.class, "test value", null);
        attr.setOwner(part);
        attr.init();
        part.addNodeAttribute(attr);
        attr.save();

        part.setRefreshAttributes("test");
        assertTrue(part.start());
        assertNotNull(part.getNodeAttribute("test"));
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
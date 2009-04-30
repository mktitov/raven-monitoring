/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.ds.impl;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.raven.PushOnDemandDataSource;
import org.raven.RavenCoreTestCase;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class DataSourceSessionAttributeNodeTest extends RavenCoreTestCase
{
    @Test
    public void forwardDataSourceAttributesTest()
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        NodeAttribute attr = new NodeAttributeImpl("testAttr", String.class, null, null);
        ds.addConsumerAttribute(attr);

        DataSourceSessionAttributeNode sessAttr = new DataSourceSessionAttributeNode();
        sessAttr.setName("sessAttr");
        tree.getRootNode().addAndSaveChildren(sessAttr);
        sessAttr.setAttributeType(String.class);
        sessAttr.setDataSource(ds);
        assertTrue(sessAttr.start());
        assertNotNull(sessAttr.getNodeAttribute("testAttr"));
        List<NodeAttribute> attrs = new ArrayList<NodeAttribute>();
        sessAttr.fillConsumerAttributes(attrs);
        assertEquals(0, attrs.size());

        sessAttr.setDataSource(null);
        sessAttr.setForwardDataSourceAttributes(true);
        sessAttr.setDataSource(ds);
        assertNull(sessAttr.getNodeAttribute("testAttr"));
        sessAttr.fillConsumerAttributes(attrs);
        assertEquals(1, attrs.size());
        assertSame(attr, attrs.get(0));
    }
}
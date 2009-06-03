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

import java.util.Arrays;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class AttributeValueDataSourceNodeTest extends RavenCoreTestCase
{
    @Test
    public void test()
    {
        AttributeValueDataSourceNode ds = new AttributeValueDataSourceNode();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.setValue("test");
        assertTrue(ds.start());

        SafeDataConsumer collector = new SafeDataConsumer();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(ds);
        assertTrue(collector.start());

        assertEquals("test", collector.refereshData(null));
    }

    @Test
    public void dataConsumerAttributesTest() throws Exception
    {
        AttributeValueDataSourceNode ds = new AttributeValueDataSourceNode();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        NodeAttribute valAttr = ds.getNodeAttribute("value");
        valAttr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        ds.setValue("'hello '+consAttr1+consAttr2");
        NodeAttributeImpl consAttr =
                new NodeAttributeImpl("consAttr1", String.class, "world", null);
        consAttr.setOwner(ds);
        consAttr.setValueHandlerType(DataConsumerAttributeValueHandlerFactory.TYPE);
        consAttr.save();
        consAttr.init();
        ds.addNodeAttribute(consAttr);
        NodeAttributeImpl consAttr2 =
                new NodeAttributeImpl("consAttr2", String.class, "world", null);
        consAttr2.setOwner(ds);
        consAttr2.setValueHandlerType(DataConsumerAttributeValueHandlerFactory.TYPE);
        consAttr2.save();
        consAttr2.init();
        ds.addNodeAttribute(consAttr2);
        ds.setRequiredAttributes("consAttr1");
        ds.setLogLevel(LogLevel.DEBUG);
        assertTrue(ds.start());


        SafeDataConsumer collector = new SafeDataConsumer();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(ds);
        assertTrue(collector.start());

        NodeAttribute attr;
        attr = collector.getNodeAttribute("consAttr1");
        assertNotNull(attr);
        assertTrue(attr.isRequired());
        assertNull(attr.getValueHandlerType());
        attr.setValue("world");
        attr = collector.getNodeAttribute("consAttr2");
        assertNotNull(attr);
        assertNull(attr.getValueHandlerType());
        assertFalse(attr.isRequired());

        attr = new NodeAttributeImpl("consAttr2", String.class, "!", null);
        attr.setOwner(collector);
        attr.init();

        assertEquals("hello world!", collector.refereshData(Arrays.asList(attr)));
    }
}
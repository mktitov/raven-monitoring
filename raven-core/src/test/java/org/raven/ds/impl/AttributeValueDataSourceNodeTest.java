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

import org.junit.Test;
import org.raven.RavenCoreTestCase;
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

    public void dataConsumerAttributesTest()
    {
        AttributeValueDataSourceNode ds = new AttributeValueDataSourceNode();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.setValue("test");
        assertTrue(ds.start());

        NodeAttributeImpl consAttr = new NodeAttributeImpl("consAttr", String.class, "world", null);

        SafeDataConsumer collector = new SafeDataConsumer();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(ds);
        assertTrue(collector.start());

        assertEquals("test", collector.refereshData(null));
        
    }
}
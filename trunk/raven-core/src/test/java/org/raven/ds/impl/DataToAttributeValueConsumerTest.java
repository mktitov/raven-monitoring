/*
 *  Copyright 2008 Mikhail Titov.
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
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.impl.objects.TestDataSource2;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeAttributeListener;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class DataToAttributeValueConsumerTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        TestDataSource2 ds = new TestDataSource2();
        ds.setName("ds");
        tree.getRootNode().addChildren(ds);
        ds.save();
        ds.init();
        
        ContainerNode recipient = new ContainerNode("recipient");
        tree.getRootNode().addChildren(recipient);
        recipient.save();
        recipient.init();
        recipient.start();
        assertEquals(Status.STARTED, recipient.getStatus());
        NodeAttribute attr = new NodeAttributeImpl("attr", Integer.class, null, null);
        recipient.addNodeAttribute(attr);
        attr.setOwner(recipient);
        attr.save();
        attr.init();
        
        DataToAttributeValueConsumer consumer = new DataToAttributeValueConsumer();
        consumer.setName("consumer");
        tree.getRootNode().addChildren(consumer);
        consumer.save();
        consumer.init();
        NodeAttribute dsAttr = consumer.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE);
        dsAttr.setValue(ds.getPath());
        dsAttr.save();
        NodeAttribute attrAttr = consumer.getNodeAttribute(
                DataToAttributeValueConsumer.ATTRIBUTE_ATTRIBUTE);
        assertNotNull(attrAttr);
        attrAttr.setValue("../recipient@attr");
        assertTrue(attrAttr.isExpressionValid());
        assertNotNull(attrAttr.getValue());
        attrAttr.save();
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());
        
        NodeAttributeListener listener = createMock(NodeAttributeListener.class);
        listener.nodeAttributeValueChanged(
                (Node)anyObject(), (NodeAttribute)anyObject(), isNull(), eq(new Integer(1)));
        replay(listener);
        
        recipient.addNodeAttributeDependency("attr", listener);
        consumer.setData(ds, 1, new DataContextImpl());
        consumer.setData(ds, 1, new DataContextImpl());
        
        verify(listener);
        
        tree.reloadTree();
        
        recipient = (ContainerNode) tree.getNode(recipient.getPath());
        attr = recipient.getNodeAttribute(attr.getName());
        assertNotNull(attr);
        assertEquals(1, attr.getRealValue());
    }
}

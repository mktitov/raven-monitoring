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

package org.raven.tree.impl;

import org.apache.tapestry.ioc.RegistryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.ServiceTestCase;
import org.raven.conf.Configurator;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeListener;
import org.raven.tree.impl.objects.NodeWithParameter2;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class ParameterFieldTransformerWorkerTest extends ServiceTestCase 
{
    private Node parentNode;
    private NodeAttribute attribute;
    private NodeListener listener;
    private Configurator configurator;
    
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Before @After
    public void initTest()
    {
        configurator = registry.getService(Configurator.class);
        configurator.getTreeStore().removeNodes();
    }
    
    @Test
    public void test() throws Exception
    {
        NodeWithParameter2 node = new NodeWithParameter2();
        createAndTrainMocks(node);
        node.setName("node");
        node.setParent(parentNode);
        assertEquals("parentValue", node.getParameter());
        
        node.setParameter("test");
        assertEquals("test", node.getParameter());
        
        assertEquals(0, node.getParameter2());
        node.setParameter2(10);
        assertEquals(10, node.getParameter2());
        
        node.addListener(listener);
        configurator.getTreeStore().saveNode(node);
        node.init();
        
        node.setParameter("newValue");
        node.setParameter2(20);
        
        verify(parentNode, attribute, listener);
    }

    private void createAndTrainMocks(Node node)
    {
        parentNode = createMock(Node.class);
        attribute = createMock(NodeAttribute.class);
        listener = createMock(NodeListener.class);
        
        expect(parentNode.getId()).andReturn(-1);
        expect(parentNode.getParent()).andReturn(null).anyTimes();
        expect(parentNode.getNodeAttribute("parameter")).andReturn(attribute).times(2);
        expect(attribute.getRealValue()).andReturn("parentValue").times(2);
        
        expect(parentNode.getName()).andReturn("parentNode").anyTimes();
        listener.nodeStatusChanged(node, Status.CREATED, Status.INITIALIZED);
        listener.nodeAttributeValueChanged(
                eq(node), isA(NodeAttribute.class), eq("test"), eq("newValue"));
        listener.nodeAttributeValueChanged(
                eq(node), isA(NodeAttribute.class), eq("10"), eq("20"));
        
        
        replay(parentNode, attribute, listener);
    }
}

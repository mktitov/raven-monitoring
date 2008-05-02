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

import javax.jdo.identity.LongIdentity;
import org.apache.tapestry.ioc.RegistryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.ServiceTestCase;
import org.raven.conf.Configurator;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeLogic;
import org.weda.constraints.ConstraintException;

/**
 *
 * @author Mikhail Titov
 */
public class BaseNodeTest extends ServiceTestCase
{
    private Configurator configurator;
    
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Before()
    public void testInit()
    {
        configurator = registry.getService(Configurator.class);
    }
    
    @After
    public void testShutdown()
    {
        configurator.deleteAll(BaseNode.class);
    }
    
    @Test
    public void nodeAttribute_saveAndLoad() throws ConstraintException
    {
        configurator.deleteAll(BaseNode.class);
        configurator.deleteAll(NodeAttributeImpl.class);
        
        BaseNode node = new ContainerNode("node with attributes");
        configurator.saveInTransaction(node);
        
        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setOwner(node);
        attr.setName("attr1");
        attr.setDescription("description");
        attr.setParameterName("param1");
        attr.setParentAttribute("parentAttr");
        attr.setType(String.class);
        attr.setValue("value");
        
        node.addNodeAttribute(attr);
        
        configurator.saveInTransaction(attr);
        
        node = configurator.getObjectById(new LongIdentity(BaseNode.class, node.getId()));
        assertNotNull(node);
        
        assertNotNull(node.getNodeAttributes());
        assertEquals(1, node.getNodeAttributes().size());
        
        NodeAttribute attr1 = node.getNodeAttribute("attr1");
        
        assertNotNull(attr1);
        assertEquals("description", attr1.getDescription());
        assertEquals("param1", attr1.getParameterName());
        assertEquals("parentAttr", attr1.getParentAttribute());
        assertEquals(String.class, attr1.getType());
        assertEquals("value", attr1.getValue());
        
        Object attrId = configurator.getObjectId(node);
        assertNotNull(attrId);
        
        assertNotNull(configurator.getObjectById(attrId));
        
        configurator.delete(node);
        
        assertNull(configurator.getObjectById(attrId));
    }
    
    @Test
    public void node_saveAndLoad() throws ConstraintException
    {
        configurator.deleteAll(BaseNode.class);
        
        ContainerNode node = new ContainerNode("node");
        node.setNodeLogicType(NodeLogic.class);
        
        configurator.saveInTransaction(node);
        
        Object nodeId = configurator.getObjectId(node);
        
        node = configurator.getObjectById(nodeId);
        
        assertNotNull(node);
        assertEquals("node", node.getName());
        assertEquals(NodeLogic.class, node.getNodeLogicType());
        
        //parent node test
        ContainerNode parentNode = new ContainerNode("parent");
        node.setParent(parentNode);
        
        configurator.saveInTransaction(parentNode);
        configurator.saveInTransaction(node);
        
        Object parentNodeId = configurator.getObjectId(parentNode);
        
        node = configurator.getObjectById(nodeId);
        parentNode = configurator.getObjectById(parentNodeId);
        
        assertNotNull(node);
        assertNotNull(parentNode);
        
        assertEquals(parentNode.getId(), node.getParent().getId());
    }
    
    @Test
    public void node_init()
    {
        //test node dependencies
        //node logic create
        //attributes and parameters synchronization
    }
}

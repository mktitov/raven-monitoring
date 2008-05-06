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

package org.raven.tree.store.impl;

import java.sql.SQLException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.raven.tree.Node;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.store.TreeStoreError;
import org.weda.constraints.ConstraintException;

/**
 *
 * @author Mikhail Titov
 */
public class H2TreeStoreTest extends Assert
{
    private static H2TreeStore store;
    
    @BeforeClass
    public static void initTests() throws TreeStoreError
    {
        store = new H2TreeStore();
        store.init("jdbc:h2:tcp://localhost/~/test;TRACE_LEVEL_FILE=3", "sa", "");
    }
    
    @Test
    public void saveAndLoad() throws TreeStoreError, SQLException
    {
        store.removeNodes();
        
        ContainerNode node = new ContainerNode();
        node.setName("node");
        store.saveNode(node);
        
        Node loadedNode = store.getNode(node.getId());
        assertNotNull(loadedNode);
        assertEquals("node", loadedNode.getName());
        assertEquals(node.getId(), loadedNode.getId());
        assertNull(loadedNode.getParent());
        
        loadedNode.setName("node 1");
        
        store.saveNode(loadedNode);
        
        assertEquals(node.getId(), loadedNode.getId());
        
        Node updatedNode = store.getNode(loadedNode.getId());
        assertNotNull(updatedNode);
        assertEquals("node 1", updatedNode.getName());
        assertEquals(node.getId(), updatedNode.getId());
        assertNull(updatedNode.getParent());
        
        store.removeNode(updatedNode.getId());
        
        Node removedNode = store.getNode(updatedNode.getId());
        assertNull(removedNode);
    }
    
    @Test
    public void saveAndLoadAttributes() throws SQLException, TreeStoreError, ConstraintException
    {
        store.removeNodes();
        
        ContainerNode node = new ContainerNode();
        node.setName("node");
        
        store.saveNode(node);
        
        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setDescription("description");
        attr.setName("name");
        attr.setOwner(node);
        attr.setParameterName("parameterName");
        attr.setParentAttribute("parentAttribute");
        attr.setType(String.class);
        attr.setValue("value");
        
        store.saveNodeAttribute(attr);
        
        assertTrue(attr.getId()>0);
        
        ContainerNode node1 = (ContainerNode) store.getNode(node.getId());
        assertNotNull(node1);
        
        assertNotNull(node1.getNodeAttributes());
        assertEquals(1, node1.getNodeAttributes().size());
        
        NodeAttributeImpl attr1 = (NodeAttributeImpl) node1.getNodeAttribute("name");
        assertNotNull(attr1);
        assertEquals(attr.getId(), attr1.getId());
        assertSame(node1, attr1.getOwner());
        assertEquals("parameterName", attr1.getParameterName());
        assertEquals("parentAttribute", attr1.getParentAttribute());
        assertEquals(String.class, attr1.getType());
        assertEquals("value", attr1.getValue());
        
        attr1.setDescription("description1");
        attr1.setName("name1");
        attr1.setOwner(node);
        attr1.setParameterName("parameterName1");
        attr1.setParentAttribute("parentAttribute1");
        attr1.setType(Integer.class);
        attr1.setValue("1");
        
        store.saveNodeAttribute(attr1);
        
        node1 = (ContainerNode) store.getNode(node.getId());
        assertNotNull(node1);
        assertNotNull(node1.getNodeAttributes());
        assertEquals(1, node1.getNodeAttributes().size());
        
        attr1 = (NodeAttributeImpl) node1.getNodeAttribute("name1");
        assertNotNull(attr1);
        assertEquals(attr.getId(), attr1.getId());
        assertSame(node1, attr1.getOwner());
        assertEquals("parameterName1", attr1.getParameterName());
        assertEquals("parentAttribute1", attr1.getParentAttribute());
        assertEquals(Integer.class, attr1.getType());
        assertEquals("1", attr1.getValue());
        
        store.removeNodeAttribute(attr1.getId());
        node1 = (ContainerNode) store.getNode(node.getId());
        assertNotNull(node1);
        assertNull(node1.getNodeAttributes());
        
    }
    
    @Test
    public void getRootNode() throws Exception
    {
        store.removeNodes();
        
        ContainerNode rootNode = new ContainerNode("root");
        store.saveNode(rootNode);
        
        ContainerNode childNode = new ContainerNode("child");
        rootNode.addChildren(childNode);
        
        store.saveNode(childNode);
        
        Node node = store.getRootNode();
        assertNotNull(node);
        
        assertNotNull(node.getChildrens());
        assertEquals(1, node.getChildrens().size());
        
        Node node2 = node.getChildren("child");
        assertNotNull(node2);
        
    }
}

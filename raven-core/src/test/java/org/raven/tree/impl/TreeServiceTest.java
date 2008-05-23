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

import java.util.Set;
import org.apache.tapestry.ioc.RegistryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.ServiceTestCase;
import org.raven.conf.Configurator;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeListener;
import org.raven.tree.NodeNotFoundError;
import org.raven.tree.Tree;
import org.raven.tree.impl.objects.AttributesGeneratorNode;
import org.raven.tree.impl.objects.NodeWithNodeParameter;
import org.raven.tree.impl.objects.NodeWithParameters;
import org.weda.constraints.ConstraintException;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class TreeServiceTest extends ServiceTestCase
{
    private static boolean checkTreeExecuted = false;
    private Tree tree;
    private Configurator configurator;
    
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Before
    public void initTest()
    {
        tree = registry.getService(Tree.class);
        assertNotNull(tree);
        
        configurator = registry.getService(Configurator.class);
        assertNotNull(configurator);
    }
    
    @Test
    public void getAvailableNodesTypes()
    {
        Class[] nodesTypes = tree.getAvailableNodesTypes();
        assertNotNull(nodesTypes);
        
        //at least two nodes types must be in the array: ContainerNode, LeafNode
        assertTrue(nodesTypes.length>=2);
    }
    
    @Test()
    public void initTree1()
    {
        checkTree();
    }

    @Test()
    public void initTree2()
    {
        checkTree();
    }
    
    @Test()
    public void remove()
    {
        try
        {
            Node root = tree.getRootNode();
            int rootId = root.getId();
            assertNotNull(root);

            Node systemNode = root.getChildren(SystemNode.NAME);
            int systemNodeId = systemNode.getId();
            assertNotNull(systemNode);

            Node dsNode = systemNode.getChildren(DataSourcesNode.NAME);
            int dsNodeId = dsNode.getId();
            assertNotNull(dsNode);

            tree.remove(systemNode);
            try{
                tree.getNode(systemNode.getPath());
                fail();
            }
            catch(NodeNotFoundError e) {}
            assertNull(configurator.getTreeStore().getNode(systemNodeId));
            assertNull(configurator.getTreeStore().getNode(dsNodeId));
        }finally
        {
            configurator.getTreeStore().removeNodes();
        }
    }
    
    //test node dependencies
    //node logic create
    //attributes and parameters synchronization
    
    @Test
    public void nodeInit_woAttributes()
    {
        BaseNode node = new BaseNode(null, false, false);
        node.init();
        
        assertEquals(Node.Status.INITIALIZED, node.getStatus());
    }
    
    @Test
    public void nodeInit_woNodeTypeAttribute()
    {
        BaseNode node = new BaseNode(null, false, false);
        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setName("attr");
        attr.setType(String.class);
        node.addNodeAttribute(attr);
        
        node.init();
        
        assertEquals(Node.Status.INITIALIZED, node.getStatus());
    }
    
    @Test
    public void nodeInit_wNodeTypeAttribute() throws ConstraintException
    {
        ContainerNode node1 = new ContainerNode("node1");
        tree.getRootNode().addChildren(node1);
        
        ContainerNode node2 = new ContainerNode("node2");
        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setName("attr");
        attr.setOwner(node2);
        attr.setType(ContainerNode.class);
        attr.setValue(node1.getPath());
        node2.addNodeAttribute(attr);
        tree.getRootNode().addChildren(node2);
        
        assertNull(node1.getDependentNodes());
        
        node2.init();
        
        Set<Node> dependentNodes = node1.getDependentNodes();
        assertNotNull(dependentNodes);
        assertEquals(1, dependentNodes.size());
        assertSame(node2, dependentNodes.iterator().next());
        assertEquals(Node.Status.CREATED, node2.getStatus());
        
        node1.init();
        
        assertEquals(Node.Status.INITIALIZED, node1.getStatus());
        assertEquals(Node.Status.STARTED, node2.getStatus());
    }
    
    @Test
    public void nodeInit_node_wParameters() throws ConstraintException 
    {
        //synchronization
        //store
        //setValue
        //getValue
        NodeWithParameters node = new NodeWithParameters();
        node.setName("node");
        
        tree.getRootNode().addChildren(node);
        
        configurator.getTreeStore().saveNode(node);
        
        node.init();
        
        assertEquals(Node.Status.INITIALIZED, node.getStatus());
        assertNotNull(node.getNodeAttributes());
        assertEquals(1, node.getNodeAttributes().size());
        
        checkAttributes(node, null);
        
        NodeAttribute attr = node.getNodeAttribute("string parameter");
        attr.setValue("value");
        
        checkAttributes(node, "value");
        
        configurator.getTreeStore().saveNodeAttribute(attr);
        
        tree.reloadTree();
        
        node = (NodeWithParameters) tree.getRootNode().getChildren("node");
        checkAttributes(node, "value");
    }
    
    @Test
    public void nodeListener() throws ConstraintException
    {
        ContainerNode node = new ContainerNode("name");
        NodeAttribute attr = new NodeAttributeImpl("attr", String.class, "1", "desc");
        node.addNodeAttribute(attr);
        attr.setOwner(node);
        
        NodeListener listener = createStrictMock(NodeListener.class);
        listener.nodeStatusChanged(eq(node), eq(Status.CREATED), eq(Status.INITIALIZED));
        listener.nodeNameChanged(eq(node), eq("name"), eq("newName"));
        listener.nodeAttributeValueChanged(eq(node), eq(attr), eq("1"), eq("2"));
        replay(listener);
        
        node.addListener(listener);
        node.init();
        node.setName("newName");
        node.getNodeAttribute("attr").setValue("2");
        
        verify(listener);
    }
    
    @Test
    public void childNodeNameChanging()
    {
        ContainerNode node = new ContainerNode("node");
        ContainerNode childNode = new ContainerNode("child");
        node.addChildren(childNode);
        
        node.init();
        childNode.init();
        
        assertEquals(childNode, node.getChildren("child"));
        
        childNode.setName("newChildNodeName");

        assertNull(node.getChildren("child"));
        assertEquals(childNode, node.getChildren("newChildNodeName"));
    }
    
    @Test
    public void attributesGenerator() throws ConstraintException
    {
        configurator.getTreeStore().removeNodes();
        tree.reloadTree();
        
        Node node = new AttributesGeneratorNode();
        node.setName("genNode");
        
        tree.getRootNode().addChildren(node);
        configurator.getTreeStore().saveNode(node);
        node.init();
        
        NodeWithNodeParameter node1 = new NodeWithNodeParameter();
        node1.setName("node");
        tree.getRootNode().addChildren(node1);
        configurator.getTreeStore().saveNode(node1);
        node1.init();
        
        NodeAttribute attr = node1.getNodeAttribute("node");
        assertNotNull(attr);
        
        attr.setValue(Node.NODE_SEPARATOR+"genNode");
        configurator.getTreeStore().saveNodeAttribute(attr);
        
        attr = node1.getNodeAttribute("gAttr");
        assertNotNull(attr);
        assertEquals("node", attr.getParentAttribute());
        assertSame(node, node1.getNode());
        
        tree.reloadTree();
        
        node1 = (NodeWithNodeParameter) tree.getRootNode().getChildren("node");
        assertNotNull(node1);
        
        attr = node1.getNodeAttribute("gAttr");
        assertNotNull(attr);
        assertEquals("node", attr.getParentAttribute());
        assertEquals(node, node1.getNode());
        
        node1.getNodeAttribute("node").setValue(null);
        attr = node1.getNodeAttribute("gAttr");
        assertNull(attr);
        
        tree.reloadTree();
        
        node1 = (NodeWithNodeParameter) tree.getRootNode().getChildren("node");
        assertNotNull(node1);
        
        attr = node1.getNodeAttribute("gAttr");
        assertNull(attr);
    }
    
    private void checkAttributes(NodeWithParameters node, String value)
    {
        assertNotNull(node.getNodeAttributes());
        assertEquals(1, node.getNodeAttributes().size());

        NodeAttribute stringAttr = node.getNodeAttribute("string parameter");
        assertNotNull(stringAttr);
        assertEquals(String.class, stringAttr.getType());
        assertEquals("stringParameter", stringAttr.getParameterName());
        assertNull(stringAttr.getParentAttribute());
        assertEquals("This is a string parameter", stringAttr.getDescription());
        assertEquals(stringAttr.getOwner(), node);
        assertEquals(value, stringAttr.getValue());
        
        assertEquals(value, node.getStringParameter());
    }

    private void checkTree() throws NodeNotFoundError
    {
        if (!checkTreeExecuted)
        {
            checkTreeExecuted = true;
            configurator.getTreeStore().removeNodes();
        }
        assertNotNull(tree.getRootNode());

        Node systemNode = tree.getNode(Node.NODE_SEPARATOR+SystemNode.NAME);
        assertNotNull(systemNode);
    }
}

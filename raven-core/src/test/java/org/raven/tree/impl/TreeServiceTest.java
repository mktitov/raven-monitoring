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

import java.util.List;
import java.util.Set;
import org.apache.tapestry.ioc.RegistryBuilder;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.ServiceTestCase;
import org.raven.conf.Configurator;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.template.TemplatesNode;
import org.raven.tree.AttributeReference;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeAttributeListener;
import org.raven.tree.NodeListener;
import org.raven.tree.NodeNotFoundError;
import org.raven.tree.ScanOperation;
import org.raven.tree.ScannedNodeHandler;
import org.raven.tree.SearchFilter;
import org.raven.tree.Tree;
import org.raven.tree.impl.objects.AnyChildsNode;
import org.raven.tree.impl.objects.AnyParentNode;
import org.raven.tree.impl.objects.AttributesGeneratorNode;
import org.raven.tree.impl.objects.ChildNode1;
import org.raven.tree.impl.objects.ChildNode2;
import org.raven.tree.impl.objects.ChildNode3;
import org.raven.tree.impl.objects.NodeWithFixedChilds;
import org.raven.tree.impl.objects.NodeWithIntegerParameter;
import org.raven.tree.impl.objects.NodeWithNodeParameter;
import org.raven.tree.impl.objects.NodeWithParameters;
import org.raven.tree.impl.objects.NodeWithParentChilds;
import org.raven.tree.impl.objects.ParentNode;
import org.raven.tree.store.TreeStore;
import org.weda.constraints.ConstraintException;
import org.weda.constraints.ReferenceValue;
import org.weda.services.TypeConverter;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class TreeServiceTest extends ServiceTestCase
{
    private static boolean checkTreeExecuted = false;
    private TreeStore store;
    private Tree tree;
    private Configurator configurator;
    private TypeConverter converter;
    
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Before
    public void initTest()
    {
        configurator = registry.getService(Configurator.class);
        assertNotNull(configurator);
        store = configurator.getTreeStore();
        
        tree = registry.getService(Tree.class);
        assertNotNull(tree);
        
        converter = registry.getService(TypeConverter.class);
        assertNotNull(converter);
    }
    
    @Test
    public void getChildNodesTypes()
    {
        store.removeNodes();
        tree.reloadTree();
        
        AnyChildsNode anyChildsNode = new AnyChildsNode();
        List<Class> types = tree.getChildNodesTypes(anyChildsNode);
        assertNotNull(types);
        assertTrue(types.contains(AnyChildsNode.class));
        assertTrue(types.contains(AnyParentNode.class));
        assertFalse(types.contains(ChildNode1.class));
        assertFalse(types.contains(ChildNode2.class));
        
        NodeWithFixedChilds nodeWithFixedChilds = new NodeWithFixedChilds();
        types = tree.getChildNodesTypes(nodeWithFixedChilds);
        assertNotNull(types);
        assertEquals(3, types.size());
        assertTrue(types.contains(ChildNode1.class));
        assertTrue(types.contains(ChildNode2.class));
        assertTrue(types.contains(ChildNode3.class));
        
        NodeWithParentChilds nodeWithParentChilds = new NodeWithParentChilds();
        nodeWithParentChilds.setName("nodeWithParentChilds");
        types = tree.getChildNodesTypes(nodeWithParentChilds);
        assertNotNull(types);
        assertEquals(1, types.size());
        assertTrue(types.contains(ChildNode3.class));
        //
        ParentNode parentNode = new ParentNode();
        parentNode.addChildren(nodeWithParentChilds);
        types = tree.getChildNodesTypes(nodeWithParentChilds);
        assertNotNull(types);
        assertEquals(2, types.size());
        assertTrue(types.contains(ChildNode1.class));
        assertTrue(types.contains(ChildNode3.class));
    }
    
    @Test()
    public void initTree1() throws InvalidPathException
    {
        checkTree();
    }

    @Test()
    public void initTree2() throws InvalidPathException
    {
        checkTree();
    }
    
    @Test()
    public void remove() throws InvalidPathException
    {
        store.removeNodes();
        tree.reloadTree();
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
            catch(InvalidPathException e) {}
            assertNull(store.getNode(systemNodeId));
            assertNull(store.getNode(dsNodeId));
        }finally
        {
//            store.removeNodes();
        }
    }
    
    //test node dependencies
    //node logic create
    //attributes and parameters synchronization
    
    @Test
    public void nodeInit_woAttributes()
    {
        store.removeNodes();
        tree.reloadTree();
        
        BaseNode node = new BaseNode();
        node.init();
        
        assertEquals(Node.Status.INITIALIZED, node.getStatus());
    }
    
    @Test
    public void nodeInit_woNodeTypeAttribute()
    {
        store.removeNodes();
        tree.reloadTree();
        
        BaseNode node = new BaseNode();
        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setName("attr");
        attr.setType(String.class);
        node.addNodeAttribute(attr);
        
        node.init();
        
        assertEquals(Node.Status.INITIALIZED, node.getStatus());
    }
    
    @Test
    public void nodeInit_wNodeTypeAttribute() throws Exception
    {
        store.removeNodes();
        tree.reloadTree();
        
        ContainerNode node1 = new ContainerNode("node1");
        tree.getRootNode().addChildren(node1);
        
        ContainerNode node2 = new ContainerNode("node2");
        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setName("attr");
        attr.setOwner(node2);
        attr.setType(ContainerNode.class);
        node2.addNodeAttribute(attr);
        tree.getRootNode().addChildren(node2);
        
        assertTrue(node1.getDependentNodes().isEmpty());
        
        node2.init();
        attr.setValue(node1.getPath());
        
        Set<Node> dependentNodes = node1.getDependentNodes();
        assertNotNull(dependentNodes);
        assertEquals(1, dependentNodes.size());
        assertSame(node2, dependentNodes.iterator().next());
        assertEquals(Node.Status.INITIALIZED, node2.getStatus());
        
        node1.init();
        
        assertEquals(Node.Status.INITIALIZED, node1.getStatus());
//        assertEquals(Node.Status.STARTED, node2.getStatus());
    }
    
    @Test
    public void nodeInit_node_wParameters() throws Exception 
    {
        store.removeNodes();
        tree.reloadTree();
        //synchronization
        //store
        //setValue
        //getValue
        NodeWithParameters node = new NodeWithParameters();
        node.setName("node");
        
        tree.getRootNode().addChildren(node);
        
        store.saveNode(node);
        
        node.init();
        
        assertEquals(Node.Status.INITIALIZED, node.getStatus());
        assertNotNull(node.getNodeAttributes());
        assertEquals(2, node.getNodeAttributes().size());
        
        checkAttributes(node, null);
        
        NodeAttribute attr = node.getNodeAttribute("stringParameter");
        attr.setValue("value");
        
        checkAttributes(node, "value");
        
        store.saveNodeAttribute(attr);
        
        tree.reloadTree();
        
        node = (NodeWithParameters) tree.getRootNode().getChildren("node");
        checkAttributes(node, "value");
        
        NodeAttribute enumAttr = node.getNodeAttribute("enumParameter");
        List<ReferenceValue> refValues = enumAttr.getReferenceValues();
        assertNotNull(refValues);
        assertEquals(2, refValues.size());
        assertEquals("ONE", refValues.get(0).getValue());
        assertEquals("ONE", refValues.get(0).getValueAsString());
        assertEquals("TWO", refValues.get(1).getValue());
        assertEquals("TWO", refValues.get(1).getValueAsString());
//        assertArrayEquals(new String[]{"ONE", "TWO"}, refValues.toArray());
    }
    
    @Test
    public void nodeListener() throws Exception
    {
        store.removeNodes();
        tree.reloadTree();
        
        ContainerNode node = new ContainerNode("name");
        NodeAttribute attr = new NodeAttributeImpl("attr", String.class, "1", "desc");
        node.addNodeAttribute(attr);
        attr.setOwner(node);
        attr.init();
        
        ContainerNode child = new ContainerNode("child");
        
        NodeListener listener = createStrictMock("listener", NodeListener.class);
        NodeListener childListener = createStrictMock("childListener", NodeListener.class);
        expect(listener.isSubtreeListener()).andReturn(false).anyTimes();
//        listener.nodeAttributeValueChanged(node, attr, null, "1");
        listener.nodeStatusChanged(eq(node), eq(Status.CREATED), eq(Status.INITIALIZED));
        listener.nodeNameChanged(eq(node), eq("name"), eq("newName"));
        listener.nodeAttributeValueChanged(eq(node), eq(attr), eq("1"), eq("2"));
        expect(listener.isSubtreeListener()).andReturn(false);
        listener.childrenAdded(node, child);
        expect(childListener.isSubtreeListener()).andReturn(false);
        childListener.nodeStatusChanged(child, Status.CREATED, Status.REMOVED);
        childListener.nodeRemoved(child);
        
        replay(listener, childListener);
        
        child.addListener(childListener);
        node.addListener(listener);
        node.init();
        node.setName("newName");
        node.getNodeAttribute("attr").setValue("2");
        node.addChildren(child);
        node.removeChildren(child);
        
        assertEquals(child.getStatus(), Status.REMOVED);
        
        verify(listener, childListener);
    }
    
    @Test
    public void subreeListener() throws Exception
    {
        store.removeNodes();
        tree.reloadTree();
        
        NodeListener listener = createMock(NodeListener.class);
        expect(listener.isSubtreeListener()).andReturn(true).anyTimes();
        listener.childrenAdded(isA(Node.class), isA(Node.class));
        replay(listener);
        
        Node parent = new ContainerNode();
        parent.setName("parent");
        Node child = new ContainerNode();
        child.setName("child");
        parent.addChildren(child);
        
        parent.addListener(listener);
        assertTrue(parent.getListeners().contains(listener));
        assertTrue(child.getListeners().contains(listener));
        
        Node child2 = new ContainerNode("child2");
        parent.addChildren(child2);
        assertTrue(child2.getListeners().contains(listener));
        
        parent.removeListener(listener);
        assertFalse(parent.getListeners().contains(listener));
        assertFalse(child.getListeners().contains(listener));
        assertFalse(child2.getListeners().contains(listener));
        
        verify(listener);
    }
    
    @Test
    public void childNodeNameChanging()
    {
        store.removeNodes();
        tree.reloadTree();
        
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
    public void attributesGenerator() throws Exception
    {
        store.removeNodes();
        tree.reloadTree();
        
        Node node = new AttributesGeneratorNode();
        node.setName("genNode");
        tree.getRootNode().addChildren(node);
        store.saveNode(node);
        node.init();
        
        NodeWithNodeParameter node1 = new NodeWithNodeParameter();
        node1.setName("node");
        tree.getRootNode().addChildren(node1);
        store.saveNode(node1);
        node1.init();
        
        NodeAttribute attr = node1.getNodeAttribute("node");
        assertNotNull(attr);
        attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        attr.setValue(Node.NODE_SEPARATOR+"genNode");
        store.saveNodeAttribute(attr);
        
        attr = node1.getNodeAttribute("gAttr");
        assertNotNull(attr);
        assertEquals("node", attr.getParentAttribute());
        assertSame(node, node1.getNode());
        attr.setValue("testVal");
        store.saveNodeAttribute(attr);
        
        tree.reloadTree();
        
        node1 = (NodeWithNodeParameter) tree.getRootNode().getChildren("node");
        assertNotNull(node1);
        
        attr = node1.getNodeAttribute("gAttr");
        assertNotNull(attr);
        assertEquals("node", attr.getParentAttribute());
        assertEquals("testVal", attr.getValue());
        assertEquals(node, node1.getNode());
        
//        store.removeNodeAttribute(attr.getId());
//        tree.reloadTree();
//        node1 = (NodeWithNodeParameter) tree.getRootNode().getChildren("node");
//        assertNotNull(node1);
//        attr = node1.getNodeAttribute("gAttr");
//        assertNotNull(attr);
//        assertEquals("node", attr.getParentAttribute());
//        assertNull(attr.getValue());
//        assertEquals(node, node1.getNode());
        
        node1.getNodeAttribute("node").setValue(null);
        store.saveNodeAttribute(node1.getNodeAttribute("node"));
        attr = node1.getNodeAttribute("gAttr");
        assertNull(attr);
        
        tree.reloadTree();
        
        node1 = (NodeWithNodeParameter) tree.getRootNode().getChildren("node");
        assertNotNull(node1);
        
        attr = node1.getNodeAttribute("gAttr");
        assertNull(attr);
    }
    
    @Test
    @Ignore
    public void attributeReference() throws Exception
    {
        store.removeNodes();
        
        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addChildren(node);
        store.saveNode(node);
        NodeAttribute attr = new NodeAttributeImpl("attr", Integer.class, null, null);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        store.saveNodeAttribute(attr);
        
        ContainerNode node2 = new ContainerNode("node2");
        tree.getRootNode().addChildren(node2);
        store.saveNode(node2);
        String attrPath = converter.convert(String.class, new AttributeReferenceImpl(attr), null);
        NodeAttribute refAttr = new NodeAttributeImpl("ref", AttributeReference.class, null, null);
        refAttr.setOwner(node2);
        node2.addNodeAttribute(refAttr);
        refAttr.setValue(attrPath);
        store.saveNodeAttribute(refAttr);
        
////        assertTrue(refAttr.isAttributeReference());
////        AttributeReference ref = refAttr.getAttributeReference();
//        assertNotNull(ref);
//        assertSame(attr, ref.getAttribute());
        assertEquals(attrPath, refAttr.getValue());
        
        node2.init();
        assertEquals(Status.CREATED, node2.getStatus());
        
        node.init();
        assertEquals(Status.INITIALIZED, node.getStatus());
        assertEquals(Status.STARTED, node2.getStatus());
        assertNull(refAttr.getValue());
        
        NodeListener listener = trainMocks_attributeReference(node2, refAttr);
        node2.addListener(listener);
        
        attr.setValue("1");
        assertEquals("1", refAttr.getValue());
        assertEquals(new Integer(1), refAttr.getRealValue());
        verify(listener);
        node2.removeListener(listener);
        store.saveNodeAttribute(attr);
        
        tree.reloadTree();
        
        node2 = (ContainerNode) tree.getNode(node2.getPath());
        assertNotNull(node2);
        assertEquals(Status.STARTED, node2.getStatus());
        refAttr = node2.getNodeAttribute("ref");
        assertNotNull(refAttr);
        assertEquals("1", refAttr.getValue());
    }
    
    @Test
    @Ignore
    public void attributeReferenceWithParameter() throws Exception
    {
        store.removeNodes();
        
        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addChildren(node);
        store.saveNode(node);
        node.init();
        NodeAttribute attr = new NodeAttributeImpl("attr", Integer.class, null, null);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        store.saveNodeAttribute(attr);
        
        NodeWithIntegerParameter node2 = new NodeWithIntegerParameter();
        node2.setName("node2");
        tree.getRootNode().addChildren(node2);
        store.saveNode(node2);
        node2.init();
        assertEquals(Status.INITIALIZED, node2.getStatus());
        
        NodeAttribute ref = node2.getNodeAttribute("parameter");
        ref.setType(AttributeReference.class);
        String attrPath = converter.convert(String.class, new AttributeReferenceImpl(attr), null);
        ref.setValue(attrPath);
        store.saveNodeAttribute(ref);
        
        assertNull(ref.getValue());
        assertNull(node2.getParameter());
        
        attr.setValue("1");
        store.saveNodeAttribute(attr);
        assertEquals("1", ref.getValue());
        assertEquals(new Integer(1), node2.getParameter());
        
        tree.reloadTree();
        
        node2 = (NodeWithIntegerParameter) tree.getNode(node2.getPath());
        assertNotNull(node2);
        assertEquals(Status.STARTED, node2.getStatus());
        ref = node2.getNodeAttribute("parameter");
        assertNotNull(ref);
        assertEquals(AttributeReference.class, ref.getType());
        assertEquals("1", ref.getValue());
        assertEquals(new Integer(1), node2.getParameter());
    }
    
    @Test
    public void attributeChangeName() throws Exception
    {
        store.removeNodes();
        tree.reloadTree();
        
        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addChildren(node);
        store.saveNode(node);
        node.init();
        
        NodeAttribute attr = new NodeAttributeImpl("attr", String.class, null, null);
        attr.setOwner(node);        
        node.addNodeAttribute(attr);
        store.saveNodeAttribute(attr);
        
        NodeAttributeListener attrListener = 
                createMock("AttributeListener", NodeAttributeListener.class);
        attrListener.nodeAttributeNameChanged(attr, "attr", "newName");
        NodeListener listener = createMock("NodeListener", NodeListener.class);
        expect(listener.isSubtreeListener()).andReturn(false).anyTimes();
        listener.nodeAttributeNameChanged(attr, "attr", "newName");
        replay(attrListener, listener);
        
        node.addListener(listener);
        node.addNodeAttributeDependency("attr", attrListener);
        
        attr.setName("newName");
        
        assertNull(node.getNodeAttribute("attr"));
        assertSame(attr, node.getNodeAttribute("newName"));
        verify(attrListener, listener);
    }
    
    @Test
    public void copy() throws ConstraintException, InvalidPathException, Exception
    {
        store.removeNodes();
        tree.reloadTree();
        
        Node node = new ContainerNode("node");
        tree.getRootNode().addChildren(node);
        store.saveNode(node);
        node.init();
        
        Node sysNode = tree.getRootNode().getChildren(SystemNode.NAME);
        NodeAttribute attr = new NodeAttributeImpl("attr", Integer.class, "1", null);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        attr.init();
        store.saveNodeAttribute(attr);
        
        Node child = new ContainerNode("child");
        node.addChildren(child);
        store.saveNode(child);
        child.init();
        
//        NodeAttribute attrRef = new NodeAttributeImpl("ref", AttributeReference.class, null, null);
//        attrRef.setOwner(child);
//        child.addNodeAttribute(attrRef);
//        attrRef.setValue(converter.convert(String.class, new AttributeReferenceImpl(attr), null));
//        store.saveNodeAttribute(attrRef);
//        
//        NodeAttribute nodeRef = new NodeAttributeImpl("nodeRef", Node.class, node.getPath(), null);
//        nodeRef.setOwner(child);
//        child.addNodeAttribute(nodeRef);
//        store.saveNodeAttribute(nodeRef);
        
        Node copyDest = new ContainerNode("copy");
        tree.getRootNode().addChildren(copyDest);
        store.saveNode(copyDest);
        copyDest.init();
        
        NodeListener listener = createMock(NodeListener.class);
        expect(listener.isSubtreeListener()).andReturn(true).anyTimes();
        listener.childrenAdded(eq(copyDest), matchNode("newName"));
        listener.childrenAdded(matchNode("newName"), matchNode("child"));
        listener.nodeStatusChanged((Node)anyObject(), (Status)anyObject(), (Status)anyObject());
        expectLastCall().anyTimes();
        
        replay(listener);
        
        copyDest.addListener(listener);
                
        tree.copy(node, copyDest, "newName", null, true, false, false);

        copyDest.removeListener(listener);
        verify(listener);
        
        checkNodeCopy(copyDest, sysNode, node, child, Status.INITIALIZED);
        
        tree.reloadTree();
        
        copyDest = tree.getNode(copyDest.getPath());
        assertNotNull(copyDest);
        checkNodeCopy(copyDest, sysNode, node, child, Status.STARTED);
    }
    
    @Test
    public void scanSubtree_allNodeTypes()
    {
        store.removeNodes();
        tree.reloadTree();
        
        Node parent1 = new BaseNode("parent1");
        tree.getRootNode().addChildren(parent1);
        parent1.save();
        parent1.init();
        
        Node parent2 = new BaseNode("parent2");
        parent1.addChildren(parent2);
        parent2.save();
        parent2.init();
        
        Node child = new ContainerNode("child");
        parent2.addChildren(child);
        child.save();
        child.init();
        child.start();
        assertEquals(Status.STARTED, child.getStatus());
        
        ScannedNodeHandler handler = createMock(ScannedNodeHandler.class);
        expect(handler.nodeScanned(parent2)).andReturn(ScanOperation.CONTINUE);
        expect(handler.nodeScanned(child)).andReturn(ScanOperation.CONTINUE);
        replay(handler);
        
        tree.scanSubtree(parent1, handler, null);
        
        verify(handler);
        
        handler = createMock(ScannedNodeHandler.class);
        expect(handler.nodeScanned(child)).andReturn(ScanOperation.CONTINUE);
        replay(handler);
        tree.scanSubtree(parent1, handler, null, Status.STARTED);
        
        verify(handler);
    }
    
    @Test
    public void scanSubtree_constrainedNodeTypes()
    {
        store.removeNodes();
        tree.reloadTree();
        
        Node parent1 = new BaseNode("parent1");
        tree.getRootNode().addChildren(parent1);
        parent1.save();
        parent1.init();
        
        Node parent2 = new BaseNode("parent2");
        parent1.addChildren(parent2);
        parent2.save();
        parent2.init();
        
        Node child = new ContainerNode("child");
        parent2.addChildren(child);
        child.save();
        child.init();
        
        ScannedNodeHandler handler = createMock(ScannedNodeHandler.class);
        expect(handler.nodeScanned(child)).andReturn(ScanOperation.CONTINUE);
        replay(handler);
        
        tree.scanSubtree(parent1, handler, new Class[]{ContainerNode.class});
        
        verify(handler);
        
        handler = createMock(ScannedNodeHandler.class);
        replay(handler);
        tree.scanSubtree(parent1, handler, new Class[]{ContainerNode.class}, Status.STARTED);
        verify(handler);
        
        handler = createMock(ScannedNodeHandler.class);
        expect(handler.nodeScanned(child)).andReturn(ScanOperation.CONTINUE);
        replay(handler);
        child.start();
        assertEquals(Status.STARTED, child.getStatus());
        tree.scanSubtree(parent1, handler, new Class[]{ContainerNode.class}, Status.STARTED);
        verify(handler);
    }
    
    @Test
    public void search()
    {
        store.removeNodes();
        tree.reloadTree();
        
        ContainerNode node = new ContainerNode("search-test-node");
        tree.getRootNode().addChildren(node);
        node.save();
        node.init();
        
        ContainerNode node1 = new ContainerNode("node");
        node.addChildren(node1);
        node1.save();
        node1.init();
        
        ContainerNode node2 = new ContainerNode("node");
        node1.addChildren(node2);
        node2.init();
        node2.save();
        
        ContainerNode node3 = new ContainerNode("node3");
        node1.addChildren(node3);
        node3.init();
        node3.save();
        
        List<Node> searchResult = tree.search(node, new SearchOptionsImpl(), new SearchFilter() {
            public boolean filter(Node node) {
                return false;
            }
        });
        assertNotNull(searchResult);
        assertTrue(searchResult.isEmpty());
        
        searchResult = tree.search(node, new SearchOptionsImpl(), new SearchFilter() {
            public boolean filter(Node node) {
                return node.getName().equals("node");
            }
        });
        assertNotNull(searchResult);
        assertEquals(2, searchResult.size());
        searchResult.contains(node1);
        searchResult.contains(node2);
        
        searchResult = tree.search(node, new SearchOptionsImpl().setFindFirst(true)
                , new SearchFilter() {
                    public boolean filter(Node node) {
                        return node.getName().equals("node");
                    }
                });
        assertNotNull(searchResult);
        assertEquals(1, searchResult.size());
        searchResult.contains(node1);
    }
    
    private static Node matchNode(final String nodeName)
    {
        reportMatcher(new IArgumentMatcher() {
            private Node node;

            public boolean matches(Object argument)
            {
                node = (Node) argument;
                return nodeName.equals(node.getName());
            }

            public void appendTo(StringBuffer buffer)
            {
                buffer.append(node.getName());
            }
        });
        
        return null;
    }
    
    private void checkNodeCopy(Node copyDest, Node sysNode, Node node, Node child, Status status)
    {
        Node nodeCopy = copyDest.getChildren("newName");
        assertNotNull(nodeCopy);
        assertFalse(nodeCopy.equals(node));
//        if (status==Status.STARTED)
//            nodeCopy.start();
        assertEquals(status, nodeCopy.getStatus());
        NodeAttribute attrCopy = nodeCopy.getNodeAttribute("attr");
        assertNotNull(attrCopy);
        assertEquals(1, attrCopy.getRealValue());
//        assertEquals(sysNode, attrCopy.getRealValue());
        
        Node childCopy = nodeCopy.getChildren("child");
        assertNotNull(childCopy);
        assertFalse(childCopy.equals(node));
        assertEquals(status, childCopy.getStatus());
        
//        String attrPath = 
//                converter.convert(String.class, new AttributeReferenceImpl(attrCopy), null);
//        NodeAttribute refCopy = childCopy.getNodeAttribute("ref");
//        assertNotNull(refCopy);
//        assertEquals(attrPath, refCopy.getRawValue());
//        
//        NodeAttribute nodeRefCopy = childCopy.getNodeAttribute("nodeRef");
//        assertNotNull(nodeRefCopy);
//        assertEquals(nodeCopy.getPath(), nodeRefCopy.getValue());
    }
    
    private NodeListener trainMocks_attributeReference(Node node, NodeAttribute attr)
    {
        NodeListener listener = createMock(NodeListener.class);
        expect(listener.isSubtreeListener()).andReturn(false).anyTimes();
        listener.nodeAttributeValueChanged(eq(node), eq(attr), (String)isNull(), eq("1"));
        
        replay(listener);
        
        return listener;
    }
    
    private void checkAttributes(NodeWithParameters node, String value)
    {
        assertNotNull(node.getNodeAttributes());
        assertEquals(2, node.getNodeAttributes().size());

        NodeAttribute stringAttr = node.getNodeAttribute("stringParameter");
        assertNotNull(stringAttr);
        assertEquals(String.class, stringAttr.getType());
        assertEquals("stringParameter", stringAttr.getParameterName());
        assertNull(stringAttr.getParentAttribute());
        assertEquals("This is a string parameter", stringAttr.getDescription());
        assertEquals(stringAttr.getOwner(), node);
        assertEquals(value, stringAttr.getValue());
        
        assertEquals(value, node.getStringParameter());
    }

    private void checkTree() throws NodeNotFoundError, InvalidPathException
    {
        if (!checkTreeExecuted)
        {
            checkTreeExecuted = true;
            store.removeNodes();
        }
        tree.reloadTree();
        
        assertNotNull(tree.getRootNode());

        Node systemNode = tree.getNode(Node.NODE_SEPARATOR+SystemNode.NAME);
        assertNotNull(systemNode);
        
        DataSourcesNode dataSourcesNode = 
                (DataSourcesNode) systemNode.getChildren(DataSourcesNode.NAME);
        assertNotNull(dataSourcesNode);

        ConnectionPoolsNode connectionPoolsNode = 
                (ConnectionPoolsNode) systemNode.getChildren(ConnectionPoolsNode.NAME);
        assertNotNull(connectionPoolsNode);
        
        TemplatesNode templatesNode = 
                (TemplatesNode) tree.getRootNode().getChildren(TemplatesNode.NAME);
        assertNotNull(templatesNode);
    }
}

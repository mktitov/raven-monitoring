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

import groovy.lang.Closure;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import static org.easymock.EasyMock.*;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class BaseNodeTest extends RavenCoreTestCase
{
    @Test
    public void setNameTest()
    {
        BaseNode parent = new BaseNode();
        parent.setName("parent");
        tree.getRootNode().addAndSaveChildren(parent);
        
        BaseNode node = new BaseNode();
        node.setName("node");
        parent.addAndSaveChildren(node);
        node.setName("new name");

        assertSame(node, parent.getNode("new name"));
    }

    @Test(expected=NodeError.class)
    public void setNameTest2()
    {
        BaseNode parent = new BaseNode();
        parent.setName("parent");
        tree.getRootNode().addAndSaveChildren(parent);

        BaseNode node = new BaseNode();
        node.setName("node");
        parent.addAndSaveChildren(node);

        BaseNode node2 = new BaseNode();
        node2.setName("new name");
        parent.addAndSaveChildren(node2);

        node.setName("new name");
    }

    @Test
    public void cloneTest() throws CloneNotSupportedException
    {
        BaseNode parent = new BaseNode();
        parent.setName("parent");
        
        BaseNode node = new BaseNode();
        node.setName("node");
        node.setId(1);
        node.setParent(parent);
        
        NodeAttribute attr = new NodeAttributeImpl();
        attr.setName("attr");
        attr.setType(String.class);
        attr.setOwner(node);
        node.addAttr(attr);
        
        //
        Node nodeClone = (Node) node.clone();
        //
        
        assertNotNull(nodeClone);
        assertNotSame(node, nodeClone);
        assertEquals(0, nodeClone.getId());
        assertNull(nodeClone.getParent());
        assertEquals("node", nodeClone.getName());
        
        NodeAttribute attrClone = nodeClone.getAttr("attr");
        assertNotNull(attrClone);
        assertNotSame(attr, attrClone);
        assertEquals(0, attrClone.getId());
        assertEquals(String.class, attrClone.getType());
    }
    
    @Test
    public void getEffectiveChildrens()
    {
        BaseNode node = new BaseNode();
        assertTrue(node.getEffectiveNodes().isEmpty());
        
        Node child1 = createMock("child1", Node.class);
        Node child3 = createMock("child3", Node.class);
        Node condChild = createMock("condChild", Node.class);
        
        child1.setParent(node);
        child1.addListener(node);
        expect(child1.getName()).andReturn("child1").times(2);
        expect(child1.compareTo(child3)).andReturn(-1).anyTimes();
        expect(child1.compareTo(condChild)).andReturn(-1).anyTimes();
        expect(child1.getIndex()).andReturn(1).anyTimes();
        expect(child1.isConditionalNode()).andReturn(false).atLeastOnce();
        
        Node condChildChild = createMock("condChildChild", Node.class);
        
        condChild.setParent(node);
        condChild.addListener(node);
        expect(condChild.getName()).andReturn("condChild").times(2);
        expect(condChild.isConditionalNode()).andReturn(true).atLeastOnce();
        expect(condChild.getIndex()).andReturn(3);
        expect(condChild.compareTo(child1)).andReturn(1).anyTimes();
        expect(condChild.compareTo(child3)).andReturn(-1).anyTimes();
//        expect(condChild.getEffectiveNodes()).andReturn(Arrays.asList(condChildChild));
        expect(condChild.getEffectiveChildrens()).andReturn(Arrays.asList(condChildChild));
        
        child3.setParent(node);
        child3.addListener(node);
        expect(child3.getName()).andReturn("child3").times(2);
        expect(child3.getIndex()).andReturn(2).anyTimes();
        expect(child3.compareTo(condChild)).andReturn(1).anyTimes();
        expect(child3.compareTo(child1)).andReturn(1).anyTimes();
        expect(child3.isConditionalNode()).andReturn(false).atLeastOnce();
        
        replay(child1, condChild, condChildChild, child3);
        
        node.addChildren(child3);
        node.addChildren(child1);
        node.addChildren(condChild);
        
        Collection childs = node.getEffectiveNodes();
        assertNotNull(childs);
        assertEquals(3, childs.size());
        
        Iterator<Node> it = childs.iterator();
        assertSame(child1, it.next());
        assertSame(condChildChild, it.next());
        assertSame(child3, it.next());
        
        verify(child1, condChild, condChildChild, child3);
    }

    @Test
    public void dynamicTest()
    {
        BaseNode root = new BaseNode();
        assertFalse(root.isDynamic());
        assertFalse(root.isChildrensDynamic());

        BaseNode child = new BaseNode();
        child.setParent(root);
        assertFalse(child.isDynamic());
        assertFalse(child.isChildrensDynamic());

        root.setChildrensDynamic(true);
        assertTrue(child.isDynamic());
    }

    @Test
    public void requiredAttributesTest() throws Exception
    {
        BaseNode node = new BaseNode("node");
        node.init();
        assertTrue(node.start());

        node.stop();

        NodeAttributeImpl attr = new NodeAttributeImpl("attr1", String.class, null, null);
        attr.setRequired(true);
        attr.setOwner(node);
        attr.init();
        node.addAttr(attr);

        assertFalse(node.start());

        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        assertTrue(node.start());

        node.stop();
        try{
            attr.setValue("1+.sd");
        }catch(Exception e){}
        assertFalse(node.start());

        attr.setValue("1+1");
        assertTrue(node.start());
    }
    
    @Test
    public void getChildByPathTest() throws Exception {
        BaseNode node1 = new BaseNode("test");
        tree.getRootNode().addAndSaveChildren(node1);
        
        BaseNode node2 = new BaseNode("test2");
        node1.addAndSaveChildren(node2);
        
        BaseNode node3 = new BaseNode("test3");
        node2.addAndSaveChildren(node3);
        
        assertSame(node2, node1.getNodeByPath("test2"));
        assertSame(node3, node1.getNodeByPath("test2/test3"));
    }
    
    @Test
    public void getVariablesTest() {
        BaseNode node = new BaseNode("test");
        tree.getRootNode().addAndSaveChildren(node);
        assertNotNull(node.getVariables());
        assertTrue(node.getVariables() instanceof Map);
    }
    
    @Test
    public void findTest() {
        BaseNode node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        ContainerNode container = new ContainerNode("container");
        tree.getRootNode().addAndSaveChildren(container);
        BaseNode child = new BaseNode("node");
        container.addAndSaveChildren(child);
        BaseNode child1 = new BaseNode("node1");
        container.addAndSaveChildren(child1);
        
        Node res = tree.getRootNode().find(new Closure<Boolean>(this){
            @Override public Boolean call(Object arg) {
                return ((Node)arg).getName().equals("node");
            }            
        });
        assertNotNull(res);
        assertSame(node, res);
        
        res = tree.getRootNode().find(new Closure<Boolean>(this){
            @Override public Boolean call(Object arg) {
                return ((Node)arg).getName().equals("node1");
            }            
        });
        assertNotNull(res);
        assertSame(child1, res);
        
        List<Node> nodes = tree.getRootNode().findAll(new Closure<Boolean>(this){
            @Override public Boolean call(Object arg) {
                return ((Node)arg).getName().equals("node");
            }            
        });
        assertNotNull(nodes);
        assertEquals(2, nodes.size());
        assertArrayEquals(new Object[]{node, child}, nodes.toArray());
    }
    
    @Test
    public void groovyAttrAccessTest() throws Exception {
        BaseNode node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        NodeAttribute attr1 = new NodeAttributeImpl("attr1", Integer.class, 1, null);
        attr1.setOwner(node);
        attr1.init();
        node.addAttr(attr1);
        NodeAttribute attr2 = new NodeAttributeImpl("attr2", String.class, "param1", null);
        attr2.setOwner(node);
        attr2.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr2.init();
        node.addAttr(attr2);
        
        assertEquals(1, node.propertyMissing("$attr1"));
        assertEquals(2, node.propertyMissing("$attr1", 2));
        assertEquals(2, attr1.getRealValue());
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("param1", 50);
        assertEquals(50, node.methodMissing("$attr2", new Object[]{args}));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void groovyAttrNotFoundTest() throws Exception {
        BaseNode node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        node.propertyMissing("$attr1");
    }
    
    @Test()
    public void groovyAttrNotFoundTest1() throws Exception {
        BaseNode node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertNull(node.propertyMissing("$$attr1"));
    }
    
    @Test(expected=MissingPropertyException.class)
    public void groovyPropertyNotFoundTest() throws Exception {
        BaseNode node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        node.propertyMissing("attr1");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void groovyAttrNotFoundTest2() throws Exception {
        BaseNode node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        node.methodMissing("$attr1", new Object[]{new HashMap()});
    }
    
    @Test()
    public void groovyAttrNotFoundTest3() throws Exception {
        BaseNode node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertNull(node.methodMissing("$$attr1", new Object[]{new HashMap()}));
    }
    
    @Test(expected=MissingMethodException.class)
    public void groovyPropertyNotFoundTest2() throws Exception {
        BaseNode node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        node.methodMissing("attr1", new Object[]{new HashMap()});
    }
    
    @Test
    public void addUniqAttrTest() throws Exception {
        BaseNode node2 = new BaseNode("node2");
        testsNode.addAndSaveChildren(node2);
        BaseNode node3 = new BaseNode("node3");
        testsNode.addAndSaveChildren(node3);
        BaseNode node = new BaseNode("node");
        testsNode.addAndSaveChildren(node);
        NodeAttribute attr = createAttr(node, "attr", Node.class, NodeReferenceValueHandlerFactory.TYPE, testsNode);
        
        assertSame(testsNode, attr.getRealValue());
        assertSame(attr, node.addUniqAttr("attr", testsNode));
        
        NodeAttribute attr1 = node.addUniqAttr("attr", node2);
        assertNotSame(attr, attr1);
        assertEquals("attr1", attr1.getName());
        assertEquals(node2, attr1.getRealValue());
        
        assertSame(attr1, node.addUniqAttr("attr", node2));
        
        NodeAttribute attr2 = node.addUniqAttr("attr", node3);
        assertEquals("attr2", attr2.getName());
        assertEquals(node3, attr2.getRealValue());
        
        attr.setValue(null);
        assertNull(attr.getRealValue());
        assertSame(attr, node.addUniqAttr("attr", testsNode, true));
        assertSame(testsNode, attr.getRealValue());
    }
    
    @Test
    public void addDependentNodeTest() {
        BaseNode node1 = createNode("node1");
        BaseNode node2 = createNode("node2");
        BaseNode node3 = createNode("node3");
        
        assertTrue(node1.getDependentNodes().isEmpty());
        assertTrue(node1.addDependentNode(node2, "owner1"));
        checkDependencies(node1, 1, new Node[]{node2});        
        assertFalse(node1.addDependentNode(node2, "owner1"));
        
        assertTrue(node1.addDependentNode(node2, "owner2"));
        checkDependencies(node1, 1, new Node[]{node2});
        
        assertTrue(node1.removeDependentNode(node2, "owner1"));
        checkDependencies(node1, 1, new Node[]{node2});
        
        assertTrue(node1.addDependentNode(node2, "owner1"));
        checkDependencies(node1, 1, new Node[]{node2});
        
        assertTrue(node1.addDependentNode(node3, node3));
        checkDependencies(node1, 2, new Node[]{node2, node3});
        
        
        assertTrue(node1.removeDependentNode(node2, "owner1"));
        checkDependencies(node1, 2, new Node[]{node2, node3});
        assertFalse(node1.removeDependentNode(node2, "owner1"));
        checkDependencies(node1, 2, new Node[]{node2, node3});
        assertTrue(node1.removeDependentNode(node2, "owner2"));
        checkDependencies(node1, 1, new Node[]{node3});
        assertFalse(node1.removeDependentNode(node2, "owner2"));
        checkDependencies(node1, 1, new Node[]{node3});
        assertTrue(node1.removeDependentNode(node3, node3));
        assertTrue(node1.getDependentNodes().isEmpty());
    }
    
    private void checkDependencies(Node node, int size, Node[] dependentNodes) {
        assertEquals(size, node.getDependentNodes().size());
        for (Node dependentNode: dependentNodes)
            assertTrue(node.getDependentNodes().contains(dependentNode));
    }
    
    private BaseNode createNode(String name) {
        BaseNode node = new BaseNode(name);
        testsNode.addAndSaveChildren(node);
        assertTrue(node.start());
        return node;
    }
    
    private NodeAttribute createAttr(Node owner, String name, Class type, String valueHandlerType, Object val) throws Exception {
        NodeAttribute attr = new NodeAttributeImpl(name, type, null, null);        
        attr.setOwner(owner);
        attr.init();
        owner.addAttr(attr);
        if (valueHandlerType!=null)
            attr.setValueHandlerType(valueHandlerType);
        attr.setValue(getConverter().convert(String.class, val, null));
        return attr;
    }
    
    private TypeConverter getConverter() {
        return registry.getService(TypeConverter.class);
    }
}

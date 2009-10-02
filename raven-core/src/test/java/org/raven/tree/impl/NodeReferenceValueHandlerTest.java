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

import org.apache.commons.lang.text.StrTokenizer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.AttributeValueHandlerListener;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import static org.easymock.EasyMock.*;
import org.raven.tree.NodeError;
/**
 *
 * @author Mikhail Titov
 */
public class NodeReferenceValueHandlerTest extends RavenCoreTestCase
{
    private Node node;
    private NodeAttribute refAttr;
    private Node parentNode;
    private Node childNode;
    
    @Before
    public void setupTest()
    {
        parentNode = new BaseNode("parent");
        tree.getRootNode().addChildren(parentNode);
        parentNode.save();
        parentNode.init();
        
        addChildNode();
        
        node = new BaseNode("node");
        tree.getRootNode().addChildren(node);
        node.save();
        node.init();
    }
    
    @Test
    @Ignore
    public void strTokenizerTest()
    {
        StrTokenizer tokenizer = new StrTokenizer("/test/\"test\"/\"te/st\"@attr", '/', '"');
        String[] result = tokenizer.getTokenArray();
        for (String token: result)
            System.out.println("("+token+")");
        assertArrayEquals(new String[]{"test", "test", "te/st"}, result);
    }
    
    @Test
    public void setData_dependentReference() throws Exception
    {
        check_setData(true);
    }
    
    @Test
    public void setData_nonDependentReference() throws Exception
    {
        check_setData(false);
    }
    
    @Test 
    public void cleanup_dependentReference() throws Exception
    {
        check_cleanup(true);
    }
    
    @Test 
    public void cleanup_nonDependentReference() throws Exception
    {
        check_cleanup(false);
    }
    
    @Test
    public void changeNodeNameInPath() throws Exception
    {
        NodeAttribute attr = createMock(NodeAttribute.class);
        expect(attr.getOwner()).andReturn(node).anyTimes();
        expect(attr.getRawValue()).andReturn(null);
        attr.save();
        attr.save();
        attr.save();
        replay(attr);
        
        NodeReferenceValueHandler valueHandler = new NodeReferenceValueHandler(attr);
        valueHandler.setData(childNode.getPath());
        
        parentNode.setName("newParentName");
        assertEquals(childNode.getPath(), valueHandler.getData());
        assertSame(childNode, valueHandler.handleData());
        childNode.setName("newChildName");
        assertEquals(childNode.getPath(), valueHandler.getData());
        verify(attr);
    }
    
    @Test
    public void removeNodeInPath() throws Exception
    {
        NodeAttribute attr = createMock(NodeAttribute.class);
        AttributeValueHandlerListener listener = createMock(AttributeValueHandlerListener.class);
        expect(attr.getOwner()).andReturn(node).anyTimes();
        expect(attr.getRawValue()).andReturn(null);
        expect(attr.getName()).andReturn("attr");
        expect(attr.isRequired()).andReturn(false);
        attr.save();
        attr.save();
        listener.valueChanged(isNull(), eq(childNode));
        listener.expressionInvalidated(childNode);
        listener.valueChanged(isNull(), isA(Node.class));
        replay(attr, listener);
        
        NodeReferenceValueHandler valueHandler = new NodeReferenceValueHandler(attr);
        valueHandler.addListener(listener);
        valueHandler.setData(childNode.getPath());
        String data = valueHandler.getData();
        assertNotNull(data);
        
        tree.remove(childNode);
        assertFalse(valueHandler.isExpressionValid());
        assertEquals(data, valueHandler.getData());
        assertNull(valueHandler.handleData());
        
        addChildNode();
        valueHandler.validateExpression();
        assertTrue(valueHandler.isExpressionValid());
        assertEquals(data, valueHandler.getData());
        assertEquals(childNode, valueHandler.handleData());
    }

    @Test
    public void ownerStop() throws Exception
    {
        NodeAttribute attr = createMock(NodeAttribute.class);
        AttributeValueHandlerListener listener = createMock(AttributeValueHandlerListener.class);
        expect(attr.getOwner()).andReturn(node).anyTimes();
        expect(attr.getRawValue()).andReturn(null);
        expect(attr.getName()).andReturn("attr");
        expect(attr.isRequired()).andReturn(true);
        attr.save();
        attr.save();
        listener.valueChanged(isNull(), eq(childNode));
        listener.expressionInvalidated(childNode);
        listener.valueChanged(isNull(), isA(Node.class));
        replay(attr, listener);

        NodeReferenceValueHandler valueHandler = new NodeReferenceValueHandler(attr);
        valueHandler.addListener(listener);
        valueHandler.setData(childNode.getPath());
        String data = valueHandler.getData();
        assertNotNull(data);

        assertTrue(node.start());
        tree.remove(childNode);
        assertFalse(valueHandler.isExpressionValid());
        assertEquals(data, valueHandler.getData());
        assertNull(valueHandler.handleData());
        assertEquals(Status.INITIALIZED, node.getStatus());

        addChildNode();
        valueHandler.validateExpression();
        assertTrue(valueHandler.isExpressionValid());
        assertEquals(data, valueHandler.getData());
        assertEquals(childNode, valueHandler.handleData());
    }

    @Test
    public void referenceMovedTest1() throws Exception
    {
        NodeAttribute attr = new NodeAttributeImpl("ref", Node.class, childNode.getPath(), null);
        attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        attr.save();
        attr.init();

        Node newHome = new BaseNode("newHome");
        tree.getRootNode().addAndSaveChildren(newHome);
        tree.move(childNode, newHome, null);

        assertEquals(newHome, childNode.getParent());
        String newPath = "/\"newHome\"/\"child\"/";
        assertEquals(newPath, childNode.getPath());
        assertEquals(newPath, attr.getValue());

        tree.reloadTree();

        node = tree.getNode(node.getPath());
        assertNotNull(node);
        attr = node.getNodeAttribute("ref");
        assertNotNull(attr);
        assertEquals(newPath, attr.getValue());
        assertEquals(childNode, attr.getRealValue());
    }
    
    @Test
    public void referenceMovedTest2() throws Exception
    {
        NodeAttribute attr = new NodeAttributeImpl("ref", Node.class, childNode.getPath(), null);
        attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        attr.save();
        attr.init();

        Node newHome = new BaseNode("newHome");
        tree.getRootNode().addAndSaveChildren(newHome);
        tree.move(parentNode, newHome, null);

        assertEquals(parentNode, childNode.getParent());
        assertEquals(newHome, parentNode.getParent());
        String newPath = "/\"newHome\"/\"parent\"/\"child\"/";
        assertEquals(newPath, childNode.getPath());
        assertEquals(newPath, attr.getValue());

        tree.reloadTree();

        node = tree.getNode(node.getPath());
        assertNotNull(node);
        attr = node.getNodeAttribute("ref");
        assertNotNull(attr);
        assertEquals(newPath, attr.getValue());
        assertEquals(childNode, attr.getRealValue());
    }

    @Test
    public void selfMovedTest() throws Exception
    {
        NodeAttribute attr = new NodeAttributeImpl("ref", Node.class, "../parent/child", null);
        attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        attr.save();
        attr.init();

        assertEquals(childNode, attr.getRealValue());

        Node newHome = new BaseNode("newHome");
        tree.getRootNode().addAndSaveChildren(newHome);
        tree.move(node, newHome, null);

        assertEquals(newHome, node.getParent());
        String newPath = "../../\"parent\"/\"child\"/";
        assertEquals(newPath, attr.getRawValue());

        tree.reloadTree();

        node = tree.getNode(node.getPath());
        assertNotNull(node);
        attr = node.getNodeAttribute("ref");
        assertNotNull(attr);
        assertEquals(newPath, attr.getRawValue());
        assertEquals(childNode, attr.getRealValue());
    }

    @Test
    public void createWithReferenceToInvalidNode() throws Exception
    {
        NodeAttribute attr = new NodeAttributeImpl("ref", Node.class, "../parent/child2", null);
        attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        attr.save();
        attr.init();

        assertFalse(attr.isExpressionValid());
        assertNull(attr.getRealValue());
        assertNull(attr.getValue());

        Node child2 = new BaseNode("child2");
        parentNode.addAndSaveChildren(child2);
        assertTrue(child2.start());
        
        assertNotNull(attr.getValue());
        assertTrue(attr.isExpressionValid());
        assertNotNull(attr.getRealValue());
    }

    @Test
    public void realTest() throws Exception
    {
        NodeAttribute attr = new NodeAttributeImpl("ref", Node.class, childNode.getPath(), null);
        attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        attr.save();
        attr.init();
        
        assertTrue(attr.isExpression());
        assertTrue(attr.isExpressionValid());
        assertEquals(childNode, attr.getRealValue());
        assertEquals(childNode.getPath(), attr.getValue());
        
        tree.reloadTree();
        
        node = tree.getNode(node.getPath());
        assertNotNull(node);
        attr = node.getNodeAttribute("ref");
        assertNotNull(attr);
        assertEquals(childNode, attr.getRealValue());
        
    }

    private void addChildNode() throws NodeError
    {
        childNode = new BaseNode("child");
        parentNode.addChildren(childNode);
        childNode.save();
        childNode.init();
    }
    
    private void check_setData(boolean addDependencyFlag) throws Exception
    {
        NodeAttribute attr = createMock(NodeAttribute.class);
        AttributeValueHandlerListener listener = createMock(AttributeValueHandlerListener.class);
        expect(attr.getOwner()).andReturn(node).anyTimes();
        expect(attr.getRawValue()).andReturn(null);
        attr.save();
        listener.valueChanged(isNull(), eq(childNode));
        replay(attr, listener);
        
        NodeReferenceValueHandler valueHandler = addDependencyFlag?
                  new NodeReferenceValueHandler(attr) 
                : new NodeReferenceValueHandler(attr, false, false);
        valueHandler.addListener(listener);
        valueHandler.setData(childNode.getPath());
        assertEquals(childNode.getPath(), valueHandler.getData());
        assertSame(childNode, valueHandler.handleData());
        if (addDependencyFlag)
        {
            assertNotNull(childNode.getDependentNodes());
            assertTrue(childNode.getDependentNodes().contains(node));
        } else
        {
            assertTrue(childNode.getDependentNodes().isEmpty());
        }
        assertTrue(childNode.getListeners().contains(valueHandler));
        assertTrue(parentNode.getListeners().contains(valueHandler));
        
        verify(attr, listener);
        
    }
    
    private void check_cleanup(boolean addDependencyFlag) throws Exception
    {
        NodeAttribute attr = createMock(NodeAttribute.class);
        AttributeValueHandlerListener listener = createMock(AttributeValueHandlerListener.class);
        expect(attr.getOwner()).andReturn(node).anyTimes();
        expect(attr.getRawValue()).andReturn(null);
        attr.save();
        expectLastCall().times(2);
        listener.valueChanged(isNull(), eq(childNode));
        listener.valueChanged(eq(childNode), isNull());
        replay(attr, listener);
        
        NodeReferenceValueHandler valueHandler = addDependencyFlag?
                  new NodeReferenceValueHandler(attr) 
                : new NodeReferenceValueHandler(attr, false, false);
        valueHandler.addListener(listener);
        valueHandler.setData(childNode.getPath());
        valueHandler.setData(null);
        
        assertNull(valueHandler.getData());
        assertNull(valueHandler.handleData());
        if (addDependencyFlag)
            assertTrue(childNode.getDependentNodes().isEmpty());
        else
            assertTrue(childNode.getDependentNodes().isEmpty());
        assertFalse(childNode.getListeners().contains(valueHandler));
        assertFalse(parentNode.getListeners().contains(valueHandler));
        
        verify(attr, listener);
    }
}

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

import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.AttributeValueHandlerListener;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodePathResolver;
/**
 *
 * @author Mikhail Titov
 */
public class AttributeReferenceValueHandlerTest extends RavenCoreTestCase
{
    private Node node;
    private NodeAttribute attr;
    private Node parentNode;
    private Node childNode;
    private NodePathResolver pathResolver;
    
    @Before
    public void setupTest() throws Exception {
        pathResolver = registry.getService(NodePathResolver.class);
        assertNotNull(pathResolver);
        parentNode = new BaseNode("parent");
        tree.getRootNode().addChildren(parentNode);
        parentNode.save();
        parentNode.init();
        
        childNode = new BaseNode("child");
        parentNode.addChildren(childNode);
        childNode.save();
        childNode.init();
        addAttribute();
//        NodeAttribute refAttr = new NodeAttributeImpl("ref", Integer.class, null, null);
//        refAttr.setValueHandlerType(AttributeReferenceValueHandlerFactory.TYPE);
        
        node = new BaseNode("node");
        tree.getRootNode().addChildren(node);
        node.save();
        node.init();
    }
    
//    @Test(expected=InvalidPathException.class)
    @Test
    public void setData_noAttributeSeparatorInPath() throws Exception {
        NodeAttribute attr = createMock(NodeAttribute.class);
        expect(attr.getRawValue()).andReturn(null);
        expect(attr.getOwner()).andReturn(node).anyTimes();
        replay(attr);
        
        AttributeReferenceValueHandler valueHandler = new AttributeReferenceValueHandler(attr);
        valueHandler.setData(childNode.getPath());
        
        verify(attr);
    }
    
//    @Test(expected=AttributeNotFoundException.class)
    @Test()
    public void setData_attributeNotFound1() throws Exception {
        NodeAttribute attr = createMock(NodeAttribute.class);
        expect(attr.getOwner()).andReturn(node).anyTimes();
        expect(attr.getRawValue()).andReturn(null).atLeastOnce();
        attr.save();
        replay(attr);
        
        AttributeReferenceValueHandler valueHandler = new AttributeReferenceValueHandler(attr);
        valueHandler.setData(childNode.getPath()+Node.ATTRIBUTE_SEPARATOR+"xxx");
        
        verify(attr);
    }
    
//    @Test(expected=AttributeNotFoundException.class)
    @Test()
    public void setData_attributeNotFound2() throws Exception {
        NodeAttribute attr = createMock(NodeAttribute.class);
        expect(attr.getOwner()).andReturn(node).anyTimes();
        expect(attr.getRawValue()).andReturn(null);
        attr.save();
        replay(attr);
        
        AttributeReferenceValueHandler valueHandler = new AttributeReferenceValueHandler(attr);
        valueHandler.setData(childNode.getPath()+Node.ATTRIBUTE_SEPARATOR);
        
        verify(attr);
    }
    
    @Test
    public void setData() throws Exception {
        NodeAttribute refAttr = createMock(NodeAttribute.class);
        AttributeValueHandlerListener listener = createMock(AttributeValueHandlerListener.class);
        
        expect(refAttr.getOwner()).andReturn(node);
        expect(refAttr.getRawValue()).andReturn(null);
        expect(refAttr.getType()).andReturn(Integer.class).times(2);
        refAttr.save();
//        listener.valueChanged(null, 10);
//        listener.valueChanged(10, 99);
        replay(refAttr, listener);
        
        AttributeReferenceValueHandler valueHandler = new AttributeReferenceValueHandler(refAttr);
        valueHandler.addListener(listener);
        valueHandler.setData(childNode.getPath()+Node.ATTRIBUTE_SEPARATOR+attr.getName());
        assertEquals(new Integer(10), valueHandler.handleData());
        
        attr.setValue("99");
        assertEquals(new Integer(99), valueHandler.handleData());
        
        verify(refAttr, listener);
    }
    
    @Test
    public void attributeRemoved() throws Exception
    {
        NodeAttribute refAttr = createMock(NodeAttribute.class);
        AttributeValueHandlerListener listener = createMock(AttributeValueHandlerListener.class);
        
        expect(refAttr.getOwner()).andReturn(node).atLeastOnce();
        expect(refAttr.getRawValue()).andReturn(null).atLeastOnce();
        expect(refAttr.getType()).andReturn(Integer.class).atLeastOnce();
        refAttr.save();
//        listener.valueChanged(null, 10);
        listener.expressionInvalidated(anyObject());
//        listener.valueChanged(null, 10);
        replay(refAttr, listener);
        
        AttributeReferenceValueHandler valueHandler = new AttributeReferenceValueHandler(refAttr);
        valueHandler.addListener(listener);
        valueHandler.setData(childNode.getPath()+Node.ATTRIBUTE_SEPARATOR+attr.getName());
        
        assertEquals(10, valueHandler.handleData());
        assertTrue(childNode.getListeners().contains(valueHandler));
        childNode.removeAttr("attr");
        
        assertFalse(childNode.getListeners().contains(valueHandler));
        assertFalse(valueHandler.expressionValid);
        assertNull(valueHandler.handleData());
        assertEquals(
                childNode.getPath()+Node.ATTRIBUTE_SEPARATOR+attr.getName()
                , valueHandler.getData());
        
        addAttribute();
        valueHandler.validateExpression();
        assertTrue(valueHandler.isExpressionValid());
        assertEquals(10, valueHandler.handleData());
        
        verify(refAttr, listener);
    }
    
    @Test
    public void attributeRenamed() throws Exception {
        NodeAttribute refAttr = createMock(NodeAttribute.class);
        AttributeValueHandlerListener listener = createMock(AttributeValueHandlerListener.class);
        
        expect(refAttr.getOwner()).andReturn(node).atLeastOnce();
        expect(refAttr.getRawValue()).andReturn(null).atLeastOnce();
        expect(refAttr.getType()).andReturn(Integer.class).atLeastOnce();
        refAttr.save();
        refAttr.save();
        listener.valueChanged(null, 10);
        listener.valueChanged(10, 20);
        replay(refAttr, listener);
        
        AttributeReferenceValueHandler valueHandler = new AttributeReferenceValueHandler(refAttr);
        valueHandler.addListener(listener);
        valueHandler.setData(childNode.getPath()+Node.ATTRIBUTE_SEPARATOR+attr.getName());
        
        assertEquals(10, valueHandler.handleData());
        attr.setName("newName");
        attr.save();
        
        assertTrue(valueHandler.getData().endsWith("newName"));
        assertEquals(10, valueHandler.handleData());
        attr.setValue("20");
        assertEquals(20, valueHandler.handleData());
    }

    @Test
    public void moveTest() throws Exception {
        NodeAttribute refAttr = createRefAttr();

        BaseNode newHome = new BaseNode("newHome");
        tree.getRootNode().addAndSaveChildren(newHome);

        tree.move(childNode, newHome, null);

        assertEquals(10, refAttr.getRealValue());
        String newPath = "/\"newHome\"/\"child\"@attr";
        assertEquals(newPath, refAttr.getRawValue());

        tree.reloadTree();

        node = tree.getNode(node.getPath());
        refAttr = node.getAttr("refAttr");
        assertEquals(newPath, refAttr.getRawValue());
        assertEquals(10, refAttr.getRealValue());
    }
    
    //moving node with refAttr
    @Test
    public void moveTest2() throws Exception {
        NodeAttribute refAttr = createRefAttr();

        BaseNode newHome = new BaseNode("newHome");
        tree.getRootNode().addAndSaveChildren(newHome);

        tree.move(node, newHome, null);

        assertEquals(10, refAttr.getRealValue());
        String path = childNode.getPath()+Node.ATTRIBUTE_SEPARATOR+attr.getName();
        assertEquals(path, refAttr.getRawValue());

        tree.reloadTree();

        node = tree.getNode(node.getPath());
        refAttr = node.getAttr("refAttr");
        assertEquals(path, refAttr.getRawValue());
        assertEquals(10, refAttr.getRealValue());
    }

    //moving childNode (relativePath)
    @Test
    public void moveTest3() throws Exception {
        NodeAttribute refAttr = createRefAttr();
        refAttr.setValue(pathResolver.getRelativePath(node, attr));
        refAttr.save();
        assertEquals(10, refAttr.getRealValue());

        BaseNode newHome = new BaseNode("newHome");
        tree.getRootNode().addAndSaveChildren(newHome);

        tree.move(childNode, newHome, null);

        assertEquals(10, refAttr.getRealValue());
        String newPath = pathResolver.getRelativePath(node, attr);
        assertEquals(newPath, refAttr.getRawValue());

        tree.reloadTree();

        node = tree.getNode(node.getPath());
        refAttr = node.getAttr("refAttr");
        assertEquals(newPath, refAttr.getRawValue());
        assertEquals(10, refAttr.getRealValue());
    }
    
    @Test
    public void ownerNodeRenameTest() throws Exception {
        NodeAttribute refAttr = createRefAttr();
        node.setName("newNodeName");
        node.save();
        assertEquals(10, refAttr.getRealValue());
        tree.reloadTree();
        
        node = tree.getNode(node.getPath());
        refAttr = node.getAttr("refAttr");
        assertEquals(10, refAttr.getRealValue());
    }
    
    @Test
    public void referencedNodeRenameTest() throws Exception {
        NodeAttribute refAttr = createRefAttr();
        assertEquals(10, refAttr.getRealValue());
        childNode.setName("newChildNodeName");
        assertEquals(10, refAttr.getRealValue());
        
        tree.reloadTree();
        node = tree.getNode(node.getPath());
        refAttr = node.getAttr("refAttr");
        assertEquals(10, refAttr.getRealValue());
    }

    private NodeAttribute createRefAttr() throws Exception {
        NodeAttribute refAttr = new NodeAttributeImpl("refAttr", Integer.class, null, null);
        refAttr.setValueHandlerType(AttributeReferenceValueHandlerFactory.TYPE);
        refAttr.setOwner(node);
        node.addAttr(refAttr);
        refAttr.init();
        refAttr.setValue(childNode.getPath()+Node.ATTRIBUTE_SEPARATOR+attr.getName());
        refAttr.save();
        return refAttr;
    }
    
    @Test
    public void realTest() throws Exception {
        NodeAttribute refAttr = createRefAttr();
        
        assertEquals("10", refAttr.getValue());
        assertEquals(10, refAttr.getRealValue());
        
        tree.reloadTree();
        
        node = tree.getNode(node.getPath());
        assertNotNull(node);
        refAttr = node.getAttr("refAttr");
        assertNotNull(refAttr);
        assertEquals("10", refAttr.getValue());
        assertEquals(10, refAttr.getRealValue());
        assertEquals(childNode.getPath()+Node.ATTRIBUTE_SEPARATOR+attr.getName(), refAttr.getRawValue());
    }

    private void addAttribute() throws Exception {
        attr = new NodeAttributeImpl("attr", String.class, "10", null);
        attr.setOwner(childNode);
        childNode.addAttr(attr);
        attr.init();
        attr.save();
//        NodeAttribute refAttr = new NodeAttributeImpl("ref", Integer.class, null, null);
//        refAttr.setValueHandlerType(AttributeReferenceValueHandlerFactory.TYPE);
    }
}

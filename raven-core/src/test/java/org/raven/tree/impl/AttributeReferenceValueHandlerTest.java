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

import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.tree.AttributeNotFoundException;
import org.raven.tree.InvalidPathException;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Node;
import org.raven.tree.AttributeValueHandlerListener;
import static org.easymock.EasyMock.*;
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
    
    @Before
    public void setupTest() throws Exception
    {
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
    
    @Test(expected=InvalidPathException.class)
    public void setData_noAttributeSeparatorInPath() throws Exception
    {
        NodeAttribute attr = createMock(NodeAttribute.class);
        expect(attr.getRawValue()).andReturn(null);
        replay(attr);
        
        AttributeReferenceValueHandler valueHandler = new AttributeReferenceValueHandler(attr);
        valueHandler.setData(childNode.getPath());
        
        verify(attr);
    }
    
    @Test(expected=AttributeNotFoundException.class)
    public void setData_attributeNotFound1() throws Exception
    {
        NodeAttribute attr = createMock(NodeAttribute.class);
        expect(attr.getOwner()).andReturn(node);
        expect(attr.getRawValue()).andReturn(null);
        replay(attr);
        
        AttributeReferenceValueHandler valueHandler = new AttributeReferenceValueHandler(attr);
        valueHandler.setData(childNode.getPath()+Node.ATTRIBUTE_SEPARATOR+"xxx");
        
        verify(attr);
    }
    
    @Test(expected=AttributeNotFoundException.class)
    public void setData_attributeNotFound2() throws Exception
    {
        NodeAttribute attr = createMock(NodeAttribute.class);
        expect(attr.getOwner()).andReturn(node);
        expect(attr.getRawValue()).andReturn(null);
        replay(attr);
        
        AttributeReferenceValueHandler valueHandler = new AttributeReferenceValueHandler(attr);
        valueHandler.setData(childNode.getPath()+Node.ATTRIBUTE_SEPARATOR);
        
        verify(attr);
    }
    
    @Test
    public void setData() throws Exception
    {
        NodeAttribute refAttr = createMock(NodeAttribute.class);
        AttributeValueHandlerListener listener = createMock(AttributeValueHandlerListener.class);
        
        expect(refAttr.getOwner()).andReturn(node);
        expect(refAttr.getRawValue()).andReturn(null);
        expect(refAttr.getType()).andReturn(Integer.class).times(2);
        refAttr.save();
        listener.valueChanged(null, 10);
        listener.valueChanged(10, 99);
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
        
        expect(refAttr.getOwner()).andReturn(node).times(2);
        expect(refAttr.getRawValue()).andReturn(null);
        expect(refAttr.getType()).andReturn(Integer.class).times(2);
        refAttr.save();
        listener.valueChanged(null, 10);
        listener.expressionInvalidated(10);
        listener.valueChanged(null, 10);
        replay(refAttr, listener);
        
        AttributeReferenceValueHandler valueHandler = new AttributeReferenceValueHandler(refAttr);
        valueHandler.addListener(listener);
        valueHandler.setData(childNode.getPath()+Node.ATTRIBUTE_SEPARATOR+attr.getName());
        
        assertEquals(10, valueHandler.handleData());
        assertTrue(childNode.getListeners().contains(valueHandler));
        childNode.removeNodeAttribute("attr");
        
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
    public void attributeRenamed() throws Exception
    {
        NodeAttribute refAttr = createMock(NodeAttribute.class);
        AttributeValueHandlerListener listener = createMock(AttributeValueHandlerListener.class);
        
        expect(refAttr.getOwner()).andReturn(node).times(1);
        expect(refAttr.getRawValue()).andReturn(null);
        expect(refAttr.getType()).andReturn(Integer.class).times(2);
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
        
        valueHandler.getData().endsWith("newName");
        assertEquals(10, valueHandler.handleData());
        attr.setValue("20");
        assertEquals(20, valueHandler.handleData());
    }

    @Test
    public void moveTest() throws Exception
    {
        NodeAttribute refAttr = new NodeAttributeImpl("refAttr", Integer.class, null, null);
        refAttr.setValueHandlerType(AttributeReferenceValueHandlerFactory.TYPE);
        refAttr.setOwner(node);
        node.addNodeAttribute(refAttr);
        refAttr.init();
        refAttr.setValue(childNode.getPath()+Node.ATTRIBUTE_SEPARATOR+attr.getName());
        refAttr.save();

        BaseNode newHome = new BaseNode("newHome");
        tree.getRootNode().addAndSaveChildren(newHome);

        tree.move(childNode, newHome);

        assertEquals(10, refAttr.getRealValue());
        String newPath = "/\"newHome\"/\"child\"@attr";
        assertEquals(newPath, refAttr.getRawValue());

        tree.reloadTree();

        node = tree.getNode(node.getPath());
        refAttr = node.getNodeAttribute("refAttr");
        assertEquals(newPath, refAttr.getRawValue());
        assertEquals(10, refAttr.getRealValue());
    }
    
    @Test
    public void realTest() throws Exception
    {
        NodeAttribute refAttr = new NodeAttributeImpl("refAttr", Integer.class, null, null);
        refAttr.setValueHandlerType(AttributeReferenceValueHandlerFactory.TYPE);
        refAttr.setOwner(node);
        node.addNodeAttribute(refAttr);
        refAttr.init();
        refAttr.setValue(childNode.getPath()+Node.ATTRIBUTE_SEPARATOR+attr.getName());
        refAttr.save();
        
        assertEquals("10", refAttr.getValue());
        assertEquals(10, refAttr.getRealValue());
        
        tree.reloadTree();
        
        node = tree.getNode(node.getPath());
        assertNotNull(node);
        refAttr = node.getNodeAttribute("refAttr");
        assertNotNull(refAttr);
        assertEquals("10", refAttr.getValue());
        assertEquals(10, refAttr.getRealValue());
        assertEquals(
                childNode.getPath()+Node.ATTRIBUTE_SEPARATOR+attr.getName(), refAttr.getRawValue());
    }

    private void addAttribute() throws Exception
    {
        attr = new NodeAttributeImpl("attr", String.class, "10", null);
        attr.setOwner(childNode);
        childNode.addNodeAttribute(attr);
        attr.init();
        attr.save();
//        NodeAttribute refAttr = new NodeAttributeImpl("ref", Integer.class, null, null);
//        refAttr.setValueHandlerType(AttributeReferenceValueHandlerFactory.TYPE);
    }
}

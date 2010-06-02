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

package org.raven.expr.impl;

import javax.script.Bindings;
import javax.script.ScriptException;
import org.easymock.IArgumentMatcher;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.AttributeValueHandlerListener;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.slf4j.Logger;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class ExpressionAttributeValueHandlerTest extends RavenCoreTestCase
{
    @Test 
    public void handlerConstruction() throws Exception
    {
        NodeAttribute attribute = createMock(NodeAttribute.class);
        Node node = createMock(Node.class);
        AttributeValueHandlerListener listener = createMock(AttributeValueHandlerListener.class);
        Logger logger = createMock(Logger.class);
        
        expect(attribute.getRawValue()).andReturn(null);
        attribute.save();
        expectLastCall().times(3);
        expect(attribute.getValueHandlerType())
                .andReturn(ExpressionAttributeValueHandlerFactory.TYPE).anyTimes();
        listener.valueChanged(isNull(), eq(2));
        expect(attribute.getOwner()).andReturn(node).times(5);
        node.formExpressionBindings(isA(Bindings.class));
        expectLastCall().times(2);
        expect(node.getLogger()).andReturn(logger);
        listener.expressionInvalidated(2);
        listener.valueChanged(2, null);
        listener.valueChanged(null, 10);
        logger.warn(isA(String.class), isA(ScriptException.class));
        
        replay(node, attribute, listener, logger);
        
        ExpressionAttributeValueHandler handler = new ExpressionAttributeValueHandler(attribute);
        assertTrue(handler.isExpressionSupported());
        assertFalse(handler.isReferenceValuesSupported());
        assertTrue(handler.isExpressionValid());
        handler.addListener(listener);
        handler.setData("1+1");
        Object result = handler.handleData();
        assertEquals(2, result);
        
        try{
            handler.setData("(_.&)");
            fail();
        }catch(ScriptException e){}
        assertFalse(handler.isExpressionValid());
        assertNull(handler.handleData());
        
        handler.setData("3+7");
        assertTrue(handler.isExpressionValid());
        assertEquals(10, handler.handleData());
        
        verify(node, attribute, listener, logger);
    }
    
    @Test 
    public void handlerReConstruction() throws Exception
    {
        NodeAttribute attribute = createMock(NodeAttribute.class);
        Node node = createMock(Node.class);
        AttributeValueHandlerListener listener = createMock(AttributeValueHandlerListener.class);
        
        expect(attribute.getRawValue()).andReturn("1+1");
        expect(attribute.getOwner()).andReturn(node).times(2);
        expect(attribute.getValueHandlerType())
                .andReturn(ExpressionAttributeValueHandlerFactory.TYPE).anyTimes();
        node.formExpressionBindings(isA(Bindings.class));
        listener.valueChanged(null, 2);
        
        replay(node, attribute, listener);
        
        ExpressionAttributeValueHandler handler = new ExpressionAttributeValueHandler(attribute);
        assertTrue(handler.isExpressionValid());
        handler.addListener(listener);
        Object result = handler.handleData();
        assertEquals(2, result);
        
        verify(node, attribute, listener);
    }
    
    @Test
    public void expressionWithBindings() throws Exception
    {
        NodeAttribute attribute = createMock(NodeAttribute.class);
        Node node = createMock(Node.class);
        
        expect(attribute.getRawValue()).andReturn(null);
        expect(attribute.getValueHandlerType())
                .andReturn(ExpressionAttributeValueHandlerFactory.TYPE).anyTimes();
        attribute.save();
        expect(attribute.getOwner()).andReturn(node).times(2);
        node.formExpressionBindings(formBindings());
        
        replay(node, attribute);
        
        ExpressionAttributeValueHandler handler = new ExpressionAttributeValueHandler(attribute);
        handler.setData("var+1");
        
        Object result = handler.handleData();
        assertEquals(2, result);
        
        handler.setData("var+1");
        
        verify(node, attribute);
    }
    
    @Test
    public void nodeBindingTest() throws Exception
    {
        NodeAttribute attribute = createMock(NodeAttribute.class);
        Node node = createMock(Node.class);
        
        expect(attribute.getRawValue()).andReturn(null);
        expect(attribute.getValueHandlerType())
                .andReturn(ExpressionAttributeValueHandlerFactory.TYPE).anyTimes();
        attribute.save();
        expect(attribute.getOwner()).andReturn(node).times(2);
        node.formExpressionBindings(isA(Bindings.class));
        expect(node.getName()).andReturn("nodeName");
        
        replay(node, attribute);
        
        ExpressionAttributeValueHandler handler = new ExpressionAttributeValueHandler(attribute);
        handler.setData("node.name");
        
        Object result = handler.handleData();
        assertEquals("nodeName", result);
        
        verify(node, attribute);
    }

    @Test
    public void scriptTest() throws Exception
    {
        NodeAttribute attribute = createMock(NodeAttribute.class);
        Node node = createMock(Node.class);

        expect(attribute.getRawValue()).andReturn(null);
        expect(attribute.getValueHandlerType())
                .andReturn(ScriptAttributeValueHandlerFactory.TYPE).anyTimes();
        attribute.save();
        expect(attribute.getOwner()).andReturn(node).times(4);
        node.formExpressionBindings(isA(Bindings.class));
        node.formExpressionBindings(formBindings2());

        replay(node, attribute);

        ExpressionAttributeValueHandler handler = new ExpressionAttributeValueHandler(attribute);
        handler.setData("var+1");

        Object result = handler.handleData();
        assertNull(result);
        result = handler.handleData();
        assertEquals(2, result);

        verify(node, attribute);
    }
    
    @Test
    public void realTest() throws Exception 
    {
        Node node = new BaseNode("node");
        tree.getRootNode().addChildren(node);
        node.save();
        node.init();
        
        NodeAttribute attr = new NodeAttributeImpl("attr", Integer.class, "1+1", null);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr.init();
        
        assertEquals(2, attr.getRealValue());
        assertEquals("1+1", attr.getRawValue());
        
        attr.setValue("5+5");
        assertEquals(10, attr.getRealValue());
        assertEquals("5+5", attr.getRawValue());
        
    }

    @Test
    public void varsBindingTest() throws Exception
    {
        Node node = new BaseNode("node");
        tree.getRootNode().addChildren(node);
        node.save();
        node.init();

        NodeAttribute attr = new NodeAttributeImpl("attr", Integer.class, "vars.val=10; node['attr2'].value+vars.val", null);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr.init();

        NodeAttribute attr2 = new NodeAttributeImpl("attr2", Integer.class, "vars.val+=10", null);
        attr2.setOwner(node);
        node.addNodeAttribute(attr2);
        attr2.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr2.init();

        assertEquals(40, attr.getRealValue());
    }
    
    private static Bindings formBindings()
    {
        reportMatcher(new IArgumentMatcher() 
        {
            public boolean matches(Object argument) {
                Bindings bindings = (Bindings) argument;
                bindings.put("var", 1);
                return true;
            }

            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }

    private static Bindings formBindings2()
    {
        reportMatcher(new IArgumentMatcher()
        {
            public boolean matches(Object argument) {
                Bindings bindings = (Bindings) argument;
                bindings.put("var", 1);
                bindings.put(ExpressionAttributeValueHandler.ENABLE_SCRIPT_EXECUTION_BINDING, true);
                return true;
            }

            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
}

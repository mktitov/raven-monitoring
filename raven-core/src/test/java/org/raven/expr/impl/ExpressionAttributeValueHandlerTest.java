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

import org.raven.tree.PropagatedAttributeValueError;
import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptException;
import org.easymock.IArgumentMatcher;
import org.junit.Test;
import org.raven.expr.BindingSupport;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.AttributeValueHandlerListener;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.slf4j.Logger;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;
import org.raven.BindingNames;
import org.raven.BindingSourceNode;
/**
 *
 * @author Mikhail Titov
 */
public class ExpressionAttributeValueHandlerTest extends RavenCoreTestCase
{
    @Test
    public void speedTest() throws Exception
    {
        Node node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());
        NodeAttributeImpl attr = new NodeAttributeImpl("attr", String.class, "'test'", null);
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr.setOwner(node);
        attr.init();
        node.addAttr(attr);
        
        assertEquals("test", attr.getRealValue());
        long startTime = System.currentTimeMillis();
        for (int i=0; i<10000; ++i)
            attr.getRealValue();
        System.out.println("!!! "+(System.currentTimeMillis()-startTime));
    }
    
    @Test 
    public void handlerConstruction() throws Exception
    {
        NodeAttribute attribute = createMock(NodeAttribute.class);
        Node node = createMock(Node.class);
        AttributeValueHandlerListener listener = createMock(AttributeValueHandlerListener.class);
        Logger logger = createMock(Logger.class);
        
        expect(attribute.getRawValue()).andReturn(null);
        expect(attribute.getName()).andReturn("attr").anyTimes();
        expect(attribute.getId()).andReturn(1).atLeastOnce();
        attribute.save();
        expectLastCall().times(3);
        expect(attribute.getValueHandlerType())
                .andReturn(ExpressionAttributeValueHandlerFactory.TYPE).anyTimes();
//        listener.valueChanged(isNull(), eq(2));
        expect(attribute.getOwner()).andReturn(node).anyTimes();
        expect(node.getId()).andReturn(1).atLeastOnce();
//        expect(node.getName()).andReturn("node").atLeastOnce();
        node.formExpressionBindings(isA(Bindings.class));
        expectLastCall().times(2);
        expect(node.getLogger()).andReturn(logger).anyTimes();
        listener.expressionInvalidated(null);
//        listener.valueChanged(2, null);
//        listener.valueChanged(null, 10);
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
        NodeAttribute attr = createMock(NodeAttribute.class);
        Node node = createMock(Node.class);
        AttributeValueHandlerListener listener = createMock(AttributeValueHandlerListener.class);

        expect(attr.getName()).andReturn("attr").anyTimes();
        expect(attr.getId()).andReturn(1);
//        expect(node.getName()).andReturn("node").atLeastOnce();
        expect(node.getId()).andReturn(1).atLeastOnce();
        expect(attr.getRawValue()).andReturn("1+1");
        expect(attr.getOwner()).andReturn(node).atLeastOnce();
        expect(attr.getValueHandlerType())
                .andReturn(ExpressionAttributeValueHandlerFactory.TYPE).anyTimes();
        expect(node.getLogger()).andReturn(null).anyTimes();
        node.formExpressionBindings(isA(Bindings.class));
//        listener.valueChanged(null, 2);
        
        replay(node, attr, listener);
        
        ExpressionAttributeValueHandler handler = new ExpressionAttributeValueHandler(attr);
        assertTrue(handler.isExpressionValid());
        handler.addListener(listener);
        Object result = handler.handleData();
        assertEquals(2, result);
        
        verify(node, attr, listener);
    }
    
    @Test
    public void expressionWithBindings() throws Exception
    {
        NodeAttribute attr = createMock(NodeAttribute.class);
        Node node = createMock(Node.class);
        
        expect(attr.getName()).andReturn("attr").anyTimes();
        expect(attr.getId()).andReturn(1);
//        expect(node.getName()).andReturn("node").atLeastOnce();
        expect(node.getId()).andReturn(1).atLeastOnce();
        expect(attr.getRawValue()).andReturn(null);
        expect(attr.getValueHandlerType())
                .andReturn(ExpressionAttributeValueHandlerFactory.TYPE).anyTimes();
        attr.save();
        expect(attr.getOwner()).andReturn(node).atLeastOnce();
        expect(node.getLogger()).andReturn(null).anyTimes();
        node.formExpressionBindings(formBindings());
        
        replay(node, attr);
        
        ExpressionAttributeValueHandler handler = new ExpressionAttributeValueHandler(attr);
        handler.setData("var+1");
        
        Object result = handler.handleData();
        assertEquals(2, result);
        
        handler.setData("var+1");
        
        verify(node, attr);
    }
    
    @Test
    public void nodeBindingTest() throws Exception
    {
        NodeAttribute attr = createMock(NodeAttribute.class);
        Node node = createMock(Node.class);
        
        expect(attr.getName()).andReturn("attr").anyTimes();
        expect(attr.getId()).andReturn(1).atLeastOnce();
        expect(node.getName()).andReturn("node").atLeastOnce();
        expect(node.getId()).andReturn(1).atLeastOnce();
        expect(attr.getRawValue()).andReturn(null);
        expect(attr.getValueHandlerType())
                .andReturn(ExpressionAttributeValueHandlerFactory.TYPE).anyTimes();
        attr.save();
        expect(attr.getOwner()).andReturn(node).atLeastOnce();
        expect(node.getLogger()).andReturn(null).anyTimes();
        node.formExpressionBindings(isA(Bindings.class));
        
        replay(node, attr);
        
        ExpressionAttributeValueHandler handler = new ExpressionAttributeValueHandler(attr);
        handler.setData("node.name");
        
        Object result = handler.handleData();
        assertEquals("node", result);
        
        verify(node, attr);
    }

    @Test
    public void scriptTest() throws Exception
    {
        NodeAttribute attr = createMock(NodeAttribute.class);
        Node node = createMock(Node.class);

        expect(attr.getName()).andReturn("attr").anyTimes();
        expect(attr.getId()).andReturn(1).atLeastOnce();
//        expect(node.getName()).andReturn("node").atLeastOnce();
        expect(node.getId()).andReturn(1).atLeastOnce();
        expect(attr.getRawValue()).andReturn(null);
        expect(attr.getValueHandlerType())
                .andReturn(ScriptAttributeValueHandlerFactory.TYPE).anyTimes();
        attr.save();
        expect(attr.getOwner()).andReturn(node).anyTimes();
        expect(node.getLogger()).andReturn(null).anyTimes();
        node.formExpressionBindings(isA(Bindings.class));
        node.formExpressionBindings(formBindings2());

        replay(node, attr);

        ExpressionAttributeValueHandler handler = new ExpressionAttributeValueHandler(attr);
        handler.setData("var+1");

        Object result = handler.handleData();
        assertNull(result);
        result = handler.handleData();
        assertEquals(2, result);

        verify(node, attr);
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
    public void varsBindingTest() throws Exception {
        Node node = new BaseNode("node");
        tree.getRootNode().addChildren(node);
        node.save();
        node.init();

        NodeAttribute attr = new NodeAttributeImpl("attr", Integer.class, "vars.val=10; node.$attr2+vars.val", null);
        attr.setOwner(node);
        node.addAttr(attr);
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr.init();

        NodeAttribute attr2 = new NodeAttributeImpl("attr2", Integer.class, "vars.val+=10", null);
        attr2.setOwner(node);
        node.addAttr(attr2);
        attr2.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr2.init();

        assertEquals(40, attr.getRealValue());
    }
    
    @Test
    public void exceptionThrowTest() throws Exception {
        Node node = new BaseNode("node");
        testsNode.addAndSaveChildren(node);
        assertTrue(node.start());
        
        NodeAttributeImpl attr = new NodeAttributeImpl("attr1", String.class, null, "test");
        attr.setOwner(node);
        node.addAttr(attr);
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);        
        attr.init();
        attr.setValue("println 'Hello world!'\na = {throw new Exception('Test exception throwed')}\na()");
        attr.getValue();
        
        attr = new NodeAttributeImpl("attr2", String.class, "node.$attr1", "test");
        attr.setOwner(node);
        node.addAttr(attr);
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);        
        attr.init();
        attr.getValue();
        
//        attr.setValue("1");
//        assertEquals(1, attr.getRealValue());
    }
    
    @Test(expected = PropagatedAttributeValueError.class)
    public void propagateExceptionTest() throws Exception {
        BindingSourceNode node = new BindingSourceNode();
        node.setName("node");
        testsNode.addAndSaveChildren(node);
        assertTrue(node.start());
        
        NodeAttributeImpl attr = new NodeAttributeImpl("attr1", String.class, null, "test");
        attr.setOwner(node);
        node.addAttr(attr);
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);        
        attr.init();
        attr.setValue("println 'Hello world!'\na = {throw new Exception('Test exception throwed')}\na()");
        node.addBinding(BindingNames.PROPAGATE_EXPRESSION_EXCEPTION, null);
        attr.getValue();
    }
    
    @Test
    public void argsBindingTest() throws Exception
    {
        Node node = new BaseNode("node");
        tree.getRootNode().addChildren(node);
        node.save();
        node.init();

        NodeAttribute attr = new NodeAttributeImpl("attr", Integer.class, "node['attr2'].getValue(arg2:20)+arg1", null);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr.init();

        NodeAttribute attr2 = new NodeAttributeImpl("attr2", Integer.class, "args.containsKey('arg1')?0:arg2", null);
        attr2.setOwner(node);
        node.addNodeAttribute(attr2);
        attr2.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr2.init();

        Map args = new HashMap();
        args.put("arg1", 10);
        BindingSupport varsSupport = tree.getGlobalBindings(Tree.EXPRESSION_VARS_BINDINGS);
        varsSupport.put(ExpressionAttributeValueHandler.RAVEN_EXPRESSION_ARGS_BINDING, args);        
        assertEquals(30, attr.getRealValue());
        assertFalse(varsSupport.contains(ExpressionAttributeValueHandler.RAVEN_EXPRESSION_ARGS_BINDING));
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

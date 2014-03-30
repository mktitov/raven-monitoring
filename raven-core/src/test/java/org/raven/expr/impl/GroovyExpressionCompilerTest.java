/*
 *  Copyright 2008 tim.
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

import java.sql.Connection;
import java.util.List;
import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.dbcp.impl.JDBCConnectionPoolNode;
import org.raven.ds.impl.DataContextImpl;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCache;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.SystemNode;

public class GroovyExpressionCompilerTest extends RavenCoreTestCase
{
    private Bindings bindings;
    private Node node;
    private Node child;
    
    @Before
    public void prepare() throws Exception {
        node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());
        
        NodeAttributeImpl attr = new NodeAttributeImpl("attr1", String.class, "val1", null);
        attr.setOwner(node);
        attr.init();
        node.addNodeAttribute(attr);
        assertNotNull(node.getAttr("attr1"));
        attr = new NodeAttributeImpl("attr2", String.class, "param1", null);        
        attr.setOwner(node);
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr.init();
        node.addNodeAttribute(attr);
        
        child = new BaseNode("child");
        node.addAndSaveChildren(child);
        assertTrue(child.start());
        
        bindings = new SimpleBindings(); //containsNode hasNode '
        bindings.put("node", node);
    }
    
    @Test
    public void simpleTest() throws ScriptException
    {
        ExpressionCache cache = trainCache("1+1", true);
        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile("1+1", GroovyExpressionCompiler.LANGUAGE, null);
        assertNotNull(expression);
        assertEquals(2, expression.eval(null));

        verify(cache);
    }

    @Test
    public void bindginsTest() throws ScriptException
    {
        ExpressionCache cache = trainCache("var+=1", true);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile("var+=1", GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        Bindings bindings = new SimpleBindings();
        bindings.put("var", 1);
        assertEquals(2, expression.eval(bindings));

        verify(cache);
    }

    @Test
    public void withConnectionTest() throws Exception
    {
        String script = "res=null; withConnection(con){c -> res='ok'}\n res";
        ExpressionCache cache = trainCache(script, false);
        Connection connection = createMock(Connection.class);
        connection.commit();
        connection.close();
        replay(connection, cache);
        
        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("con", connection);
        assertEquals("ok", expression.eval(bindings));

        verify(connection, cache);
    }
    
    @Test
    public void withSqlTest() throws Exception
    {
        String script = "res=null; withSql(con){c -> res='ok'}\n res";
        ExpressionCache cache = trainCache(script, false);
        Connection connection = createMock(Connection.class);
        connection.commit();
        connection.close();
        replay(connection, cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("con", connection);
        assertEquals("ok", expression.eval(bindings));

        verify(connection, cache);
    }

    @Test
    public void withSqlTest2() throws Exception
    {
        String script = "res=null; withSql(con){c -> res='ok'; res.notDefinedProperty}\n res";
        ExpressionCache cache = trainCache(script, false);
        Connection connection = createMock(Connection.class);
        connection.rollback();
        connection.close();
        replay(connection, cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("con", connection);
        try{
            expression.eval(bindings);
            fail();
        }catch(ScriptException e){
        }

        verify(connection, cache);
    }

    @Test
    public void withSqlTest3() throws Exception
    {
        Config conf = configurator.getConfig();
        assertNotNull(conf);

        ConnectionPoolsNode poolsNode =
                (ConnectionPoolsNode)
                tree.getNode(SystemNode.NAME).getChildren(ConnectionPoolsNode.NAME);
        assertNotNull(poolsNode);
        JDBCConnectionPoolNode pool = new JDBCConnectionPoolNode();
        pool.setName("pool");
        poolsNode.addChildren(pool);
        pool.save();
        pool.init();

        pool.setUserName(conf.getStringProperty(Configurator.TREE_STORE_USER, null));
        pool.setPassword(conf.getStringProperty(Configurator.TREE_STORE_PASSWORD, null));
        pool.setUrl(conf.getStringProperty(Configurator.TREE_STORE_URL, null));
        pool.setDriver("org.h2.Driver");
        assertTrue(pool.start());

        NodeAttributeImpl attr = new NodeAttributeImpl("connectionPool", Node.class, null, null);
        attr.setOwner(node);
        attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        attr.init();
        node.addNodeAttribute(attr);
        attr.setValue(pool.getPath());

        String script = "res=null; withSql{sql-> res=sql.firstRow('select count(*) from nodes')[0]}; res";
		ExpressionCache cache = trainCache(script, false);
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("node", node);
        Object res=expression.eval(bindings);
        assertNotNull(res);
        assertTrue(res instanceof Number);

        verify(cache);
    }
    
    @Test
    public void buildJsonTest() throws Exception {
        String script = "buildJson test:true, test2:false";
        ExpressionCache cache = trainCache(script, false);
        
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        Object res = expression.eval(new SimpleBindings());
        assertNotNull(res);
        assertEquals("{\"test\":true,\"test2\":false}", res);

        verify(cache);       
    }
    
    @Test
    public void buildXmlTest() throws Exception {
        String script = "buildXml{it.hello('world')}";
        ExpressionCache cache = trainCache(script, false);
        
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        Object res = expression.eval(new SimpleBindings());
        assertNotNull(res);
        assertEquals("<hello>world</hello>", res);

        verify(cache);       
    }
    
    @Test
    public void buildXmlWithEncodingTest() throws Exception {
        String script = "buildXml('utf-8'){it.hello('world')}";
        ExpressionCache cache = trainCache(script, false);
        
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        Object res = expression.eval(new SimpleBindings());
        assertNotNull(res);
        assertEquals("<?xml encoding='utf-8' version='1.0'?>\n<hello>world</hello>", res);

        verify(cache);       
    }
    
    @Test
    public void ifDataTest() throws Exception {
        String script = "ifData { 1 }";
        ExpressionCache cache = trainCache(script, false);
        
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        bindings.put("data", 0);
        assertEquals(0, expression.eval(bindings));

        verify(cache);       
    }
    
    @Test
    public void ifDataTest2() throws Exception {
        String script = "ifData { 5 }";
        ExpressionCache cache = trainCache(script, false);
        
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        bindings.put("data", 2);
        assertEquals(5, expression.eval(bindings));

        verify(cache);       
    }
    
    @Test
    public void ifDataTest3() throws Exception {
        String script = "ifData(10) { 1 }";
        ExpressionCache cache = trainCache(script, false);
        
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        bindings.put("data", 0);
        assertEquals(10, expression.eval(bindings));

        verify(cache);       
    }
    
    @Test
    public void ifDataTest4() throws Exception {
        String script = "ifData(10) { 1 }";
        ExpressionCache cache = trainCache(script, false);
        
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        bindings.put("data", 2);
        assertEquals(1, expression.eval(bindings));

        verify(cache);       
    }
    
    @Test
    public void catchErrors_successTest() throws Exception {
        String script = "catchErrors { 1 }";
        ExpressionCache cache = trainCache(script, false);
        
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        DataContextImpl context = new DataContextImpl();
        bindings.put("context", context);
        assertEquals(1, expression.eval(bindings));
        assertFalse(context.hasErrors());

        verify(cache);       
    }
    
    @Test
    public void catchErrors_successWithFinalValueTest() throws Exception {
        String script = "catchErrors(2) { 1 }";
        ExpressionCache cache = trainCache(script, false);
        
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        DataContextImpl context = new DataContextImpl();
        bindings.put("context", context);
        assertEquals(2, expression.eval(bindings));
        assertFalse(context.hasErrors());

        verify(cache);       
    }
    
    @Test()
    public void catchErrors_withExceptionTest() throws Exception {
        String script = "catchErrors { throw new Exception('test error') }";
        ExpressionCache cache = trainCache(script, false);
        
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        DataContextImpl context = new DataContextImpl();
        bindings.put("context", context);
        try {
            expression.eval(bindings);
            fail();
        } catch (ScriptException e) { }
        assertTrue(context.hasErrors());
        assertEquals("test error", context.getFirstError().getMessage());

        verify(cache);       
    }
    
    @Test()
    public void catchErrors_withExceptionTest2() throws Exception {
        String script = "catchErrors { throw new NullPointerException() }";
        ExpressionCache cache = trainCache(script, false);
        
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        DataContextImpl context = new DataContextImpl();
        bindings.put("context", context);
        try {
            expression.eval(bindings);
            fail();
        } catch (ScriptException e) { }
        assertTrue(context.hasErrors());
        assertTrue(context.getFirstError().getError() instanceof NullPointerException);

        verify(cache);       
    }
    
    @Test
    public void catchErrors_withFinalValueAndExceptionTest() throws Exception {
        String script = "catchErrors(2) { throw new Exception('test error') }";
        ExpressionCache cache = trainCache(script, false);
        
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile(script, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        DataContextImpl context = new DataContextImpl();
        bindings.put("context", context);
        assertEquals(2, expression.eval(bindings));
        assertTrue(context.hasErrors());
        assertEquals("test error", context.getFirstError().getMessage());

        verify(cache);       
    }
    
    @Test
    public void getNodeWrapperTest() throws Exception {
        ExpressionCache cache = trainCache();
        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        
        assertSame(child, executeExpr(compiler, "node.getNode('child')"));
        Object nodes = executeExpr(compiler, "node.nodes");
        assertTrue(nodes instanceof List);
        List list = (List) nodes;
        assertEquals(1, list.size());
        assertSame(child, list.get(0));
        verify(cache);     
        
        assertEquals("val1", executeExpr(compiler, "node.$attr1"));        
        assertEquals("val2", executeExpr(compiler, "node.$attr1 = 'val2'"));        
        assertEquals("val2", executeExpr(compiler, "node.$attr1"));
        assertEquals("val", executeExpr(compiler, "node.$attr2 param1:'val'"));
        assertSame(child, executeExpr(compiler, "node.getNode('child')"));
        assertSame(node.getAttr("attr1"), executeExpr(compiler, "node.getAttr 'attr1'"));
    }
    
    @Test
    public void getDataTest() throws Exception {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        ExpressionCache cache = trainCache();
        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
//        bindings.put("context", new DataContextImpl());
        
        Object res = executeExpr(compiler, "getData(node.parent.getNode('dataSource'), createDataContext())");
        assertNotNull(res);
        assertTrue(res instanceof List);
        assertTrue(((List)res).isEmpty());
        
        ds.addDataPortion("test");
        ds.addDataPortion(null);
        res = executeExpr(compiler, "getData(node.parent.getNode('dataSource'), createDataContext())");
        assertNotNull(res);
        assertTrue(res instanceof List);
        List data = (List) res;
        assertEquals(1, data.size());
        assertEquals("test", data.get(0));
        
        verify(cache);
    }

    @Test
    public void getDataTest2() throws Exception {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        ExpressionCache cache = trainCache();
        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
//        bindings.put("context", new DataContextImpl());
        
        Object res = executeExpr(compiler, "getData(node.parent.getNode('dataSource'))");
        assertNotNull(res);
        assertTrue(res instanceof List);
        assertTrue(((List)res).isEmpty());
        
        ds.addDataPortion("test");
        ds.addDataPortion(null);
        res = executeExpr(compiler, "getData(node.parent.getNode('dataSource'))");
        assertNotNull(res);
        assertTrue(res instanceof List);
        List data = (List) res;
        assertEquals(1, data.size());
        assertEquals("test", data.get(0));
        
        verify(cache);
    }

    @Test
    public void nonGroovyLanguageTest() throws Exception {
        ExpressionCache cache = createMock(ExpressionCache.class);
        replay(cache);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile("some expression", "notGroovy", null);
        assertNull(expression);
    }

    private ExpressionCache trainCache(String expressionSource, boolean replay) {
        ExpressionCache cache = createMock(ExpressionCache.class);
        cache.putExpression(eq(expressionSource), isA(Expression.class));
        
        if (replay) {
            replay(cache);
        }

        return cache;
    }
    
    private ExpressionCache trainCache() {
        ExpressionCache cache = createMock(ExpressionCache.class);
        cache.putExpression(isA(String.class), isA(Expression.class));
        expectLastCall().atLeastOnce();
        replay(cache);
        return cache;
    }
    
    private Object executeExpr(GroovyExpressionCompiler compiler, String expr) throws Exception {
        Expression expression = compiler.compile(expr, GroovyExpressionCompiler.LANGUAGE, "test");
        assertNotNull(expression);
        return expression.eval(bindings);
    }
}
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
import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.junit.Test;
import org.raven.api.impl.NodeAccessImpl;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.dbcp.impl.JDBCConnectionPoolNode;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCache;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.SystemNode;
import static org.easymock.EasyMock.*;

public class GroovyExpressionCompilerTest extends RavenCoreTestCase
{
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

        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());
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
        NodeAccessImpl nodeAccess = new NodeAccessImpl(node);
        bindings.put("node", nodeAccess);
        Object res=expression.eval(bindings);
        assertNotNull(res);
        assertTrue(res instanceof Number);

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
}
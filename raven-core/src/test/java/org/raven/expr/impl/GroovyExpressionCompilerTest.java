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
import org.junit.Assert;
import org.junit.Test;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCache;
import static org.easymock.EasyMock.*;

public class GroovyExpressionCompilerTest extends Assert
{
    @Test
    public void simpleTest() throws ScriptException
    {
		ExpressionCache cache = trainCache("1+1", true);
        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile("1+1", "groovy");
        assertNotNull(expression);
        assertEquals(2, expression.eval(null));

		verify(cache);
    }

    @Test
    public void bindginsTest() throws ScriptException
    {
		ExpressionCache cache = trainCache("var+=1", true);

        GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
        Expression expression = compiler.compile("var+=1", "groovy");
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
        Expression expression = compiler.compile(script, "groovy");
        assertNotNull(expression);
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("con", connection);
        assertEquals("ok", expression.eval(bindings));

        verify(connection, cache);
    }
    
	@Test
	public void nonGroovyLanguageTest() throws Exception
	{
		ExpressionCache cache = createMock(ExpressionCache.class);
		replay(cache);

		GroovyExpressionCompiler compiler = new GroovyExpressionCompiler(cache);
		Expression expression = compiler.compile("some expression", "notGroovy");
		assertNull(expression);
	}

	private ExpressionCache trainCache(String expressionSource, boolean replay)
	{
		ExpressionCache cache = createMock(ExpressionCache.class);
		cache.putExpression(eq(expressionSource), isA(Expression.class));

        if (replay)
            replay(cache);

		return cache;
	}
}
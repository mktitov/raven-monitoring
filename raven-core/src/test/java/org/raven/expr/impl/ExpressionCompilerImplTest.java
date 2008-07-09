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
import javax.script.SimpleBindings;
import org.junit.Assert;
import org.junit.Test;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCompiler;

/**
 *
 * @author Mikhail Titov
 */
public class ExpressionCompilerImplTest extends Assert
{
    @Test
    public void simpleTest() throws ScriptException
    {
        ExpressionCompiler compiler = new ExpressionCompilerImpl();
        Expression expression = compiler.compile("1+1", "groovy");
        assertNotNull(expression);
        assertEquals(2, expression.eval(null));
    }
    
    @Test
    public void bindginsTest() throws ScriptException
    {
        ExpressionCompiler compiler = new ExpressionCompilerImpl();
        Expression expression = compiler.compile("var+=1", "groovy");
        assertNotNull(expression);
        Bindings bindings = new SimpleBindings();
        bindings.put("var", 1);
        assertEquals(2, expression.eval(bindings));
        assertEquals(2, bindings.get("var"));
    }
}

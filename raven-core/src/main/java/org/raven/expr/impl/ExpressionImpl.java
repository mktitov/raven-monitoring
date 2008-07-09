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
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.raven.expr.Expression;

/**
 *
 * @author Mikhial Titov
 */
public class ExpressionImpl implements Expression
{
    private final ScriptEngine engine;
    private final CompiledScript compiledExpression;
    private final String expression;

    public ExpressionImpl(ScriptEngine engine, String expression) throws ScriptException 
    {
        if (engine instanceof Compilable)
        {
            compiledExpression = ((Compilable)engine).compile(expression);
            this.engine = null;
            this.expression = null;
        }
        else
        {
            this.expression = expression;
            this.engine = engine;
            this.compiledExpression = null;
        }
    }

    public Object eval(Bindings bindings) throws ScriptException 
    {
        if (compiledExpression==null)
            return engine.eval(expression, bindings);
        else
            return compiledExpression.eval(bindings);
    }
}

/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.template.impl;

import javax.script.Bindings;
import javax.script.ScriptException;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCompiler;
import org.raven.expr.impl.GroovyExpressionCompiler;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class TemplateExpression
{
    public static final String TEMPLATE_EXPRESSION_PREFIX = "^t";
    @Service
    private static ExpressionCompiler compiler;

    public static Object eval(String expression, Bindings bindings) throws ScriptException
    {
        if (expression==null || !expression.startsWith(TEMPLATE_EXPRESSION_PREFIX))
            return expression;
        
        expression = expression.substring(TEMPLATE_EXPRESSION_PREFIX.length());
        Expression compExpr = compiler.compile(expression, GroovyExpressionCompiler.LANGUAGE);
        return compExpr.eval(bindings);
    }
}

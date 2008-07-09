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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCompiler;

/**
 *
 * @author Mikhail Titov
 */
public class ExpressionCompilerImpl implements ExpressionCompiler
{
    private final ScriptEngineManager engineManager;

    public ExpressionCompilerImpl() 
    {
        this.engineManager = new ScriptEngineManager();
    }

    public Expression compile(String expression, String language) throws ScriptException 
    {
        ScriptEngine engine = engineManager.getEngineByName(language);
        if (engine==null)
            throw new ScriptException(
                    String.format("Expression engine not found for language (%s)", language));
        return new ExpressionImpl(engine, expression);
    }
}

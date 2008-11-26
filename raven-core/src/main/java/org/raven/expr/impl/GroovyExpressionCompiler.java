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

import groovy.lang.GroovyClassLoader;
import javax.script.ScriptException;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCache;
import org.raven.expr.ExpressionCompiler;

/**
 *
 * @author Mikhail Titov
 */
public class GroovyExpressionCompiler implements ExpressionCompiler
{
	public final static String LANGUAGE = "groovy";
	private final ExpressionCache cache;
	private final GroovyClassLoader classLoader;

	public GroovyExpressionCompiler(ExpressionCache cache)
	{
		this.cache = cache;
		classLoader = new GroovyClassLoader();
	}

	public Expression compile(String expression, String language) throws ScriptException
	{
		if (!LANGUAGE.equals(language))
			return null;

		try
		{
			Class expressionClass = classLoader.parseClass(expression);
			GroovyExpression groovyExpression = new GroovyExpression(expressionClass);
			cache.putExpression(expression, groovyExpression);
			
			return groovyExpression;
		}
		catch(Exception e)
		{
			if (e instanceof ScriptException)
				throw (ScriptException)e;
			else
				throw new ScriptException(e);
		}
	}
}

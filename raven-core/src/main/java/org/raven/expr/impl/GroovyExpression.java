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

import groovy.lang.GroovyObject;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptException;
import org.raven.expr.Expression;

/**
 *
 * @author Mikhail Titov
 */
public class GroovyExpression implements Expression
{
	private final Class expressionClass;

	public GroovyExpression(Class expressionClass)
	{
		this.expressionClass = expressionClass;
	}

	public Object eval(Bindings bindings) throws ScriptException
	{
		try
		{
			GroovyObject obj = (GroovyObject) expressionClass.newInstance();
			if (bindings!=null && bindings.size()>0)
			for (Map.Entry<String, Object> prop: bindings.entrySet())
				obj.setProperty(prop.getKey(), prop.getValue());
			
			return obj.invokeMethod("run", null);
		}
		catch (Exception e)
		{
			throw new ScriptException(e);
		}
	}
}

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
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.concurrent.atomic.AtomicLong;
import javax.script.ScriptException;
import org.raven.api.impl.ApiUtils;
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
    private final AtomicLong counter = new AtomicLong(0);

	public GroovyExpressionCompiler(ExpressionCache cache)
	{
		this.cache = cache;
		classLoader = new GroovyClassLoader();
	}

	public Expression compile(String expression, String language, String scriptName) throws ScriptException
	{
		if (!LANGUAGE.equals(language))
			return null;

		try {
            StringBuilder buf = new StringBuilder()
                .append("import static ").append(ApiUtils.class.getName()).append(".*; ")
                .append("_={}; if (node) { _.delegate = new ").append(PropertySupport.class.getName()).append("(_, node); ")
                .append("_.resolveStrategy = Closure.DELEGATE_FIRST; };")
                .append(expression);
//                .append("action = {").append(expression).append("}")
//                .append("\naction.delegate = new ").append(PropertySupport.class.getName()).append("(action,node)")
//                .append("\naction.resolveStrategy = Closure.DELEGATE_FIRST")
//                .append("\naction()");
            if (expression.contains("withSql"))
                buf
                .append("\ndef withSql(Closure c){\n")
                .append("  con = node['connectionPool']?.value?.connection\n")
                .append("  if (!con) throw new Exception(\"Attribute (connectionPool) not found in the node (${node.path})\".toString())\n")
                .append("  withSql(con, c)\n")
                .append("}\n");
            if (expression.contains("sendData"))
                buf.append("\ndef sendData(target, data) { sendData(node.asNode(), target, data); }\n");
//            buf.append("\ndef methodMissing(String name, Object args) {"
//                    + "invokeMissingMethod(this, node, name, args)}");
//            buf.append("\ndef getProperty(String name) { "
//                    + "getMissingProperty(name, __props) }");
//            buf.append("\ndef void setProperty(String name, val) { "
//                    + " setMissingProperty(name, val, __props) }");
//            buf.append("\ndef getProperty(String name) { "
//                    + "val = getMissingProperty(node, name);"
//                    + "val? val[0] : super.getProperty(name) }");
//            buf.append("\ndef propertyMissing(String name, val) { setMissingProperty(node, name, val) }");
//            buf.append("\ndef hasProperty(String name) { println 'PROP CHECK: '+name }");
            String name = convert(scriptName);
			Class expressionClass = classLoader.parseClass(buf.toString(), name);
			GroovyExpression groovyExpression = new GroovyExpression(expressionClass);
			cache.putExpression(expression, groovyExpression);
			
			return groovyExpression;
		} catch(Exception e) {
			if (e instanceof ScriptException)
				throw (ScriptException)e;
			else
				throw new ScriptException(e);
		}
	}

    private String convert(String ident) {
        StringBuilder sb = new StringBuilder("org.raven.EXPR.");
        if (ident==null || ident.length() == 0)
            sb.append("__");
        else {
            CharacterIterator ci = new StringCharacterIterator(ident);
            for (char c = ci.first(); c != CharacterIterator.DONE; c = ci.next()) {
                if (c == ' ')
                    c = '_';
                if (sb.length() == 0) {
                    if (Character.isJavaIdentifierStart(c)) {
                        sb.append(c);
                        continue;
                    } else {
                        sb.append('_');
                    }
                }
                if (Character.isJavaIdentifierPart(c))
                    sb.append(c);
                else
                    sb.append('_');
            }
        }
        sb.append("._").append(counter.incrementAndGet());
        return sb.toString();
    }
}

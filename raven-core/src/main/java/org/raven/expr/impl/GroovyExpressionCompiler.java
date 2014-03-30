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
import javax.script.ScriptException;
import org.raven.api.impl.ApiUtils;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCache;
import org.raven.expr.ExpressionCompiler;

/**
 *
 * @author Mikhail Titov
 */
public class GroovyExpressionCompiler implements ExpressionCompiler {

    public final static String LANGUAGE = "groovy";
//    private final static AtomicLong counter = new AtomicLong(0);
    private final ExpressionCache cache;
    private final GroovyClassLoader classLoader;

    public GroovyExpressionCompiler(ExpressionCache cache) {
        this.cache = cache;
        classLoader = new GroovyClassLoader();
    }

    public Expression compile(String expression, String language, String scriptName) throws ScriptException {
        if (!LANGUAGE.equals(language)) {
            return null;
        }

        try {
            StringBuilder buf = new StringBuilder()
                    .append("import static ").append(ApiUtils.class.getName()).append(".*; ")
                    .append(expression);
            if (expression.contains("withConnection")) 
                buf.append("\ndef withConnection(Closure c){ withConnection(node.$connectionPool.connection, c) }\n");
            if (expression.contains("withSql")) 
                buf.append("\ndef withSql(Closure c){ withSql(node.$connectionPool.connection, c) }\n");
            if (expression.contains("sendData")) 
                buf.append("\ndef sendData(target, data) { sendData(node, target, data); }\n");
            if (expression.contains("getData")) {
                buf.append("\ndef getData(org.raven.ds.DataSource dataSource, org.raven.ds.DataContext context) { getData(node, dataSource, context); }\n");
                buf.append("\ndef getData(dataSource) { getData(node, dataSource, createDataContext()); }\n");
            } if (expression.contains("ifData")) {
                buf.append("\ndef ifData(Closure c) { data? c() : data }\n");
                buf.append("\ndef ifData(Object rdata, Closure c) { data? c() : rdata }\n");
            } if (expression.contains("catchErrors")) {
                buf.append("\ndef catchErrors(Closure c){ catchErrors(context, node, c); }");
                buf.append("\ndef catchErrors(finalValue, Closure c){ catchErrors(context, node, finalValue, c); }");
            }
            
            String name = convertToIdentificator(scriptName);
            Class expressionClass = classLoader.parseClass(buf.toString(), name);
            GroovyExpression groovyExpression = new GroovyExpression(expressionClass);
            cache.putExpression(expression, groovyExpression);
            return groovyExpression;
        } catch (Exception e) {
            if (e instanceof ScriptException) throw (ScriptException) e;
            else throw new ScriptException(e);
        }
    }

    public static String convertToIdentificator(String ident) {
        if (ident!=null && ident.startsWith("org.raven.EXPR."))
            return ident;
        StringBuilder sb = new StringBuilder("org.raven.EXPR.");
        if (ident == null || ident.length() == 0) {
            sb.append("__");
        } else {
            CharacterIterator ci = new StringCharacterIterator(ident);
            for (char c = ci.first(); c != CharacterIterator.DONE; c = ci.next()) {
                if (c == ' ') {
                    c = '_';
                }
                if (sb.length() == 0) {
                    if (Character.isJavaIdentifierStart(c)) {
                        sb.append(c);
                        continue;
                    } else {
                        sb.append('_');
                    }
                }
                if (Character.isJavaIdentifierPart(c)) {
                    sb.append(c);
                } else {
                    sb.append('_');
                }
            }
        }
//        sb.append("._").append(counter.incrementAndGet());
        return sb.toString();
    }
}

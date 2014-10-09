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

import java.util.Collections;
import org.raven.tree.PropagatedAttributeValueError;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.raven.BindingNames;
import org.raven.expr.BindingSupport;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCompiler;
import org.raven.expr.ExpressionInfo;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodePathResolver;
import org.raven.tree.Tree;
import org.raven.tree.impl.AbstractAttributeValueHandler;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class ExpressionAttributeValueHandler extends AbstractAttributeValueHandler
{
    public final static String ENABLE_SCRIPT_EXECUTION_BINDING = "enableScriptExecution";
    public static final String LOGGER = "logger";
    public static final String NODE_BINDING = "node";
    public static final String RAVEN_EXPRESSION_ARGS_BINDING = "args";
    public static final String RAVEN_EXPRESSION_VARS_BINDING = "vars";
    public static final String RAVEN_EXPRESSION_SOURCES_BINDINS = "sources";
    public static final String EXECUTED_FROM_TEMPLATE_BINDING = "executedFromTemplate";
    public static final String RAVEN_EXPRESSION_VARS_INITIATED_BINDING = "isVarsInitiated";

    @Service
    private static ExpressionCompiler compiler;
    @Service
    private static NodePathResolver pathResolver;
    @Service
    private static Tree tree;
    
    private boolean expressionValid = false;
    private volatile String data;
    private volatile Expression expression;
    private volatile String expressionIdent;
    private Object value;
    
    public ExpressionAttributeValueHandler(NodeAttribute attribute) {
        super(attribute);
        try {
            data = attribute.getRawValue();
            compileExpression();
        } catch (ScriptException ex) {}
    }

    public Expression getExpression() {
        return expression;
    }

    public void setData(String data) throws Exception {
        if (ObjectUtils.equals(this.data, data))
            return;
        this.data = data;
        attribute.save();
        compileExpression();
    }
    
    private void compileExpression() throws ScriptException {
        expressionValid = true;
        if (data==null)
            expression = null;
        else {
            try {
                synchronized(this) {
                    expressionIdent = GroovyExpressionCompiler.convertToIdentificator(
                            "n"+attribute.getOwner().getId()+"_a"+attribute.getId());
                    expression = compiler.compile(data, GroovyExpressionCompiler.LANGUAGE, expressionIdent);
                }
            } catch(ScriptException e) {
                expressionValid = false;
                attribute.getOwner().getLogger().warn(String.format(
                        "Error compile expression (%s)", data), e);
                fireExpressionInvalidatedEvent(null);
                throw e;
            }
        }
    }        

    public String getData() {
        return data;
    }

    public Object handleData() 
    {
//        Object oldValue = value;
        Object res = null;
        if (expression!=null && expressionValid) {
            Bindings bindings = new SimpleBindings();
//            bindings.put(NODE_BINDING, new NodeAccessImpl(attribute.getOwner()));
            bindings.put(NODE_BINDING, attribute.getOwner());
            bindings.put(LOGGER, attribute.getOwner().getLogger());

            BindingSupport varsSupport = tree.getGlobalBindings(Tree.EXPRESSION_VARS_BINDINGS);
            boolean varsInitiated = varsSupport.contains(RAVEN_EXPRESSION_VARS_INITIATED_BINDING);
            boolean executedFromTemplate = false;
            if (!varsInitiated) {
                varsSupport.put(RAVEN_EXPRESSION_VARS_INITIATED_BINDING, true);
                varsSupport.put(RAVEN_EXPRESSION_VARS_BINDING, new HashMap());
                varsSupport.put(RAVEN_EXPRESSION_SOURCES_BINDINS, new HashMap());
            } else {
                Boolean param = (Boolean) varsSupport.get(EXECUTED_FROM_TEMPLATE_BINDING);
                executedFromTemplate = param==null? false : param;
                if (executedFromTemplate) 
                    varsSupport.put(EXECUTED_FROM_TEMPLATE_BINDING, false);
            }
            bindings.put(RAVEN_EXPRESSION_VARS_BINDING, varsSupport.get(RAVEN_EXPRESSION_VARS_BINDING));
            Map args = (Map) varsSupport.get(RAVEN_EXPRESSION_ARGS_BINDING);
            if (args!=null) {
                bindings.putAll(args);
                bindings.put(RAVEN_EXPRESSION_ARGS_BINDING, args);
                varsSupport.remove(RAVEN_EXPRESSION_ARGS_BINDING);
            } else 
                bindings.put(RAVEN_EXPRESSION_ARGS_BINDING, Collections.EMPTY_MAP);
            try {
                bindings.remove(ENABLE_SCRIPT_EXECUTION_BINDING);
                attribute.getOwner().formExpressionBindings(bindings);
                bindings.put(NODE_BINDING, attribute.getOwner());
                bindings.put(LOGGER, attribute.getOwner().getLogger());
                if (   !attribute.getValueHandlerType().equals(ScriptAttributeValueHandlerFactory.TYPE)
                    || bindings.containsKey(ENABLE_SCRIPT_EXECUTION_BINDING)
                    || varsInitiated)
                {
                    final HashMap<String, ExpressionInfo> sources = 
                            (HashMap<String, ExpressionInfo>)varsSupport.get(RAVEN_EXPRESSION_SOURCES_BINDINS);
                    try {
                        bindings.remove(ENABLE_SCRIPT_EXECUTION_BINDING);                        
                        sources.put(expressionIdent, 
                                    new ExpressionInfoImpl(attribute.getName(), attribute.getOwner(), data));
                        res = expression.eval(bindings);
                    } catch (Throwable ex) {
                        final Node owner = attribute.getOwner();
                        String mess = String.format(
                                "Exception in @%s (%s)"
                                , attribute.getName(), owner.getPath());
                        GroovyExpressionExceptionAnalyzator an = new GroovyExpressionExceptionAnalyzator(
                                expressionIdent, ex, 2);
                        GroovyExpressionException error = new GroovyExpressionException(mess, ex, an);
                        if (varsInitiated && !executedFromTemplate)
                            throw error;
                        else if (owner.isLogLevelEnabled(LogLevel.ERROR)) {
                            owner.getLogger().error(mess, ex);
                            varsSupport.put(EXECUTED_FROM_TEMPLATE_BINDING, false);
                            final AtomicInteger counter = new AtomicInteger(1);
                            String errMess = GroovyExpressionExceptionAnalyzator.aggregate(
                                    error, 
                                    sources,
                                    counter);
                            if (errMess==null || errMess.isEmpty())
                                owner.getLogger().error(mess, ex);
                            else
                                owner.getLogger().error(errMess, ex);
                            if (bindings.containsKey(BindingNames.PROPAGATE_EXPRESSION_EXCEPTION) || executedFromTemplate) {
                                String err = errMess==null || errMess.isEmpty()? mess : errMess;
                                throw new PropagatedAttributeValueError(err, ex, counter);
                            }
                        }
                    }
                }
            }finally{
                if (!varsInitiated)
                    varsSupport.reset();
            }
        }
//        if (   !attribute.getValueHandlerType().equals(ScriptAttributeValueHandlerFactory.TYPE)
//            && !ObjectUtils.equals(value, oldValue))
//        {
//            fireValueChangedEvent(oldValue, value);
//        }
        return res;
    }
    
    public void close() {
    }

    public boolean isReferenceValuesSupported() {
        return false;
    }

    public boolean isExpressionSupported() {
        return true;
    }

    public boolean isExpressionValid() {
        return expressionValid;
    }

    public void validateExpression() throws Exception {
        compileExpression();
    }
}

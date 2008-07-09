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

import java.util.logging.Level;
import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.raven.api.impl.NodeAccessImpl;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCompiler;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodePathResolver;
import org.raven.tree.impl.AbstractAttributeValueHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class ExpressionAttributeValueHandler extends AbstractAttributeValueHandler
{
    @Service
    private static ExpressionCompiler compiler;
    @Service
    private static NodePathResolver pathResolver;
    
    private final static Logger logger = 
            LoggerFactory.getLogger(ExpressionAttributeValueHandler.class);
    
    private boolean expressionValid = false;
    private String data;
    private Expression expression;
    private Object value;
    
    public ExpressionAttributeValueHandler(NodeAttribute attribute) 
    {
        super(attribute);
        try {
            data = attribute.getRawValue();
            compileExpression();
        } catch (ScriptException ex) {}
    }

    public void setData(String data) throws Exception 
    {
        if (ObjectUtils.equals(this.data, data))
            return;
        this.data = data;
        attribute.save();
        compileExpression();
    }
    
    private void compileExpression() throws ScriptException
    {
        expressionValid = true;
        if (data==null)
            expression = null;
        else
        {
            try
            {
                expression = compiler.compile(data, "groovy");
            }
            catch(ScriptException e)
            {
                expressionValid = false;
                fireExpressionInvalidatedEvent(value);
                throw e;
            }
        }
    }        

    public String getData() {
        return data;
    }

    public Object handleData() 
    {
        Object oldValue = value;
        value = null;
        if (expression!=null && expressionValid)
        {
            Bindings bindings = new SimpleBindings();
            bindings.put("node", new NodeAccessImpl(attribute.getOwner()));
            attribute.getOwner().formExpressionBindings(bindings);
            try {
                value = expression.eval(bindings);
            } catch (ScriptException ex) {
                logger.warn(String.format(
                        "Attribute (%s) getValue error. Error executing expression (%s). %s"
                        , pathResolver.getAbsolutePath(attribute), data, ex.getMessage()));
            }
        }
        if (!ObjectUtils.equals(value, oldValue))
            fireValueChangedEvent(oldValue, value);
        return value;
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

    public void validateExpression() throws Exception 
    {
        compileExpression();
    }
}

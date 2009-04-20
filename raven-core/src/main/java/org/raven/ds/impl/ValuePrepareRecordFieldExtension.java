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

package org.raven.ds.impl;

import javax.script.Bindings;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.raven.util.BindingSupport;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class ValuePrepareRecordFieldExtension extends BaseNode
{
    public final static String EXPRESSION_ATTRIBUTE = "expression";

    @Parameter
    private Class convertToType;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean useExpression;

    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE)
    private String expression;

    private BindingSupport bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        
        bindingSupport = new BindingSupport();
    }

    public Class getConvertToType()
    {
        return convertToType;
    }

    public void setConvertToType(Class convertToType)
    {
        this.convertToType = convertToType;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    public Boolean getUseExpression()
    {
        return useExpression;
    }

    public void setUseExpression(Boolean useExpression)
    {
        this.useExpression = useExpression;
    }

    public Object prepareValue(Object value, Bindings bindings)
    {
        Class _convertToType = convertToType;
        if (_convertToType!=null)
            value = converter.convert(_convertToType, value, null);

        if (useExpression)
        {
            bindingSupport.put("value", value);
            if (bindings!=null)
                bindingSupport.putAll(bindings);
            try
            {
                value = getNodeAttribute(EXPRESSION_ATTRIBUTE).getRealValue();
            }
            finally
            {
                bindingSupport.reset();
            }
        }
        return value;
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);

        bindingSupport.addTo(bindings);
    }
}

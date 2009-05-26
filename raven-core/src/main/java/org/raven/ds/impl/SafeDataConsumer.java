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

import java.util.Collection;
import javax.script.Bindings;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.expr.impl.BindingSupportImpl;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class SafeDataConsumer extends BaseNode implements DataConsumer
{
    public static final String DATA_BINDING = "data";
    public static final String EXPRESSION_ATTRIBUTE = "expression";

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;

    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE)
    private String expression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useExpression;

    protected BindingSupportImpl bindingSupport;
    protected ThreadLocal data;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
        data = new ThreadLocal();
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
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

    public void setData(DataSource dataSource, Object data)
    {
        if (useExpression)
        {
            bindingSupport.put(DATA_BINDING, data);
            try
            {
                NodeAttribute exprAttr = getNodeAttribute(EXPRESSION_ATTRIBUTE);
                data = exprAttr.getRealValue();
            }
            finally
            {
                bindingSupport.reset();
            }
        }
        this.data.set(data);
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes)
    {
        try
        {
            dataSource.getDataImmediate(this, sessionAttributes);
            return data.get();
        }
        finally
        {
            data.remove();
        }
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

}

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
import java.util.LinkedList;
import java.util.List;
import javax.script.Bindings;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class SafeDataConsumer extends BaseNode implements DataConsumer
{
    public static final String DATA_BINDING = "data";
    public static final String EXPRESSION_ATTRIBUTE = "expression";
    public static final String DATA_CONTEXT_BINDING = "context";

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String expression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useExpression;

    protected BindingSupportImpl bindingSupport;
//    protected ThreadLocal data;
    protected ThreadLocal<List> data;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
        data = new ThreadLocal<List>();
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

    public void setData(DataSource dataSource, Object data, DataContext context)
    {
        if (useExpression)
        {
            bindingSupport.put(DATA_BINDING, data);
            bindingSupport.put(DATA_CONTEXT_BINDING, context);
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
        if (this.data.get()==null)
            this.data.set(new LinkedList());
        this.data.get().add(data);
    }

    /**
     * Returns the list of the data recieved from the data source or null if no data was send by
     * the data source.
     * @param sessionAttributes the set of the session attributes
     */
    public Object refereshData(Collection<NodeAttribute> sessionAttributes)
    {
        try
        {
            data.remove();
            dataSource.getDataImmediate(this, new DataContextImpl(sessionAttributes));
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

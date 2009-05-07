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
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordGeneratorNode.class)
public class DataSourceRecordFieldValueGenerator
        extends AbstractFieldValueGenerator implements DataConsumer
{
    public final static String DATASOURCE_ATTRIBUTE = "dataSource";
    public static final String DATA_BINDING = "data";
    public static final String EXPRESSION_ATTRIBUTE = "expression";

    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @NotNull
    private DataSource dataSource;

    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE)
    private String expression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useExpression;

    private ThreadLocal value;

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

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    protected void initFields()
    {
        super.initFields();
        value = new ThreadLocal();
    }

    public void setData(DataSource dataSource, Object data)
    {
        value.set(data);
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Object doGetFieldValue(Map<String, NodeAttribute> sessionAttributes)
    {
        dataSource.getDataImmediate(
                this, sessionAttributes==null? null : sessionAttributes.values());
        Object val = value.get();
        value.remove();
        if (useExpression)
        {
            bindingSupport.put(DATA_BINDING, val);
            val = getNodeAttribute(EXPRESSION_ATTRIBUTE).getRealValue();
        }
        return val;
    }
}

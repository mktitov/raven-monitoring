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

package org.raven.ds.impl;

import java.util.Collection;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataPipe;
import org.raven.ds.DataSource;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.Description;

/**
 * Collects data from one data source and transmits it to all {@link DataConsumer data consumers} 
 * in the {@link org.raven.tree.Node#getDependentNodes()  dependency list}.
 * @author Mikhail Titov
 */
@NodeClass()
@Description(
    "The node takes data from data source and gives data to back to the data consumers " +
    "connected to this node")
public class DataPipeImpl extends AbstractDataConsumer implements DataPipe
{
    public final static String CONVERT_VALUE_TO_TYPE_ATTRIBUTE = "convertValueToType";
    public final static String EXPRESSION_ATTRIBUTE = "expression";
    public final static String DATA_ATTRIBUTE = "data";
    
    @Parameter()
    @Description(
        "Allows to convert data to the selected type before sending data to the data consumers")
    private Class convertValueToType;
    
    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE)
    @Description(
        "Allows to transform data to another data using this expression before sending data to " +
        "consumers. Expression executes after data converting (see convertValueToType parameter)")
    private String expression;

    @Parameter(defaultValue="false")
    private Boolean skipFirstCycle;

    public Boolean getSkipFirstCycle()
    {
        return skipFirstCycle;
    }

    public void setSkipFirstCycle(Boolean skipFirstCycle)
    {
        this.skipFirstCycle = skipFirstCycle;
    }


    @Override
    protected void doSetData(DataSource dataSource, Object data)
    {
        if (skipFirstCycle && getPreviuosDataTimeMillis()==0)
            return;
        Class convertTo = convertValueToType;
        if (convertTo!=null)
            this.data = converter.convert(convertTo, this.data, null);
        Object newData = this.data;
        NodeAttribute attr = getNodeAttribute(EXPRESSION_ATTRIBUTE);
        if (attr.getRawValue()!=null && attr.getRawValue().trim().length()>0)
            newData = attr.getRealValue();
        
        if (getDependentNodes()!=null)
            for (Node node: getDependentNodes())
                if (node.getStatus()==Status.STARTED && node instanceof DataConsumer)
                    ((DataConsumer)node).setData(this, newData);
    }

    public boolean getDataImmediate(
            DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes)
    {
        return getDataSource().getDataImmediate(this, sessionAttributes);
    }

    @Override
    public void formExpressionBindings(Bindings bindings) 
    {
        super.formExpressionBindings(bindings);
        bindings.put("lastData", data);
        bindings.put("previousData", getPreviousData());
        bindings.put("lastDataTimeMillis", getLastDataTimeMillis());
        bindings.put("previousDataTimeMillis", getPreviuosDataTimeMillis());
        bindings.put("timeDiffMillis", getLastDataTimeMillis()-getPreviuosDataTimeMillis());
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }

    public Class getConvertValueToType() 
    {
        return convertValueToType;
    }

    public Object getValue() 
    {
        return data;
    }

    public String getExpression() {
        return expression;
    }
    
}

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
import org.raven.ds.DataContext;
import org.raven.ds.DataPipe;
import org.raven.ds.DataSource;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.expr.impl.BindingSupportImpl;

/**
 * Collects data from one data source and transmits it to all {@link DataConsumer data consumers} 
 * in the {@link org.raven.tree.Node#getDependentNodes()  dependency list}.
 * @author Mikhail Titov
 */
@NodeClass()
//@Description(
//    "The node takes data from data source and gives data to back to the data consumers " +
//    "connected to this node")
public class DataPipeImpl extends AbstractDataConsumer implements DataPipe
{
    public final static String CONVERT_VALUE_TO_TYPE_ATTRIBUTE = "convertValueToType";
    public static final String DATA_CONTEXT_BINDING = "context";
    public final static String EXPRESSION_ATTRIBUTE = "expression";
    public final static String DATA_ATTRIBUTE = "data";
    
    @Parameter()
//    @Description(
//        "Allows to convert data to the selected type before sending data to the data consumers")
    private Class convertValueToType;
    
    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE)
//    @Description(
//        "Allows to transform data to another data using this expression before sending data to " +
//        "consumers. Expression executes after data converting (see convertValueToType parameter)")
    private String expression;

    @Parameter(defaultValue="false")
    private Boolean skipFirstCycle;

    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();

        bindingSupport = new BindingSupportImpl();
    }

    public Boolean getSkipFirstCycle()
    {
        return skipFirstCycle;
    }

    public void setSkipFirstCycle(Boolean skipFirstCycle)
    {
        this.skipFirstCycle = skipFirstCycle;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context)
    {
        try
        {
            Class convertTo = convertValueToType;
            if (convertTo!=null)
                this.data = converter.convert(convertTo, this.data, null);
            bindingSupport.put("lastData", this.data);
            bindingSupport.put("previousData", getPreviousData());
            bindingSupport.put("lastDataTimeMillis", getLastDataTimeMillis());
            bindingSupport.put("previousDataTimeMillis", getPreviuosDataTimeMillis());
            bindingSupport.put(
                    "timeDiffMillis", getLastDataTimeMillis()-getPreviuosDataTimeMillis());
            bindingSupport.put(DATA_CONTEXT_BINDING, context);
            if (skipFirstCycle && getPreviuosDataTimeMillis()==0)
                return;
            Object newData = this.data;
            NodeAttribute attr = getNodeAttribute(EXPRESSION_ATTRIBUTE);
            if (attr.getRawValue()!=null && attr.getRawValue().trim().length()>0)
                newData = attr.getRealValue();

            if (getDependentNodes()!=null)
                for (Node node: getDependentNodes())
                    if (node.getStatus()==Status.STARTED && node instanceof DataConsumer)
                        sendDataToConsumer((DataConsumer)node, newData, context);
        }
        finally
        {
            bindingSupport.reset();
        }
    }

	protected void sendDataToConsumer(DataConsumer consumer, Object data, DataContext context)
	{
		consumer.setData(this, data, context);
	}

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        return getDataSource().getDataImmediate(this, context);
    }

    @Override
    public void formExpressionBindings(Bindings bindings) 
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
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

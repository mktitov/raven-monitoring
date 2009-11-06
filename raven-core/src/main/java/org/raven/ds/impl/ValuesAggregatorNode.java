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

import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;
import org.raven.annotations.Parameter;
import org.raven.ds.AggregateFunction;
import org.raven.ds.AggregateFunctionType;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class ValuesAggregatorNode extends BaseNode implements AggregateFunction
{
    public static final String AGGREGATION_EXPRESSION_ATTR = "aggregationExpression";
    public static final String COUNTER_BINDING = "counter";
    public static final String FINISH_AGGREGATION_EXPRESSION_ATTR = "finishAggregationExpression";
    public static final String NEXTVALUE_BINDING = "nextValue";
    public static final String PARAMS_BINDING = "params";
    public static final String START_AGGREGATION_EXPRESSION_ATTR = "startAggregationExpression";
    public static final String VALUE_BINDING = "value";

    @NotNull @Parameter
    private AggregateFunctionType aggregateFunction;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String aggregationExpression;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String startAggregationExpression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useStartAggregationExpression;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String finishAggregationExpression;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean useFinishAggregationExpression;

    private ThreadLocal<Value> valueContainer;
    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
        valueContainer = new ThreadLocal<Value>();
    }

    public void startAggregation() 
    {
        Value value = new Value();
        valueContainer.set(value);
        if (useStartAggregationExpression)
        {
            bindingSupport.put(PARAMS_BINDING, value.params);
            try{
                value.setValue(getNodeAttribute(START_AGGREGATION_EXPRESSION_ATTR).getRealValue());
            }finally{
                bindingSupport.reset();
            }
        }
    }

    public void aggregate(Object nextValue)
    {
        Value value = this.valueContainer.get();
        value.incCounter();
        bindingSupport.put(VALUE_BINDING, value.getValue());
        bindingSupport.put(COUNTER_BINDING, value.getCounter());
        bindingSupport.put(NEXTVALUE_BINDING, nextValue);
        bindingSupport.put(PARAMS_BINDING, value.params);
        try
        {
            value.setValue(getNodeAttribute(AGGREGATION_EXPRESSION_ATTR).getRealValue());
        }
        finally
        {
            bindingSupport.reset();
        }
    }

    public void finishAggregation() 
    {
        if (!useFinishAggregationExpression)
            return;
        Value value = this.valueContainer.get();
        bindingSupport.put(VALUE_BINDING, value.getValue());
        bindingSupport.put(COUNTER_BINDING, value.getCounter());
        bindingSupport.put(PARAMS_BINDING, value.params);
        try
        {
            value.setValue(getNodeAttribute(FINISH_AGGREGATION_EXPRESSION_ATTR).getRealValue());
        }
        finally
        {
            value.incCounter();
            bindingSupport.reset();
        }
    }

    public Object getAggregatedValue() 
    {
        return valueContainer.get().getValue();
    }

    public void close()
    {
        valueContainer.remove();
    }

    public AggregateFunction createAggregateFunction() throws Exception
    {
        AggregateFunction func = null;
        switch(aggregateFunction)
        {
            case AVG : func = new AvgAggregateFunction(); break;
            case COUNT : func = new CountAggregateFunction(); break;
            case MAX : func = new MaxAggregateFunction(); break;
            case MIN : func = new MinAggregateFunction(); break;
            case SUM : func = new SumAggregateFunction(); break;
            case CUSTOM : func = this; break;
            default:
                throw new Exception(String.format(
                        "Undefned aggregate function (%s)", aggregateFunction));
        }
        return func;
    }
    
    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public AggregateFunctionType getAggregateFunction()
    {
        return aggregateFunction;
    }

    public void setAggregateFunction(AggregateFunctionType aggregateFunction)
    {
        this.aggregateFunction = aggregateFunction;
    }

    public String getAggregationExpression()
    {
        return aggregationExpression;
    }

    public void setAggregationExpression(String aggregationExpression)
    {
        this.aggregationExpression = aggregationExpression;
    }

    public String getFinishAggregationExpression()
    {
        return finishAggregationExpression;
    }

    public void setFinishAggregationExpression(String finishAggregationExpression)
    {
        this.finishAggregationExpression = finishAggregationExpression;
    }

    public String getStartAggregationExpression()
    {
        return startAggregationExpression;
    }

    public void setStartAggregationExpression(String startAggregationExpression)
    {
        this.startAggregationExpression = startAggregationExpression;
    }

    public Boolean getUseFinishAggregationExpression()
    {
        return useFinishAggregationExpression;
    }

    public void setUseFinishAggregationExpression(Boolean useFinishAggregationExpression)
    {
        this.useFinishAggregationExpression = useFinishAggregationExpression;
    }

    public Boolean getUseStartAggregationExpression()
    {
        return useStartAggregationExpression;
    }

    public void setUseStartAggregationExpression(Boolean useStartAggregationExpression)
    {
        this.useStartAggregationExpression = useStartAggregationExpression;
    }

    private static class Value
    {
        private int counter=0;
        private Object value;
        private Map<String, Object> params = new HashMap<String, Object>();

        public void incCounter()
        {
            ++counter;
        }

        public int getCounter()
        {
            return counter;
        }

        public void setCounter(int counter)
        {
            this.counter = counter;
        }

        public Object getValue()
        {
            return value;
        }

        public void setValue(Object value)
        {
            this.value = value;
        }
    }
}
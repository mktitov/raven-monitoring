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

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.AggregateFunctionType;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordsAggregatorNode.class)
public class RecordsAggregatorValueFieldNode extends RecordsAggregatorField
{
    @NotNull @Parameter
    private AggregateFunctionType aggregateFunction;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object finishAggregationExpression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useFinishAggregationExpression;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object startAggregationExpression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useStartAggregationExpression;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object aggregationExpression;

    public AggregateFunctionType getAggregateFunction()
    {
        return aggregateFunction;
    }

    public void setAggregateFunction(AggregateFunctionType aggregateFunction)
    {
        this.aggregateFunction = aggregateFunction;
    }

    public Object getFinishAggregationExpression() {
        return finishAggregationExpression;
    }

    public void setFinishAggregationExpression(Object finishAggregationExpression) {
        this.finishAggregationExpression = finishAggregationExpression;
    }

    public Object getStartAggregationExpression() {
        return startAggregationExpression;
    }

    public void setStartAggregationExpression(Object startAggregationExpression) {
        this.startAggregationExpression = startAggregationExpression;
    }

    public Boolean getUseFinishAggregationExpression() {
        return useFinishAggregationExpression;
    }

    public void setUseFinishAggregationExpression(Boolean useFinishAggregationExpression) {
        this.useFinishAggregationExpression = useFinishAggregationExpression;
    }

    public Boolean getUseStartAggregationExpression() {
        return useStartAggregationExpression;
    }

    public void setUseStartAggregationExpression(Boolean useStartAggregationExpression) {
        this.useStartAggregationExpression = useStartAggregationExpression;
    }

    public Object getAggregationExpression()
    {
        return aggregationExpression;
    }

    public void setAggregationExpression(Object aggregationExpression)
    {
        this.aggregationExpression = aggregationExpression;
    }
}

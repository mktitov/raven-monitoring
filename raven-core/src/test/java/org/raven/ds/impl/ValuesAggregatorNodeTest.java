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

import org.junit.Before;
import org.junit.Test;
import org.raven.ds.AggregateFunction;
import org.raven.ds.AggregateFunctionType;
import org.raven.log.LogLevel;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class ValuesAggregatorNodeTest extends RavenCoreTestCase
{
    private ValuesAggregatorNode agg;

    @Before
    public void prepare()
    {
        agg = new ValuesAggregatorNode();
        agg.setName("values aggregator");
        tree.getRootNode().addAndSaveChildren(agg);
        agg.setLogLevel(LogLevel.TRACE);
    }

    @Test
    public void createSumFunctionTest() throws Exception
    {
        checkFunctionCreation(AggregateFunctionType.SUM, SumAggregateFunction.class);
    }

    @Test
    public void createCountFunctionTest() throws Exception
    {
        checkFunctionCreation(AggregateFunctionType.COUNT, CountAggregateFunction.class);
    }

    @Test
    public void createMinFunctionTest() throws Exception
    {
        checkFunctionCreation(AggregateFunctionType.MIN, MinAggregateFunction.class);
    }

    @Test
    public void createMaxFunctionTest() throws Exception
    {
        checkFunctionCreation(AggregateFunctionType.MAX, MaxAggregateFunction.class);
    }

    @Test
    public void customFunctionTest() throws Exception
    {
        agg.setAggregationExpression("nextValue+(value?:0)+counter");
        AggregateFunction func = checkFunctionCreation(
                AggregateFunctionType.CUSTOM, ValuesAggregatorNode.CustomAggregator.class);
        func.startAggregation();
        func.aggregate(1);
        func.aggregate(2);
        func.finishAggregation();
        Object val = func.getAggregatedValue();
        func.close();
        assertEquals(new Integer(6), val);
        func.close();
    }

    @Test
    public void customFunctionWithStartAndFinishExpressionsTest() throws Exception
    {
        agg.setUseStartAggregationExpression(Boolean.TRUE);
        agg.setStartAggregationExpression("params.p1=100; 10");
        agg.setUseFinishAggregationExpression(Boolean.TRUE);
        agg.setFinishAggregationExpression("value+counter+params.p1");
        agg.setAggregationExpression("nextValue+value");
        AggregateFunction func = checkFunctionCreation(
                AggregateFunctionType.CUSTOM, ValuesAggregatorNode.CustomAggregator.class);
        func.startAggregation();
        func.aggregate(1);
        func.aggregate(2);
        func.finishAggregation();
        Object val = func.getAggregatedValue();
        func.close();
        assertEquals(new Integer(115), val);
        func.close();
    }

    private AggregateFunction checkFunctionCreation(
            AggregateFunctionType aggregateFunctionType, Class functionClass)
        throws Exception
    {
        agg.setAggregateFunction(aggregateFunctionType);
        assertTrue(agg.start());
        AggregateFunction func = agg.createAggregateFunction();
        assertNotNull(func);
        assertTrue(functionClass.isAssignableFrom(func.getClass()));

        return func;
    }
}
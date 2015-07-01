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

package org.raven.statdb.impl;

import javax.script.SimpleBindings;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.statdb.query.SelectEntry;
import org.raven.statdb.query.StatisticsValues;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class AggregationCalculationUnitTest extends RavenCoreTestCase
{
    @Test
    public void checkAndCreate_noAggregations_test() throws Exception
    {
        SelectEntry selectEntry = createMock(SelectEntry.class);
        expect(selectEntry.getExpression()).andReturn("1+1");
        replay(selectEntry);
        
        AggregationCalculationUnit aggs = AggregationCalculationUnit.checkAndCreate(selectEntry);
        assertNull(aggs);

        verify(selectEntry);
    }

    @Test
    public void checkAndCreate_oneAggregation_test() throws Exception
    {
        SelectEntry selectEntry = createMock(SelectEntry.class);
        expect(selectEntry.getExpression()).andReturn("$sum{name}");
        expect(selectEntry.getName()).andReturn("entry1");
        replay(selectEntry);

        AggregationCalculationUnit aggs = AggregationCalculationUnit.checkAndCreate(selectEntry);
        assertNotNull(aggs);

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("name", 1);
        aggs.calculate(bindings);
        bindings.put("name", 2.);
        aggs.calculate(bindings);

        StatisticsValues statValues = aggs.getStatisticsValues();
        assertNotNull(statValues);
        assertNotNull("entry1", statValues.getStatisticsName());
        assertNotNull(statValues.getValues());
        assertEquals(1, statValues.getValues().length);

        assertEquals(3., statValues.getValues()[0], 0.);

        verify(selectEntry);
    }

    @Test
    public void checkAndCreate_compoundAggregationExpression_test() throws Exception
    {
        SelectEntry selectEntry = createMock(SelectEntry.class);
        expect(selectEntry.getExpression()).andReturn("1 +$sum{name+1} + $average{name} -0.5");
        expect(selectEntry.getName()).andReturn("entry1");
        replay(selectEntry);

        AggregationCalculationUnit aggs = AggregationCalculationUnit.checkAndCreate(selectEntry);
        assertNotNull(aggs);

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("name", 1);
        aggs.calculate(bindings);
        bindings.put("name", 2.);
        aggs.calculate(bindings);

        StatisticsValues statValues = aggs.getStatisticsValues();
        assertNotNull(statValues);
        assertNotNull("entry1", statValues.getStatisticsName());
        assertNotNull(statValues.getValues());
        assertEquals(1, statValues.getValues().length);

        assertEquals(7., statValues.getValues()[0], 0.);

        verify(selectEntry);
    }

    @Test
    public void checkAndCreate_reset_test() throws Exception
    {
        SelectEntry selectEntry = createMock(SelectEntry.class);
        expect(selectEntry.getExpression()).andReturn("$sum{name}");
        expect(selectEntry.getName()).andReturn("entry1");
        replay(selectEntry);

        AggregationCalculationUnit aggs = AggregationCalculationUnit.checkAndCreate(selectEntry);
        assertNotNull(aggs);

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("name", 1);
        aggs.calculate(bindings);
        bindings.put("name", 2.);
        aggs.calculate(bindings);

        StatisticsValues statValues = aggs.getStatisticsValues();
        assertEquals(3., statValues.getValues()[0], 0.);

        aggs.reset();
        bindings.put("name", 1);
        aggs.calculate(bindings);
        statValues = aggs.getStatisticsValues();
        assertEquals(1., statValues.getValues()[0], 0.);

        verify(selectEntry);
    }
}
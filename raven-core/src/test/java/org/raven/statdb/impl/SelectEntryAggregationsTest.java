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
import org.raven.RavenCoreTestCase;
import org.raven.statdb.query.SelectEntry;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class SelectEntryAggregationsTest extends RavenCoreTestCase
{
    @Test
    public void checkAndCreate_noAggregations_test() throws Exception
    {
        SelectEntry selectEntry = createMock(SelectEntry.class);
        expect(selectEntry.getExpression()).andReturn("1+1");
        replay(selectEntry);
        
        SelectEntryAggregations aggs = SelectEntryAggregations.checkAndCreate(selectEntry);
        assertNull(aggs);

        verify(selectEntry);
    }

    @Test
    public void checkAndCreate_oneAggregation_test() throws Exception
    {
        SelectEntry selectEntry = createMock(SelectEntry.class);
        expect(selectEntry.getExpression()).andReturn("$sum{name}");
        replay(selectEntry);

        SelectEntryAggregations aggs = SelectEntryAggregations.checkAndCreate(selectEntry);
        assertNotNull(aggs);
        assertNotNull(aggs.getAggregations());
        assertEquals(1, aggs.getAggregations().length);
        SelectEntryAggregation agg = aggs.getAggregations()[0];
        assertTrue(agg instanceof SelectEntryAggregation);

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("name", 1);
        aggs.aggregate(bindings);
        bindings.put("name", 2.);
        aggs.aggregate(bindings);

        assertEquals(3., (Double)aggs.eval(), 0.);

        verify(selectEntry);
    }

    @Test
    public void checkAndCreate_compoundAggregationExpression_test() throws Exception
    {
        SelectEntry selectEntry = createMock(SelectEntry.class);
        expect(selectEntry.getExpression()).andReturn("1 +$sum{name+1} + $average{name} -0.5");
        replay(selectEntry);

        SelectEntryAggregations aggs = SelectEntryAggregations.checkAndCreate(selectEntry);
        assertNotNull(aggs);
        assertNotNull(aggs.getAggregations());
        assertEquals(2, aggs.getAggregations().length);
        SelectEntryAggregation agg = aggs.getAggregations()[0];
        assertNotNull(aggs.getAggregations()[0]);
        assertNotNull(aggs.getAggregations()[1]);

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("name", 1);
        aggs.aggregate(bindings);
        bindings.put("name", 2.);
        aggs.aggregate(bindings);

        assertEquals(7., (Double)aggs.eval(), 0.);

        verify(selectEntry);
    }

    @Test
    public void checkAndCreate_reset_test() throws Exception
    {
        SelectEntry selectEntry = createMock(SelectEntry.class);
        expect(selectEntry.getExpression()).andReturn("$sum{name}");
        replay(selectEntry);

        SelectEntryAggregations aggs = SelectEntryAggregations.checkAndCreate(selectEntry);
        assertNotNull(aggs);
        assertNotNull(aggs.getAggregations());
        assertEquals(1, aggs.getAggregations().length);
        SelectEntryAggregation agg = aggs.getAggregations()[0];
        assertTrue(agg instanceof SelectEntryAggregation);

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("name", 1);
        aggs.aggregate(bindings);
        bindings.put("name", 2.);
        aggs.aggregate(bindings);

        assertEquals(3., (Double)aggs.eval(), 0.);

        aggs.reset();
        bindings.put("name", 1);
        aggs.aggregate(bindings);
        assertEquals(1., (Double)aggs.eval(), 0.);

        verify(selectEntry);
    }
}
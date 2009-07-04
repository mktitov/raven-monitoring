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

import javax.script.Bindings;
import javax.script.ScriptException;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.expr.Expression;
import static org.easymock.EasyMock.*;
import org.raven.statdb.query.CalculationUnitException;
import org.raven.statdb.query.StatisticsValues;

/**
 *
 * @author Mikhail Titov
 */
public class ConstantAggregationCalculationUnitTest extends RavenCoreTestCase
{
    @Test
    public void test() throws ScriptException, CalculationUnitException
    {
        Expression expr = createMock(Expression.class);
        expect(expr.eval(isA(Bindings.class))).andReturn(1);
        replay(expr);

        ConstantAggregationCalculationUnit cunit =
                new ConstantAggregationCalculationUnit("entry", expr);
        StatisticsValues values = cunit.getStatisticsValues();
        assertNotNull(values);
        assertEquals("entry", values.getStatisticsName());
        assertEquals(1, values.getValues().length);
        assertEquals(1., values.getValues()[0], 0.);

        verify(expr);
    }
}
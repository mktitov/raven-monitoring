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

import java.util.Arrays;
import javax.script.Bindings;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.expr.Expression;
import org.raven.statdb.query.StatisticsValues;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class ExpressionCalculationUnitTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        Expression expression = createMock(Expression.class);
        Bindings bindings = createMock(Bindings.class);

        expect(expression.eval(bindings)).andReturn(1.);
        expect(expression.eval(bindings)).andReturn(2.);
        expect(expression.eval(bindings)).andReturn(3.);
        expect(expression.eval(bindings)).andReturn(4.);

        replay(expression, bindings);

        ExpressionCalculationUnit cunit = new ExpressionCalculationUnit(expression, "entry", 2);
        cunit.calculate(bindings);
        cunit.calculate(bindings);
        
        StatisticsValues values = cunit.getStatisticsValues();
        assertNotNull(values);
        assertEquals("entry", values.getStatisticsName());
        assertNotNull(values.getValues());
        assertEquals(2, values.getValues().length);
        assertTrue(Arrays.equals(new double[]{1., 2.}, values.getValues()));

        cunit.reset();
        
        cunit.calculate(bindings);
        cunit.calculate(bindings);
        values = cunit.getStatisticsValues();
        assertNotNull(values);
        assertEquals("entry", values.getStatisticsName());
        assertNotNull(values.getValues());
        assertEquals(2, values.getValues().length);
        assertTrue(Arrays.equals(new double[]{3., 4.}, values.getValues()));

        verify(expression, bindings);
    }

}
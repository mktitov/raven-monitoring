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
import javax.script.SimpleBindings;
import org.raven.expr.Expression;
import org.raven.statdb.query.CalculationUnitException;
import org.raven.statdb.query.SelectEntryCalculationUnit;
import org.raven.statdb.query.StatisticsValues;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class ConstantAggregationCalculationUnit implements SelectEntryCalculationUnit
{
    private final StatisticsValues values;

    @Service
    private static TypeConverter converter;

    public ConstantAggregationCalculationUnit(String selectEntryName, Expression expression)
            throws CalculationUnitException
    {
        try
        {
            Object val = expression.eval(new SimpleBindings());
            Double doubleVal = converter.convert(Double.class, val, null);
            values = new StatisticsValuesImpl(selectEntryName, new double[]{doubleVal});
        }
        catch(Exception e)
        {
            throw new CalculationUnitException(String.format(
                    "Error creating statistics values for select entry (%s)", selectEntryName)
                    ,e);
        }
    }

    public void calculate(Bindings bindings) throws CalculationUnitException
    {
    }

    public void reset()
    {
    }

    public StatisticsValues getStatisticsValues() throws CalculationUnitException
    {
        return values;
    }
}

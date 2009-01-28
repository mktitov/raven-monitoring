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
public class ExpressionCalculationUnit implements SelectEntryCalculationUnit
{
    private final Expression expression;
    private final String selectEntryName;

    @Service
    private static TypeConverter converter;

    private double[] values;
    private int valuePos;

    public ExpressionCalculationUnit(Expression expression, String selectEntryName, int valueCount)
    {
        this.expression = expression;
        this.selectEntryName = selectEntryName;
        values = new double[valueCount];
        reset();
    }

    public void calculate(Bindings bindings) throws CalculationUnitException
    {
        try
        {
            Object val = expression.eval(bindings);
            Double doubleVal = converter.convert(Double.class, val, null);
            values[valuePos++] = doubleVal;
        }
        catch (Exception ex)
        {
            throw new CalculationUnitException(String.format(
                    "Error calculating value for select entry (%s)", selectEntryName), ex);
        }
    }

    public void reset()
    {
        Arrays.fill(values, Double.NaN);
        valuePos = 0;
    }

    public StatisticsValues getStatisticsValues() throws CalculationUnitException
    {
        return new StatisticsValuesImpl(selectEntryName, values);
    }
    
}

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

import org.raven.statdb.query.CalculationUnitException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCompiler;
import org.raven.expr.impl.GroovyExpressionCompiler;
import org.raven.statdb.AggregationFunction;
import org.raven.statdb.query.SelectEntry;
import org.raven.statdb.query.SelectEntryCalculationUnit;
import org.raven.statdb.query.StatisticsValues;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class AggregationCalculationUnit implements SelectEntryCalculationUnit
{
	private static Pattern pattern = Pattern.compile("\\$(\\w+)\\{(.*?)\\}");
    private static String AGGREGATION_PREFIX = "rm_agg_";

    private final Expression expression;
    private final SelectEntryAggregation[] aggregations;
    private final String selectEntryName;

	@Service
	private static ExpressionCompiler expressionCompiler;
	@Service
	private static TypeConverter converter;

	/**
	 * Создает и возвращает экземпляр {@link SelectEntryAggregations} если в выражении присутствуют
	 * агрегатные функции, иначе функция возвращает <code>null</code>
	 * @param selectEntryExpression
	 */
	public static AggregationCalculationUnit checkAndCreate(SelectEntry selectEntry)
			throws Exception
	{
		Matcher m = pattern.matcher(selectEntry.getExpression());
		StringBuffer resExpression = new StringBuffer();
		int i=0;
		List<SelectEntryAggregation> entryAggrgations = null;
		while (m.find())
		{
			AggregationFunction func = null;
			String funcName = m.group(1).toUpperCase();
			try
			{
				func = AggregationFunction.valueOf(funcName);
			}
			catch (IllegalArgumentException e)
			{
				throw new Exception(
						String.format("Invalid aggregation function name (%s)", funcName));
			}
			String aggExpression = m.group(2);
			Expression expr = expressionCompiler.compile(
					aggExpression, GroovyExpressionCompiler.LANGUAGE, null);

			if (entryAggrgations==null)
			{
				entryAggrgations = new ArrayList<SelectEntryAggregation>(2);
			}
			entryAggrgations.add(new SelectEntryAggregation(func, expr, converter));

			m.appendReplacement(resExpression, AGGREGATION_PREFIX+(++i));
		}

		if (entryAggrgations!=null)
		{
			//Создаем calculation unit
            m.appendTail(resExpression);
            Expression entryExpression = expressionCompiler.compile(
                    resExpression.toString(), GroovyExpressionCompiler.LANGUAGE, null);
            SelectEntryAggregation[] aggArr = new SelectEntryAggregation[entryAggrgations.size()];
            entryAggrgations.toArray(aggArr);

            return new AggregationCalculationUnit(entryExpression, aggArr, selectEntry.getName());
		}
        else
            return null;
	}

    public AggregationCalculationUnit(
            Expression expression, SelectEntryAggregation[] aggregations, String selectEntryName)
    {
        this.expression = expression;
        this.aggregations = aggregations;
        this.selectEntryName = selectEntryName;
    }


    public void calculate(Bindings bindings) throws CalculationUnitException
    {
        try
        {
            for (SelectEntryAggregation aggregation: aggregations)
                aggregation.aggregate(bindings);
        }
        catch(ScriptException e)
        {
            throw new CalculationUnitException(
                    String.format("Aggregation error in the (%s) select entry", selectEntryName)
                    , e);
        }
    }

    public void reset()
    {
        for (SelectEntryAggregation aggregation: aggregations)
            aggregation.reset();
    }

    public StatisticsValues getStatisticsValues() throws CalculationUnitException
    {
        SimpleBindings bindings = new SimpleBindings();
        for (int i=0; i<aggregations.length; ++i)
            bindings.put(AGGREGATION_PREFIX+(i+1), aggregations[i].getValue());
        try
        {
            Object value = expression.eval(bindings);
            Double doubleVal = converter.convert(Double.class, value, null);

            return new StatisticsValuesImpl(selectEntryName, new double[]{doubleVal});
        }
        catch (Exception ex)
        {
            throw new CalculationUnitException(String.format(
                    "Error executing aggregation expresion for the select entry (%s)"
                        , selectEntryName)
                    , ex);
        }
    }

}

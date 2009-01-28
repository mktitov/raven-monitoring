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
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class SelectEntryAggregations
{
	private static Pattern pattern = Pattern.compile("\\$(\\w+)\\{(.*?)\\}");
    private static String AGGREGATION_PREFIX = "rm_agg_";

    private final Expression expression;
    private final SelectEntryAggregation[] aggregations;
    private final SelectEntry selectEntry;

	@Service
	private static ExpressionCompiler expressionCompiler;
	@Service
	private static TypeConverter converter;

	/**
	 * Создает и возвращает экземпляр {@link SelectEntryAggregations} если в выражении присутствуют
	 * агрегатные функции, иначе функция возвращает <code>null</code>
	 * @param selectEntryExpression
	 */
	public static SelectEntryAggregations checkAndCreate(SelectEntry selectEntry)
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
					aggExpression, GroovyExpressionCompiler.LANGUAGE);

			if (entryAggrgations==null)
			{
				entryAggrgations = new ArrayList<SelectEntryAggregation>(2);
			}
			entryAggrgations.add(new SelectEntryAggregation(func, expr, converter));

			m.appendReplacement(resExpression, "rm_agg_"+(++i));
		}

		if (entryAggrgations!=null)
		{
			//Создаем selectEntryAggregations
            m.appendTail(resExpression);
            Expression entryExpression = expressionCompiler.compile(
                    resExpression.toString(), GroovyExpressionCompiler.LANGUAGE);
            SelectEntryAggregation[] aggArr = new SelectEntryAggregation[entryAggrgations.size()];
            entryAggrgations.toArray(aggArr);
            
            return new SelectEntryAggregations(entryExpression, aggArr, selectEntry);
		}
        else
            return null;
	}

    public SelectEntryAggregations(
            Expression expression, SelectEntryAggregation[] aggregations, SelectEntry selectEntry)
    {
        this.expression = expression;
        this.aggregations = aggregations;
        this.selectEntry = selectEntry;
    }

    public SelectEntryAggregation[] getAggregations()
    {
        return aggregations;
    }

    public SelectEntry getSelectEntry()
    {
        return selectEntry;
    }

    public Object eval() throws ScriptException
    {
        SimpleBindings bindings = new SimpleBindings();
        for (int i=0; i<aggregations.length; ++i)
            bindings.put(AGGREGATION_PREFIX+(i+1), aggregations[i].getValue());

        return expression.eval(bindings);
    }

    public void aggregate(Bindings bindings) throws ScriptException
    {
        for (SelectEntryAggregation aggregation: aggregations)
            aggregation.aggregate(bindings);
    }

    public void reset()
    {
        for (SelectEntryAggregation aggregation: aggregations)
            aggregation.reset();
    }
}

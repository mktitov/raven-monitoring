/*
 *  Copyright 2008 Mikhail Titov.
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

import org.raven.statdb.AggregationFunction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jrobin.core.Util;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.statdb.Aggregation;
import org.raven.statdb.Rule;
import org.raven.statdb.RuleProcessingResult;
import org.raven.statdb.StatisticsDatabase;
import org.raven.statdb.StatisticsRecord;
import org.raven.tree.NodeError;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RulesNode.class)
public class AggregationRuleNode extends BaseNode implements Rule
{
	@Parameter(defaultValue="AVERAGE")
	@NotNull
	private AggregationFunction aggregationFunction;

	private Map<String, Aggregation> aggregations;

	private ReadWriteLock aggregationsLock;

	@Override
	protected void initFields()
	{
		super.initFields();

		aggregationsLock = new ReentrantReadWriteLock();
	}

	@Override
	protected void doStart() throws Exception
	{
		super.doStart();
		aggregations = new ConcurrentHashMap<String, Aggregation>(16, .75f, 3);
	}

	@Override
	public synchronized void stop() throws NodeError
	{
		try
		{
			aggregationsLock.writeLock().lock();
			aggregations = null;
			
			super.stop();
		}
		finally
		{
			aggregationsLock.writeLock().unlock();
		}
	}

	public void processRule(
			String key, String name, Double value, StatisticsRecord record
			, RuleProcessingResult result, StatisticsDatabase database)
		throws Exception
	{
		String aggregationKey = key+"#"+name;
        Aggregation aggregation = null;
		aggregationsLock.readLock().lock();
		try {
			if (!isStarted())
				return;
			aggregation = aggregations.get(aggregationKey);
		} finally {
			aggregationsLock.readLock().unlock();
		}
        long ntime = Util.normalize(record.getTime(), database.getStep());
        if (aggregation==null)
            aggregation = createAggregation(aggregationKey, ntime, value);
        else
        {
            synchronized(aggregation)
            {
                if (ntime>aggregation.getTime())
                {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug(String.format(
                                "Saving aggregated value (%s) for aggregation key (%s) " +
                                "to the statistics database. "
                                , aggregation.getValue(), aggregationKey));
                    database.saveStatisticsValue(
                            key, name, aggregation.getValue(), aggregation.getTime());
                    aggregation.reset(ntime, value);
                }
                else
                {
                    aggregation.aggregate(value);
                }
            }
        }
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format(
                    "Aggregating value (%s) for aggregation key (%s). " +
                    "New aggregated value is (%s)"
                    , value, aggregationKey, aggregation.getValue()));
	}

	public AggregationFunction getAggregationFunction()
	{
		return aggregationFunction;
	}

	public void setAggregationFunction(AggregationFunction aggregationFunction)
	{
		this.aggregationFunction = aggregationFunction;
	}

	private Aggregation createAggregation(String aggregationKey, long time, double value)
	{
        aggregationsLock.writeLock().lock();
        try
        {
            Aggregation aggregation = aggregations.get(aggregationKey);
            if (aggregation==null)
            {
                switch (aggregationFunction)
                {
                    case MAX: aggregation = new MaxAggregation(time, value); break;
                    case AVERAGE : aggregation = new AverageAggregation(time, value); break;
                    case LAST : aggregation = new LastAggregation(time, value); break;
                    case MIN : aggregation = new MinAggregation(time, value); break;
                    case SUM : aggregation = new SumAggregation(time, value); break;
                }

                aggregations.put(aggregationKey, aggregation);
            }
            return aggregation;
        }
        finally
        {
            aggregationsLock.writeLock().unlock();
        }
	}
}

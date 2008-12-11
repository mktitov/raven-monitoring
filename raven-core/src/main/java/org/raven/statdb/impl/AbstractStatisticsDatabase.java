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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.log.LogLevel;
import org.raven.statdb.ProcessingInstruction;
import org.raven.statdb.Rule;
import org.raven.statdb.RuleProcessingResult;
import org.raven.statdb.StatisticsDatabase;
import org.raven.statdb.StatisticsRecord;

/**
 *
 * @author Mikhail Titov 
 */
public abstract class AbstractStatisticsDatabase
	extends AbstractDataConsumer implements StatisticsDatabase 
{
	protected StatisticsDefinitionsNode statisticsDefinitions;
    protected RulesNode rulesNode;

    protected Map<String, Double> previousValues;

	@Override
	protected void initFields()
	{
		super.initFields();

		previousValues = new ConcurrentHashMap<String, Double>();
	}

	@Override
	protected void doInit() throws Exception
	{
		super.doInit();
		
		initConfigurationNodes();
	}

	@Override
	protected void doStart() throws Exception
	{
		super.doStart();

		initConfigurationNodes();
	}

	protected abstract void saveStatisticsValue(String[] key, String statisticName, Double value);

	private void initConfigurationNodes()
	{
		statisticsDefinitions =
			(StatisticsDefinitionsNode)getChildren(StatisticsDefinitionsNode.NAME);
		if (statisticsDefinitions==null)
		{
			statisticsDefinitions = new StatisticsDefinitionsNode();
			addChildren(statisticsDefinitions);
			statisticsDefinitions.save();
			statisticsDefinitions.init();
			statisticsDefinitions.start();
		}

        rulesNode = (RulesNode) getChildren(RulesNode.NAME);
        if (rulesNode==null)
        {
            rulesNode = new RulesNode();
            addChildren(rulesNode);
            rulesNode.save();
            rulesNode.init();
            rulesNode.start();
        }
	}

	@Override
	protected void doSetData(DataSource dataSource, Object data)
	{
		if (!(data instanceof StatisticsRecord))
		{
			logger.warn(String.format(
					"Invalid data type recieved from (%s). The data must have (%s) type, " +
					"but recieved (%s)"
					, dataSource.getPath(), StatisticsRecord.class.getName()
					, (data==null? "null" : data.getClass().getName())));
			return;
		}

		StatisticsRecord record = (StatisticsRecord) data;

		String[] key = record.getKey().split("/");
		if (key==null || key.length==0 || key[0].length()==0)
		{
			logger.error(String.format(
					"Invalid statistics record key (%s) recieved from (%s)"
					, record.getKey(), dataSource.getPath()));
			return;
		}

		if (record.getValues()==null || record.getValues().isEmpty())
		{
			if (isLogLevelEnabled(LogLevel.DEBUG))
				logger.debug(String.format(
						"Recieved empty statistic record for key (%s) from (%s)"
						, record.getKey(), dataSource.getPath()));
			return;
		}
        StringBuilder partialKeyBuf = new StringBuilder("/");
        for (String subKey: key)
        {
            String partialKey = partialKeyBuf.toString();
            Collection<Rule> rules = getRules(partialKey);
            for (Map.Entry<String, Double> value: record.getValues().entrySet())
            {
                try
                {
    //				processStatisticsValue(key, value.getKey(), value.getValue());
                }
                catch(Throwable e)
                {
                    logger.error(
                        String.format(
                            "Error processing statistics record value (%s) for statistics name (%s) " +
                            "and record key (%s). %s"
                            , value.getValue(), value.getKey(), record.getKey(), e.getMessage())
                        , e);
                }
            }
            partialKeyBuf.append(subKey+"/");
        }

	}

    private Collection<Rule> getRules(String partialKey)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private ProcessingInstruction processStatistics(
            String partialKey, String name, Double value, StatisticsRecord record, boolean transit)
    {
        Collection<Rule> rules = getRules(partialKey);
        if (rules!=null)
        {
            RuleProcessingResultImpl result =
                    new RuleProcessingResultImpl(ProcessingInstruction.CONTINUE_PROCESSING, value);
            for (Rule rule: rules)
            {
                rule.processRule(partialKey, value, record, result);
                if (result.getInstruction()!=ProcessingInstruction.CONTINUE_PROCESSING)
                    return result.getInstruction();
                result.setInstruction(ProcessingInstruction.CONTINUE_PROCESSING);
            }
        }

        return ProcessingInstruction.CONTINUE_PROCESSING;
    }

//	private void processStatisticsValue(
//			String[] key, String name, Double value, StatisticsRecord record)
//		throws Exception
//	{
//		StatisticsDefinitionNode statDef =
//				(StatisticsDefinitionNode) statisticsDefinitions.getChildren(name);
//		if (statDef==null)
//			throw new Exception("Unknown statistics name");
//
//		//process routes
//
//		boolean savePreviousValue = statDef.getSavePreviousValue();
//		String statisticsId = null;
//		Double previousValue = null;
//		if (savePreviousValue)
//			previousValue = previousValues.get(statisticsId);
//
//		Double newValue = statDef.calculateValue(value, previousValue, record);
//
//		if (savePreviousValue)
//			previousValues.put(statisticsId, value);
//
//
//
//		//process aggregations
//		//process routes
//
//		//process value
//	}

	protected static String getStatisticsNameId(String key, String name)
	{
		return key+"#"+name;
	}
}

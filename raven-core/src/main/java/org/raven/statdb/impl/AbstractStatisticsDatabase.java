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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.Bindings;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.log.LogLevel;
import org.raven.statdb.ProcessingInstruction;
import org.raven.statdb.Rule;
import org.raven.statdb.StatisticsDatabase;
import org.raven.statdb.StatisticsRecord;
import org.raven.tree.Node;
import org.raven.util.BindingSupport;

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
	protected BindingSupport bindingSupport;

	@Override
	protected void initFields()
	{
		super.initFields();

		previousValues = new ConcurrentHashMap<String, Double>();
		bindingSupport = new BindingSupport();
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

	public RulesNode getRulesNode()
	{
		return rulesNode;
	}

	protected abstract void saveStatisticsValue(String key, String statisticName, Double value);

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

		if (record.getKey()==null || !record.getKey().startsWith("/"))
		{
			error(String.format(
					"Invalid statistic record recieved from (%s). Key (%s) must not be null and " +
					"must start from (/)", dataSource.getPath(), record.getKey()));
			return;
		}
		
		String[] key = record.getKey().substring(1).split("/");
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
		Set<String> stopList = new HashSet<String>();
		int i=0;
        for (String subKey: key)
        {
			++i;
            String partialKey = partialKeyBuf.append(subKey+"/").toString();
            for (Map.Entry<String, Double> value: record.getValues().entrySet())
            {
                try
                {
					if (!stopList.contains(value.getKey()))
					{
						ProcessingInstruction ins =
								processStatistics(
									partialKey, value.getKey(), value.getValue(), record
									, key.length!=i);
						if (ins==ProcessingInstruction.STOP_PROCESSING_KEY)
							stopList.add(record.getKey());
					}
                }
                catch(Throwable e)
                {
                    logger.error(
                        String.format(
                            "Error processing statistics record value (%s) " +
							"for statistics name (%s) and record key (%s). %s"
                            , value.getValue(), value.getKey(), record.getKey(), e.getMessage())
                        , e);
                }
            }
        }

	}

    private ProcessingInstruction processStatistics(
            String partialKey, String name, Double value, StatisticsRecord record, boolean transit)
    {
		bindingSupport.put("key", partialKey);
		bindingSupport.put("name", name);
		bindingSupport.put("value", value);
		bindingSupport.put("record", record);
		Collection<Node> rules = rulesNode.getEffectiveChildrens();
		bindingSupport.reset();
		
        if (rules!=null)
        {
            RuleProcessingResultImpl result =
                    new RuleProcessingResultImpl(ProcessingInstruction.CONTINUE_PROCESSING, value);
            for (Node ruleNode: rules)
            {
				if (ruleNode instanceof Rule)
				{
					Rule rule = (Rule) ruleNode;
					rule.processRule(partialKey, name, value, record, result);
					if (result.getInstruction()!=ProcessingInstruction.CONTINUE_PROCESSING)
						return result.getInstruction();
					result.setInstruction(ProcessingInstruction.CONTINUE_PROCESSING);
				}
				else
					error(String.format(
							"Node (%s) is not a rule. Rule node must implement the (%s) interface"
							, ruleNode.getPath(), Rule.class.getName()));
            }
        }

		if (!transit)
			saveStatisticsValue(partialKey, name, value);

        return ProcessingInstruction.CONTINUE_PROCESSING;
    }

	@Override
	public void formExpressionBindings(Bindings bindings)
	{
		super.formExpressionBindings(bindings);

		bindingSupport.addTo(bindings);
	}

	protected static String getStatisticsNameId(String key, String name)
	{
		return key+"#"+name;
	}
}

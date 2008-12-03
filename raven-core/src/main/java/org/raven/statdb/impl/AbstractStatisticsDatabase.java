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

import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.statdb.StatisticsDatabase;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractStatisticsDatabase
	extends AbstractDataConsumer implements StatisticsDatabase 
{
	protected StatisticsDefinitionsNode statisticsDefinitions;
	protected AggregationsNode aggregations;
	protected RoutesNode routes;

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

		aggregations = (AggregationsNode) getChildren(AggregationsNode.NAME);
		if (aggregations==null)
		{
			aggregations = new AggregationsNode();
			addChildren(aggregations);
			aggregations.save();
			aggregations.init();
			aggregations.start();
		}

		routes = (RoutesNode) getChildren(RoutesNode.NAME);
		if (routes==null)
		{
			routes = new RoutesNode();
			addChildren(routes);
			routes.save();
			routes.init();
			routes.start();
		}
	}
}

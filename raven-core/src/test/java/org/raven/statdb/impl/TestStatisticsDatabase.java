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

/**
 *
 * @author Mikhail Titov
 */
public class TestStatisticsDatabase extends AbstractStatisticsDatabase
{
	private SaveStatisticsValue databaseMock;

	public SaveStatisticsValue getDatabaseMock()
	{
		return databaseMock;
	}

	public void setDatabaseMock(SaveStatisticsValue databaseMock)
	{
		this.databaseMock = databaseMock;
	}

	public void saveStatisticsValue(String key, String statisticName, double value, long time)
	{
		databaseMock.saveStatisticsValue(key, statisticName, value, time);
	}

	@Override
	protected boolean isStatisticsDefenitionValid(StatisticsDefinitionNode statDef)
	{
		return true;
	}

}

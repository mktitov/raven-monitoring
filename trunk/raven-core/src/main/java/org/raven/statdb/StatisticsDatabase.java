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

package org.raven.statdb;

import org.raven.ds.DataConsumer;
import org.raven.statdb.query.Query;
import org.raven.statdb.query.QueryExecutionException;
import org.raven.statdb.query.QueryResult;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface StatisticsDatabase extends Node, DataConsumer
{
	public final static String KEY_DELIMITER = "/";

	/**
	 * Returns the database step in seconds
	 */
	public Long getStep();

	/**
	 * Saves the value of the given statistics to the database
	 * @param key the statistics key
	 * @param statisticName the statistics name
	 * @param value the value of the statistics
	 */
	public void saveStatisticsValue(String key, String statisticName, double value, long time)
			throws Exception;
	
	public void processStatisticsRecord(Node source, StatisticsRecord record);

	/**
	 * Executes query on this database.
	 * @param query the query
	 * @return The result of the query execution
	 */
	public QueryResult executeQuery(Query query) throws QueryExecutionException;

}

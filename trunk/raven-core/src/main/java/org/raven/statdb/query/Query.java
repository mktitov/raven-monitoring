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

package org.raven.statdb.query;

import org.raven.statdb.StatisticsDatabase;

/**
 * The query interface for retriving data from statistics database.
 * @see StatisticsDatabase#executeQuery
 * @author Mikhail Titov
 */
public interface Query
{
	/**
	 * Returns the time step in seconds. If returns zero than 
	 * {@link StatisticsDatabase#getStep() database step} will be taken
	 * @see FromClause#getDatabase() 
	 */
	public Long getStep();
    /**
     * Returns the select mode of the query
     */
    public SelectMode getSelectMode();
    /**
     * Returns the the "at" style start time.
     * @see #getEndTime()
     */
    public String getStartTime();
    /**
     * Returns the the "at" style end time.
     * @see #getStartTime()
     */
    public String getEndTime();
	/**
	 * Returns the maximum keys count in the query result.
	 */
	public Integer getMaximumKeyCount();
	/**
	 * Returns the name of the statistics that will be take part in the query
	 */
	public QueryStatisticsName[] getStatisticsNames();
	/**
	 * Returns the query from clause
	 */
	public FromClause getFromClause();
	/**
	 * Returns the select clause of the query.
	 */
	public SelectClause getSelectClause();
    /**
     * Returns the order clause of the query.
     */
    public OrderClause getOrderClause();
}

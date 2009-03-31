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

import org.raven.statdb.query.Query;
import org.raven.statdb.query.QueryExecutionException;
import org.raven.statdb.query.QueryResult;

/**
 *
 * @author Mikhail Titov
 */
public class TestStatisticsDatabase2  extends AbstractStatisticsDatabase
{
    private QueryResult result;
    private Query query;

    public Query getQuery()
    {
        return query;
    }

    public void setResult(QueryResult result)
    {
        this.result = result;
    }


    @Override
    protected boolean isStatisticsDefenitionValid(StatisticsDefinitionNode statDef)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void saveStatisticsValue(String key, String statisticName, double value, long time)
            throws Exception
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public QueryResult executeQuery(Query query) throws QueryExecutionException
    {
        this.query = query;
        return result;
    }
}

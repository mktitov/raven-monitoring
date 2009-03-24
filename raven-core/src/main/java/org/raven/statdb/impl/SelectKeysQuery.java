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

import org.raven.statdb.StatisticsDatabase;
import org.raven.statdb.query.FromClause;
import org.raven.statdb.query.Query;
import org.raven.statdb.query.QueryStatisticsName;
import org.raven.statdb.query.SelectClause;
import org.raven.statdb.query.SelectMode;

/**
 * Query that allows to select keys from statistics database.
 * 
 * @author Mikhail Titov
 */
public class SelectKeysQuery implements Query
{
    private final FromClause fromClause;

    public SelectKeysQuery(String keyExpression, StatisticsDatabase statisticsDatabase)
    {
        this.fromClause = new FromClauseImpl(keyExpression, statisticsDatabase);
    }

    public Long getStep()
    {
        return 0l;
    }

    public String getStartTime()
    {
        return null;
    }

    public String getEndTime()
    {
        return null;
    }

    public Integer getMaximumKeyCount()
    {
        return Integer.MAX_VALUE;
    }

    public QueryStatisticsName[] getStatisticsNames()
    {
        return null;
    }

    public FromClause getFromClause()
    {
        return fromClause;
    }

    public SelectClause getSelectClause()
    {
        return null;
    }

    public SelectMode getSelectMode()
    {
        return SelectMode.SELECT_KEYS;
    }
}

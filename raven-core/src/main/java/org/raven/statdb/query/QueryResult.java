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

import java.util.Collection;
import org.raven.statdb.StatisticsDatabase;

/**
 * The result of the query execution.
 * @see Query
 * @see StatisticsDatabase#executeQuery(org.raven.statdb.query.Query) 
 * @author Mikhail Titov
 */
public interface QueryResult
{
    public String[] getStatisticNames();
	public long[] getTimestamps();
    public long getStep();
    public int getValuesCount();
    /**
     * Returns the collection of key values. Method can return an empty collection but never
     * returns <code>null</code>.
     */
    public Collection<KeyValues> getKeyValues();
}

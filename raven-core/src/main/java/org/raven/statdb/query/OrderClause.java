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

/**
 * The order clause of the query
 * @author Mikhail Titov
 */
public interface OrderClause
{
    /**
     * Returns the name of statistics which values will used to order the collection of the
     * {@link QueryResult#getKeyValues KeyValues}
     */
    public String getStatisticName();
    /**
     * If the method returns <b>true</b> then the collection of the
     * {@link QueryResult#getKeyValues KeyValues} will be sorted in reverse order.
     */
    public Boolean getReverseOrder();
}

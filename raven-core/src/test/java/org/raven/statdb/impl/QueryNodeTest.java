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

import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.statdb.AggregationFunction;
import org.raven.statdb.query.QueryStatisticsName;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class QueryNodeTest extends RavenCoreTestCase
{
    private QueryNode query;

    @Before
    public void prepareTest()
    {
        query = new QueryNode();
        query.setName("query");
        tree.getRootNode().addAndSaveChildren(query);
    }

    @Test
    public void statisticsNamesNode_test() throws Exception
    {
        Node statisticsNames = query.getChildren(StatisticsNamesNode.NAME);
        assertNotNull(statisticsNames);
        assertTrue(statisticsNames instanceof StatisticsNamesNode);
        assertNull(query.getStatisticsNames());

        StatisticsNameNode statNameNode = new StatisticsNameNode();
        statNameNode.setName("s1");
        statisticsNames.addAndSaveChildren(statNameNode);
        statNameNode.setAggregationFunction(AggregationFunction.LAST);

        assertArrayEquals(new QueryStatisticsName[]{statNameNode}, query.getStatisticsNames());
        
    }
}
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
import org.raven.test.RavenCoreTestCase;
import org.raven.statdb.AggregationFunction;
import org.raven.statdb.query.QueryStatisticsName;
import org.raven.statdb.query.SelectMode;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
/**
 *
 * @author Mikhail Titov
 */
public class SdbQueryNodeTest extends RavenCoreTestCase
{
    private SdbQueryNode query;

    @Before
    public void prepareTest()
    {
        query = new SdbQueryNode();
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
        assertNull(query.getStatisticsNames());

        statNameNode.start();
        assertEquals(Status.STARTED, statNameNode.getStatus());

        assertArrayEquals(new QueryStatisticsName[]{statNameNode}, query.getStatisticsNames());
    }

    @Test
    public void fromClause_test() throws Exception
    {
        TestStatisticsDatabase db = new TestStatisticsDatabase();
        db.setName("db");
        tree.getRootNode().addAndSaveChildren(db);

        Node fromNode = query.getChildren(FromClauseNode.NAME);
        assertNotNull(fromNode);
        assertTrue(fromNode instanceof FromClauseNode);
        FromClauseNode from = (FromClauseNode) fromNode;
        assertEquals(Status.INITIALIZED, from.getStatus());
        from.setKeyExpression("/.*/");
        from.start();

        assertEquals(Status.STARTED, from.getStatus());
    }

    @Test
    public void selectClause_test() throws Exception
    {
        SelectClauseNode select = (SelectClauseNode) query.getSelectClause();
        assertNotNull(select);
        assertEquals(Status.STARTED, select.getStatus());
        assertEquals(SelectMode.SELECT_KEYS_AND_DATA, query.getSelectMode());

        assertNull(select.getSelectEntries());

        SelectEntryNode entry = new SelectEntryNode();
        entry.setName("e1");
        select.addAndSaveChildren(entry);
        entry.setExpression("s1");

        assertNull(select.getSelectEntries());

        entry.start();
        assertEquals(Status.STARTED, entry.getStatus());

        assertNotNull(select.getSelectEntries());
        assertEquals(1, select.getSelectEntries().length);
        assertSame(entry, select.getSelectEntries()[0]);
    }

    @Test
    public void orderClause_test() throws Exception
    {
        assertNotNull(query.getOrderClause());
    }
}
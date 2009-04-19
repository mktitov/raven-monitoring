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

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.DataCollector;
import org.raven.PushDataSource;
import org.raven.RavenCoreTestCase;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.statdb.query.QueryResult;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class SdbQueryResultNodeTest extends RavenCoreTestCase
{
    private SdbQueryResultNode resultNode;
    private TestStatisticsDatabase2 db;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        PushDataSource ds = new PushDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        RecordSchemaNode schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        db = new TestStatisticsDatabase2();
        db.setName("db");
        tree.getRootNode().addAndSaveChildren(db);
        db.setStep(10);
        db.setDataSource(ds);
        db.setRecordSchema(schema);
        assertTrue(db.start());

        resultNode = new SdbQueryResultNode();
        resultNode.setName("queryResultNode");
        tree.getRootNode().addAndSaveChildren(resultNode);
        resultNode.setStatisticsDatabase(db);
        assertTrue(resultNode.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(resultNode);
        assertTrue(collector.start());
    }

    @Test
    public void queryNodeGeneration_test()
    {
        SdbQueryNode query = resultNode.getQueryNode();
        assertNotNull(query);
        assertInititalized(query);
    }

    @Test
    public void dataSource_test()
    {
        QueryResult queryResult = createMock(QueryResult.class);
        replay(queryResult);
        db.setResult(queryResult);

        collector.refereshData(null);
        List dataList = collector.getDataList();
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertSame(queryResult, dataList.get(0));

        verify(queryResult);
    }
}
/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.table;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenUtils;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.AbstractDataConsumer.ResetDataPolicy;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.log.LogLevel;
import org.raven.test.DataCollector;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class RecordsToTableNodeTest extends RavenCoreTestCase
{
    private RecordSchemaNode schema;
    private RecordsToTableNode conv;
    private PushOnDemandDataSource ds;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        RecordSchemaFieldNode field1 = new RecordSchemaFieldNode();
        field1.setName("field1");
        schema.addAndSaveChildren(field1);
        field1.setDisplayName("Field 1");
        field1.setFieldType(RecordSchemaFieldType.STRING);
        assertTrue(field1.start());

        field1 = new RecordSchemaFieldNode();
        field1.setName("field2");
        schema.addAndSaveChildren(field1);
        field1.setDisplayName("Field 2");
        field1.setFieldType(RecordSchemaFieldType.STRING);
        assertTrue(field1.start());

        ds = new PushOnDemandDataSource();
        ds.setName("data source");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.setLogLevel(LogLevel.TRACE);
        assertTrue(ds.start());

        conv = new RecordsToTableNode();
        conv.setName("records to table converter");
        tree.getRootNode().addAndSaveChildren(conv);
        conv.setRecordSchema(schema);
        conv.setLogLevel(LogLevel.TRACE);
        conv.setDataSource(ds);

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(conv);
        collector.setLogLevel(LogLevel.TRACE);
        collector.setResetDataPolicy(ResetDataPolicy.DONT_RESET_DATA);
        assertTrue(collector.start());
    }

    @Test
    public void startWithEmptyColumnsOrderTest()
    {
        assertTrue(conv.start());
    }

    @Test
    public void startWithValidColumnsOrderTest()
    {
        conv.setFieldsOrder("field2, field1");
        assertTrue(conv.start());
    }

    @Test
    public void startWithInvalidColumnsOrderTest()
    {
        conv.setFieldsOrder("field3");
        assertFalse(conv.start());
    }

    @Test
    public void sendTableWithEmptyColumnsOrderTest() throws Exception
    {
        assertTrue(conv.start());
        
        Record rec = schema.createRecord();
        rec.setValue("field1", "value1");
        rec.setValue("field2", "value2");

        ds.addDataPortion(rec);
        ds.addDataPortion(null);

        Object res = collector.refereshData(null);
        assertNotNull(res);
        assertTrue(res instanceof Table);
        Table table = (Table) res;
        assertArrayEquals(new String[]{"Field 1", "Field 2"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        assertArrayEquals(new Object[]{"value1", "value2"}, rows.get(0));
    }

    @Test
    public void sendTableWithColumnsOrderTest() throws Exception
    {
        conv.setFieldsOrder("field2, field1");
        assertTrue(conv.start());

        Record rec = schema.createRecord();
        rec.setValue("field1", "value1");
        rec.setValue("field2", "value2");

        ds.addDataPortion(rec);
        ds.addDataPortion(null);

        Object res = collector.refereshData(null);
        assertNotNull(res);
        assertTrue(res instanceof Table);
        Table table = (Table) res;
        assertArrayEquals(new String[]{"Field 2", "Field 1"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        assertArrayEquals(new Object[]{"value2", "value1"}, rows.get(0));
    }
}
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

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.raven.test.DataCollector;
import org.raven.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.log.LogLevel;
import org.raven.statdb.StatisticsRecord;
import org.raven.statdb.query.KeyValues;

/**
 *
 * @author Mikhail Titov
 */
public class SdbQueryResultToRecordsNodeTest extends RavenCoreTestCase
{
    private RecordSchemaNode schema;

    @Test
    public void test() throws Exception
    {
        PushDataSource ds = new PushDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        RecordSchemaFieldNode timeField = new RecordSchemaFieldNode();
        timeField.setName(StatisticsRecord.TIME_FIELD_NAME);
        schema.addAndSaveChildren(timeField);
        timeField.setFieldType(RecordSchemaFieldType.LONG);
        assertTrue(timeField.start());

        RecordSchemaFieldNode keyField = new RecordSchemaFieldNode();
        keyField.setName(StatisticsRecord.KEY_FIELD_NAME);
        schema.addAndSaveChildren(keyField);
        keyField.setFieldType(RecordSchemaFieldType.STRING);
        assertTrue(keyField.start());

        RecordSchemaFieldNode s1Field = new RecordSchemaFieldNode();
        s1Field.setName("s1");
        schema.addAndSaveChildren(s1Field);
        s1Field.setFieldType(RecordSchemaFieldType.DOUBLE);
        assertTrue(s1Field.start());

        KeyValuesImpl keyValues1 = new KeyValuesImpl("/test/");
        KeyValuesImpl keyValues2 = new KeyValuesImpl("/test1/");
        keyValues2.addStatisticsValues(new StatisticsValuesImpl("s1", new double[]{10, 20}));
        keyValues2.addStatisticsValues(new StatisticsValuesImpl("s2", new double[]{100, 200}));
        QueryResultImpl res = new QueryResultImpl(
                Arrays.asList((KeyValues)keyValues1, (KeyValues)keyValues2));
        res.setTimestamps(new long[]{0, 100});

        SdbQueryResultToRecordsNode converter = new SdbQueryResultToRecordsNode();
        converter.setName("converter");
        tree.getRootNode().addAndSaveChildren(converter);
        converter.setDataSource(ds);
        converter.setRecordSchema(schema);
        converter.setLogLevel(LogLevel.DEBUG);
        assertTrue(converter.start());

        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(converter);
        assertTrue(collector.start());

        ds.pushData(res);
        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(4, data.size());

        Record rec;
        rec = getRecord(data, 0);
        testRecord(getRecord(data, 0), "/test/", null, null);
        testRecord(getRecord(data, 1), "/test1/", 0l, 10.);
        testRecord(getRecord(data, 2), "/test1/", 100l, 20.);
        assertNull(data.get(3));
    }

    private Record getRecord(List data, int ind)
    {
        Object obj = data.get(ind);
        assertTrue(obj instanceof Record);
        return (Record) obj;
    }

    private void testRecord(Record rec, String key, Long time, Double value) throws RecordException
    {
        assertEquals(key, rec.getValue(StatisticsRecord.KEY_FIELD_NAME));
        assertEquals(time, rec.getValue(StatisticsRecord.TIME_FIELD_NAME));
        assertEquals(value, rec.getValue("s1"));
    }
}
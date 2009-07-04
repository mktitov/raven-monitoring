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

package org.raven.ds.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import org.raven.DataCollector;
import org.raven.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
public class CsvRecordWriterNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws ParseException, RecordException
    {
        RecordSchemaNode schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
        field.setName("f1");
        schema.addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.STRING);
        assertTrue(field.start());

        field = new RecordSchemaFieldNode();
        field.setName("f2");
        schema.addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.TIMESTAMP);
        field.setPattern("dd.MM.yyyy");
        assertTrue(field.start());

        PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        CsvRecordWriterNode writer = new CsvRecordWriterNode();
        writer.setName("csvWriter");
        tree.getRootNode().addAndSaveChildren(writer);
        writer.setRecordSchema(schema);
        writer.setDataSource(ds);
        writer.setLogLevel(LogLevel.DEBUG);
        assertTrue(writer.start());

        DataCollector consumer = new DataCollector();
        consumer.setName("consumer");
        tree.getRootNode().addAndSaveChildren(consumer);
        consumer.setDataSource(writer);
        assertTrue(consumer.start());

        Date ts = new SimpleDateFormat("dd.MM.yyyy").parse("05.05.2009");
        Record rec = schema.createRecord();
        rec.setValue("f1", "row1");
        rec.setValue("f2", ts);
        ds.pushData(rec);

        rec = schema.createRecord();
        rec.setValue("f1", "row2");
        rec.setValue("f2", ts);
        ds.pushData(rec);
        ds.pushData(null);

        List dataList = consumer.getDataList();
        assertEquals(1, dataList.size());
        assertNotNull(dataList.get(0));
        assertTrue(dataList.get(0) instanceof String);
        assertEquals("row1,05.05.2009\nrow2,05.05.2009", dataList.get(0));

        writer.setFieldSeparator(" ");

        consumer.getDataList().clear();
        rec = schema.createRecord();
        rec.setValue("f1", "row1");
        rec.setValue("f2", ts);
        ds.pushData(rec);
        ds.pushData(null);
        dataList = consumer.getDataList();
        assertEquals(1, dataList.size());
        assertNotNull(dataList.get(0));
        assertTrue(dataList.get(0) instanceof String);
        assertEquals("row1 05.05.2009", dataList.get(0));

        writer.setFieldsOrder("f1");
        consumer.getDataList().clear();
        rec = schema.createRecord();
        rec.setValue("f1", "row1");
        rec.setValue("f2", ts);
        ds.pushData(rec);
        ds.pushData(null);
        dataList = consumer.getDataList();
        assertEquals(1, dataList.size());
        assertNotNull(dataList.get(0));
        assertTrue(dataList.get(0) instanceof String);
        assertEquals("row1", dataList.get(0));

        writer.setFieldsOrder("f2, f1");
        consumer.getDataList().clear();
        rec = schema.createRecord();
        rec.setValue("f1", "row1");
        rec.setValue("f2", ts);
        ds.pushData(rec);
        ds.pushData(null);
        dataList = consumer.getDataList();
        assertEquals(1, dataList.size());
        assertNotNull(dataList.get(0));
        assertTrue(dataList.get(0) instanceof String);
        assertEquals("05.05.2009 row1", dataList.get(0));
    }
}
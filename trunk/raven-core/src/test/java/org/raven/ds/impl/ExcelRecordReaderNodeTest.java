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

package org.raven.ds.impl;

import java.io.FileInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import static org.junit.Assert.*;
import org.raven.ds.RecordException;
import org.raven.log.LogLevel;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node.Status;

/**
 *
 * @author Mikhail Titov
 */
public class ExcelRecordReaderNodeTest extends RavenCoreTestCase
{
    private RecordSchemaNode schema;
    private PushDataSource ds;
    private ExcelRecordReaderNode reader;
    private DataCollector consumer;

    @Before
    public void prepare()
    {
        schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        schema.start();
        assertEquals(Status.STARTED, schema.getStatus());

        RecordSchemaFieldNode field1 = new RecordSchemaFieldNode();
        field1.setName("field1");
        schema.addAndSaveChildren(field1);
        field1.setFieldType(RecordSchemaFieldType.DOUBLE);
        field1.start();
        assertEquals(Status.STARTED, field1.getStatus());

        CsvRecordFieldExtension csvExtension = new CsvRecordFieldExtension();
        csvExtension.setName("csv");
        field1.addAndSaveChildren(csvExtension);
        csvExtension.setColumnNumber(1);
        csvExtension.start();
        assertEquals(Status.STARTED, csvExtension.getStatus());

        field1 = new RecordSchemaFieldNode();
        field1.setName("field2");
        schema.addAndSaveChildren(field1);
        field1.setFieldType(RecordSchemaFieldType.DATE);
        field1.start();
        assertEquals(Status.STARTED, field1.getStatus());

        csvExtension = new CsvRecordFieldExtension();
        csvExtension.setName("csv");
        field1.addAndSaveChildren(csvExtension);
        csvExtension.setColumnNumber(2);
        csvExtension.start();
        assertEquals(Status.STARTED, csvExtension.getStatus());

        field1 = new RecordSchemaFieldNode();
        field1.setName("field3");
        schema.addAndSaveChildren(field1);
        field1.setFieldType(RecordSchemaFieldType.STRING);
        field1.start();
        assertEquals(Status.STARTED, field1.getStatus());

        csvExtension = new CsvRecordFieldExtension();
        csvExtension.setName("csv");
        field1.addAndSaveChildren(csvExtension);
        csvExtension.setColumnNumber(3);
        csvExtension.start();
        assertEquals(Status.STARTED, csvExtension.getStatus());

        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.start();
        assertEquals(Status.STARTED, ds.getStatus());

        reader = new ExcelRecordReaderNode();
        reader.setName("excel record reader");
        tree.getRootNode().addAndSaveChildren(reader);
        reader.setDataSource(ds);
        reader.setRecordSchema(schema);
        reader.setStartFromRow(2);
        reader.start();
        reader.setLogLevel(LogLevel.TRACE);
        assertEquals(Status.STARTED, reader.getStatus());

        consumer = new DataCollector();
        consumer.setName("consumer");
        tree.getRootNode().addAndSaveChildren(consumer);
        consumer.setDataSource(reader);
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());
    }

    @Test
    public void test() throws Exception
    {
        FileInputStream st = new FileInputStream("src/test/conf/test.xls");
        try{
            ds.pushData(st);
            assertEquals(2, consumer.getDataListSize());
            List data = consumer.getDataList();
            assertNull(data.get(1));
            assertTrue(data.get(0) instanceof Record);
            checkRecord((Record)data.get(0));
        }finally{
            st.close();
        }
    }

    @Test
    public void testIgnoreEmptyRows() throws Exception
    {
        FileInputStream st = new FileInputStream("src/test/conf/test1.xls");
        try{
            ds.pushData(st);
            assertEquals(8, consumer.getDataListSize());
        }finally{
            st.close();
        }
        
        consumer.getDataList().clear();
        reader.setIgnoreEmptyRows(Boolean.TRUE);
        st = new FileInputStream("src/test/conf/test1.xls");
        try{
            ds.pushData(st);
            assertEquals(2, consumer.getDataListSize());            
            List data = consumer.getDataList();
            assertNull(data.get(1));
            assertTrue(data.get(0) instanceof Record);
            checkRecord((Record)data.get(0));
        }finally{
            st.close();
        }
    }

    @Test
    public void testMaxEmptyRowsCount() throws Exception {
        reader.setMaxEmptyRowsCount(1);
        FileInputStream st = new FileInputStream("src/test/conf/test1.xls");
        try{
            ds.pushData(st);
            assertEquals(3, consumer.getDataListSize());
            List data = consumer.getDataList();
            assertNull(data.get(2));
            assertTrue(data.get(1) instanceof Record);
            assertTrue(data.get(0) instanceof Record);
            checkRecord((Record)data.get(0));
            checkEmptyRecord((Record)data.get(1));
        }finally{
            st.close();
        }
    }
    
    private void checkRecord(Record rec) throws ParseException, RecordException {
        assertEquals(123456789012l, ((Number)rec.getValue("field1")).longValue());
        assertEquals(new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2010"), rec.getValue("field2"));
        assertEquals("Строка", rec.getValue("field3"));
    }

    private void checkEmptyRecord(Record rec) throws Exception {
        assertNull(rec.getValue("field1"));
        assertNull(rec.getValue("field2"));
        assertNull(rec.getValue("field3"));
    }
}
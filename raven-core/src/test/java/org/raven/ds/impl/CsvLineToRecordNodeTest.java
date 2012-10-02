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

import org.junit.Before;
import org.junit.Test;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node.Status;

/**
 *
 * @author Mikhail Titov
 */
public class CsvLineToRecordNodeTest extends RavenCoreTestCase
{
    private RecordSchemaNode schema;
    private PushDataSource ds;
    private CsvLineToRecordNode converter;
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
        field1.setFieldType(RecordSchemaFieldType.INTEGER);
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

        converter = new CsvLineToRecordNode();
        converter.setName("converter");
        tree.getRootNode().addAndSaveChildren(converter);
        converter.setDataSource(ds);
        converter.setRecordSchema(schema);
        converter.start();
        assertEquals(Status.STARTED, converter.getStatus());

        consumer = new DataCollector();
        consumer.setName("consumer");
        tree.getRootNode().addAndSaveChildren(consumer);
        consumer.setDataSource(converter);
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());
    }

    @Test
    public void test() throws RecordException {
        ds.pushData("10,something,hello");

        assertEquals(1, consumer.getDataListSize());
        assertTrue(consumer.getDataList().get(0) instanceof Record);
        Record rec = (Record) consumer.getDataList().get(0);
        assertEquals(10, rec.getValue("field1"));
        assertEquals("hello", rec.getValue("field2"));
    }
    
    @Test
    public void nullDataTest() throws Exception {
        ds.pushData(null);
        assertEquals(1, consumer.getDataListSize());
        assertNull(consumer.getDataList().get(0));
    }
}
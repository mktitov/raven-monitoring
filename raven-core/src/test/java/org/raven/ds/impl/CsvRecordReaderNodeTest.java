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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.DataCollector;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.test.PushDataSource;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class CsvRecordReaderNodeTest extends RavenCoreTestCase
{
    private RecordSchemaNode schema;
    private PushDataSource ds;
    private CsvRecordReaderNode converter;
    private DataCollector consumer;
    private CsvRecordFieldExtension field1CsvExtension;
    
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

        field1CsvExtension = new CsvRecordFieldExtension();
        field1CsvExtension.setName("csv");
        field1.addAndSaveChildren(field1CsvExtension);
        field1CsvExtension.setColumnNumber(1);
        field1CsvExtension.start();
        assertEquals(Status.STARTED, field1CsvExtension.getStatus());

        field1 = new RecordSchemaFieldNode();
        field1.setName("field2");
        schema.addAndSaveChildren(field1);
        field1.setFieldType(RecordSchemaFieldType.STRING);
        field1.start();
        assertEquals(Status.STARTED, field1.getStatus());

        CsvRecordFieldExtension csvExtension = new CsvRecordFieldExtension();
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

        converter = new CsvRecordReaderNode();
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
    public void defaultConfigurationTest() throws Exception
    {
        ByteArrayInputStream stream = new ByteArrayInputStream(
                "1,2,\"3\"\n11,,".getBytes());
        ds.pushData(stream);

        List dataList = consumer.getDataList();
        assertEquals(3, dataList.size());

        Record record = (Record) dataList.get(0);
        assertEquals(1, record.getValue("field1"));
        assertEquals("3", record.getValue("field2"));
        record = (Record) dataList.get(1);
        assertEquals(11, record.getValue("field1"));
        assertNull(record.getValue("field2"));
    }

    @Test
    public void lineFilterTest() throws Exception
    {
        NodeAttribute filterAttr =
                converter.getNodeAttribute(CsvRecordReaderNode.LINEFILTER_ATTRIBUTE);
        assertNotNull(filterAttr);
        filterAttr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        filterAttr.setValue("!line.startsWith('#') && linenumber!=2");
        ByteArrayInputStream stream = new ByteArrayInputStream(
                "#comment\na,b,c,d\n1,2,3,4".getBytes());
        ds.pushData(stream);
        
        List dataList = consumer.getDataList();
        assertEquals(2, dataList.size());

        Record record = (Record) dataList.get(0);
        assertEquals(1, record.getValue("field1"));
        assertEquals("3", record.getValue("field2"));
    }

    @Test
    public void dataEncodingTest() throws Exception
    {
        Charset utfCharset = Charset.forName("UTF-8");
        converter.setDataEncoding(utfCharset);
        ByteArrayInputStream stream = new ByteArrayInputStream(
                "1,2,\"test,тест\",3".getBytes(utfCharset));
        ds.pushData(stream);
        
        List dataList = consumer.getDataList();
        assertEquals(2, dataList.size());

        Record record = (Record) dataList.get(0);
        assertEquals(1, record.getValue("field1"));
        assertEquals("test,тест", record.getValue("field2"));
    }
    
    @Test
    public void bindingTestForGetColumnNumber() throws Exception {
        NodeAttribute attr = new NodeAttributeImpl("colNum", Integer.class, 2, null);
        attr.setOwner(schema);
        attr.init();
        schema.addAttr(attr);
        
        attr = field1CsvExtension.getAttr("columnNumber");
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("recordSchema.$colNum");
        
        ByteArrayInputStream stream = new ByteArrayInputStream("1,2,\"3\"".getBytes());
        ds.pushData(stream);

        List dataList = consumer.getDataList();
        assertEquals(2, dataList.size());

        Record record = (Record) dataList.get(0);
        assertEquals(2, record.getValue("field1"));       
    }
}
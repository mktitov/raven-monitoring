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
import org.raven.RavenCoreTestCase;
import org.raven.RavenUtils;
import org.raven.ds.impl.objects.TestDataConsumer;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.rrd.objects.PushDataSource;
import org.raven.table.Table;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class CsvDataConverterNodeTest extends RavenCoreTestCase
{
    private PushDataSource ds;
    private CsvDataConverterNode converter;
    private TestDataConsumer consumer;
    
    @Before
    public void prepare()
    {
        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.start();
        assertEquals(Status.STARTED, ds.getStatus());

        converter = new CsvDataConverterNode();
        converter.setName("converter");
        tree.getRootNode().addAndSaveChildren(converter);
        converter.setDataSource(ds);
        converter.start();
        assertEquals(Status.STARTED, converter.getStatus());

        consumer = new TestDataConsumer();
        consumer.setName("consumer");
        tree.getRootNode().addAndSaveChildren(consumer);
        consumer.setDataSource(converter);
        consumer.setResetDataPolicy(AbstractDataConsumer.ResetDataPolicy.RESET_PREVIOUS_DATA);
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());
    }
        

    @Test
    public void defaultConfigurationTest() throws Exception
    {
        ByteArrayInputStream stream = new ByteArrayInputStream(
                "1,2,\"test,test\",3\n11,,,".getBytes());
        ds.pushData(stream);
        Object data = consumer.getLastData();
        assertNotNull(data);
        assertTrue(data instanceof Table);

        Table table = (Table) data;
        assertArrayEquals(new String[]{"1", "2", "3", "4"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertNotNull(rows);
        assertEquals(2, rows.size());
        assertArrayEquals(new String[]{"1", "2", "test,test", "3"}, rows.get(0));
        assertArrayEquals(new String[]{"11", null, null, null}, rows.get(1));
    }

    @Test
    public void columnHeadersTest() throws Exception
    {
        converter.setColumnNamesLineNumber(1);
        ByteArrayInputStream stream = new ByteArrayInputStream(
                "col1,col2,col3,col4\n1,2,3,4".getBytes());
        ds.pushData(stream);
        Object data = consumer.getLastData();
        assertNotNull(data);
        assertTrue(data instanceof Table);

        Table table = (Table) data;
        assertArrayEquals(new String[]{"col1", "col2", "col3", "col4"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        assertArrayEquals(new String[]{"1", "2", "3", "4"}, rows.get(0));
    }

    @Test
    public void lineFilterTest() throws Exception
    {
        NodeAttribute filterAttr =
                converter.getNodeAttribute(CsvDataConverterNode.LINEFILTER_ATTRIBUTE);
        assertNotNull(filterAttr);
        filterAttr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        filterAttr.setValue("!line.startsWith('#') && linenumber!=2");
        ByteArrayInputStream stream = new ByteArrayInputStream(
                "#comment\na,b,c,d\n1,2,3,4".getBytes());
        ds.pushData(stream);
        Object data = consumer.getLastData();
        assertNotNull(data);
        assertTrue(data instanceof Table);

        Table table = (Table) data;
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        assertArrayEquals(new String[]{"1", "2", "3", "4"}, rows.get(0));
    }

    @Test
    public void dataEncodingTest() throws Exception
    {
        Charset utfCharset = Charset.forName("UTF-8");
        converter.setDataEncoding(utfCharset);
        ByteArrayInputStream stream = new ByteArrayInputStream(
                "1,2,\"test,тест\",3".getBytes(utfCharset));
        ds.pushData(stream);
        Object data = consumer.getLastData();
        assertNotNull(data);
        assertTrue(data instanceof Table);

        Table table = (Table) data;
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        assertArrayEquals(new String[]{"1", "2", "test,тест", "3"}, rows.get(0));
    }
}
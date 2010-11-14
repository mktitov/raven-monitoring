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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.ds.DataContext;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.test.DataCollector;
import org.raven.test.DataHandler;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class TextToLinesNodeTest extends RavenCoreTestCase
{
    private PushDataSource ds;
    private TextToLinesNode splitter;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        splitter = new TextToLinesNode();
        splitter.setName("splitter");
        tree.getRootNode().addAndSaveChildren(splitter);
        splitter.setDataSource(ds);
        splitter.setEncoding(Charset.forName("utf-8"));
        assertTrue(splitter.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(splitter);
        assertTrue(collector.start());
    }

    @Test
    public void stringSourceAndLineNumbersTest()
    {
        final List<Object> dataList = new ArrayList<Object>();
        final List<Object> lineNumbers = new ArrayList<Object>();

        collector.setDataHandler(new DataHandler() {
            public void handleData(Object data, DataContext context) {
                dataList.add(data);
                lineNumbers.add(context.getAt(TextToLinesNode.LINE_NUMBER_PARAM));
            }
        });

        ds.pushData("Строка1\nline2");

        assertEquals(2, dataList.size());
        assertEquals("Строка1", dataList.get(0));
        assertEquals("line2", dataList.get(1));

        assertArrayEquals(new Object[]{1,2}, lineNumbers.toArray());
    }

    @Test
    public void lineFilterTest() throws Exception
    {
        final List<Object> dataList = new ArrayList<Object>();
        final List<Object> lineNumbers = new ArrayList<Object>();

        collector.setDataHandler(new DataHandler() {
            public void handleData(Object data, DataContext context) {
                dataList.add(data);
                lineNumbers.add(context.getAt(TextToLinesNode.LINE_NUMBER_PARAM));
            }
        });

        NodeAttribute attr = splitter.getNodeAttribute(TextToLinesNode.LINE_FILTER_ATTR);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("line!='Строка1'");

        ds.pushData("Строка1\nline2");

        assertEquals(1, dataList.size());
        assertEquals("line2", dataList.get(0));

        assertArrayEquals(new Object[]{2}, lineNumbers.toArray());
    }

    @Test
    public void inputStreamSourceTest() throws UnsupportedEncodingException
    {
        ByteArrayInputStream is = new ByteArrayInputStream("Строка1\nline2".getBytes("utf-8"));
        ds.pushData(is);

        assertEquals(2, collector.getDataListSize());
        assertEquals("Строка1", collector.getDataList().get(0));
        assertEquals("line2", collector.getDataList().get(1));
    }
}
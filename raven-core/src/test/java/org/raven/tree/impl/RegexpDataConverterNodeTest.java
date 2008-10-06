/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven.tree.impl;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.RavenUtils;
import org.raven.ds.impl.AbstractDataConsumer.ResetDataPolicy;
import org.raven.table.Table;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.objects.RegexpDataConsumer;
import org.raven.tree.impl.objects.RegexpDataSource;

/**
 *
 * @author Mikhail Titov
 */
public class RegexpDataConverterNodeTest extends RavenCoreTestCase
{
    private RegexpDataConverterNode regexpNode;
    private RegexpDataSource ds;
    private RegexpDataConsumer consumer;

    @Before
    public void prepareTest()
    {
        ds = new RegexpDataSource();
        ds.setName("ds");
        tree.getRootNode().addChildren(ds);
        ds.save();
        ds.init();
        ds.start();
        assertEquals(Status.STARTED, ds.getStatus());

        regexpNode = new RegexpDataConverterNode();
        regexpNode.setName("regexpNode");
        tree.getRootNode().addChildren(regexpNode);
        regexpNode.save();
        regexpNode.init();
        regexpNode.setDataSource(ds);

        consumer = new RegexpDataConsumer();
        consumer.setName("consumer");
        tree.getRootNode().addChildren(consumer);
        consumer.save();
        consumer.init();
        consumer.setDataSource(regexpNode);
        consumer.setResetDataPolicy(ResetDataPolicy.DONT_RESET_DATA);
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());
    }

    @Test
    public void defaultValuesTest() throws Exception
    {
        ds.setStringData("col1-1 col1-2\ncol2-1 col2-2");
        regexpNode.start();
        assertEquals(Status.STARTED, regexpNode.getStatus());

        Object data = consumer.refereshData(null);
        assertNotNull(data);
        assertTrue(data instanceof Table);

        List<Object[]> rows = RavenUtils.tableAsList((Table)data);
        assertEquals(2, rows.size());
        assertEquals("col1-1", rows.get(0)[0]);
        assertEquals("col1-2", rows.get(0)[1]);
        assertEquals("col2-1", rows.get(1)[0]);
        assertEquals("col2-2", rows.get(1)[1]);
    }

    @Test
    public void readAsOneRow() throws Exception
    {
        ds.setStringData("col1-1 col1-2\ncol2-1 col2-2");
        regexpNode.setRowDelimiter(null);
        regexpNode.start();
        assertEquals(Status.STARTED, regexpNode.getStatus());

        Object data = consumer.refereshData(null);
        assertNotNull(data);
        assertTrue(data instanceof Table);

        List<Object[]> rows = RavenUtils.tableAsList((Table)data);
        assertEquals(1, rows.size());
        assertEquals("col1-1", rows.get(0)[0]);
        assertEquals("col1-2", rows.get(0)[1]);
        assertEquals("col2-1", rows.get(0)[2]);
        assertEquals("col2-2", rows.get(0)[3]);
    }

    @Test
    public void readAsOneRowAndOneCol() throws Exception
    {
        ds.setStringData("col1-1 col1-2\ncol2-1 col2-2");
        regexpNode.setRowDelimiter(null);
        regexpNode.setColumnDelimiter(null);
        regexpNode.start();
        assertEquals(Status.STARTED, regexpNode.getStatus());

        Object data = consumer.refereshData(null);
        assertNotNull(data);
        assertTrue(data instanceof Table);

        List<Object[]> rows = RavenUtils.tableAsList((Table)data);
        assertEquals(1, rows.size());
        assertEquals("col1-1 col1-2\ncol2-1 col2-2", rows.get(0)[0]);
    }

//    @Test
//    public void regexpTest() throws Exception
//    {
//        Pattern p = Pattern.compile("(col[0-9])");
//        Matcher m = p.matcher("col1     ds \n asdas col2");
//        while (m.find())
//        {
//            assertTrue(m.groupCount()>0);
//        }
//    }

    @Test
    public void useRegexpGroupsInRowsDelimiterTest() throws Exception
    {
        ds.setStringData("col1 tr\ncol2 tr col3");
        regexpNode.setRowDelimiter("(col[0-9])");
        regexpNode.setUseRegexpGroupsInRowsDelimiter(true);
        regexpNode.start();
        assertEquals(Status.STARTED, regexpNode.getStatus());

        Object data = consumer.refereshData(null);
        assertNotNull(data);
        assertTrue(data instanceof Table);

        List<Object[]> rows = RavenUtils.tableAsList((Table)data);
        assertEquals(3, rows.size());
        assertEquals("col1", rows.get(0)[0]);
        assertEquals("col2", rows.get(1)[0]);
        assertEquals("col3", rows.get(2)[0]);
    }

    @Test
    public void useRegexpGroupsInColumnDelimiterTest() throws Exception
    {
        ds.setStringData("col1-1 trds col1-2 \ncol2-1 tr col2-2");
        regexpNode.setColumnDelimiter("(col.-[0-9])");
        regexpNode.setUseRegexpGroupsInColumnDelimiter(true);
        regexpNode.start();
        assertEquals(Status.STARTED, regexpNode.getStatus());

        Object data = consumer.refereshData(null);
        assertNotNull(data);
        assertTrue(data instanceof Table);

        List<Object[]> rows = RavenUtils.tableAsList((Table)data);
        assertEquals(2, rows.size());
        assertEquals("col1-1", rows.get(0)[0]);
        assertEquals("col1-2", rows.get(0)[1]);
        assertEquals("col2-1", rows.get(1)[0]);
        assertEquals("col2-2", rows.get(1)[1]);
    }
}
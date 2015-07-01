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

import java.io.FileInputStream;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenUtils;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class ExcelTableReaderNodeTest extends RavenCoreTestCase
{
    private PushDataSource ds;
    private ExcelTableReaderNode reader;
    private DataCollector consumer;
    
    @Before
    public void prepare()
    {
        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        reader = new ExcelTableReaderNode();
        reader.setName("excel record reader");
        tree.getRootNode().addAndSaveChildren(reader);
        reader.setDataSource(ds);
        reader.setStartFromRow(1);
        reader.start();
        assertTrue(reader.start());

        consumer = new DataCollector();
        consumer.setName("consumer");
        tree.getRootNode().addAndSaveChildren(consumer);
        consumer.setDataSource(reader);
        assertTrue(consumer.start());
    }

    @Test
    public void test() throws Exception
    {
        FileInputStream st = new FileInputStream("src/test/conf/test2.xls");
        try{
            ds.pushData(st);
            assertEquals(1, consumer.getDataListSize());
            List data = consumer.getDataList();
            assertNotNull(data.get(0));
            assertTrue(data.get(0) instanceof Table);
            Table table = (Table) data.get(0);
            assertArrayEquals(new String[]{"1", "2", "3", "4"}, table.getColumnNames());
            List<Object[]> rows = RavenUtils.tableAsList(table);
            assertEquals(4, rows.size());
            assertArrayEquals(new Object[]{null, 1.0, null, null}, rows.get(0));
            assertArrayEquals(new Object[]{"two", null, null, 3.0}, rows.get(1));
            assertArrayEquals(new Object[]{null, null, null, null}, rows.get(2));
            assertArrayEquals(new Object[]{4., null, null, null}, rows.get(3));
        }finally{
            st.close();
        }

    }
}
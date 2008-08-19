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

package org.raven.table;

import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.table.objects.TestDataConsumer;
import org.raven.table.objects.TestTableDataConsumer;
import org.raven.table.objects.TestTableDataSource;
import org.raven.tree.Node.Status;

/**
 *
 * @author Mikhail Titov
 */
public class TableMultiplexerNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        TableMultiplexerNode mux = new TableMultiplexerNode();
        mux.setName("mux");
        tree.getRootNode().addChildren(mux);
        mux.save();
        mux.init();
        
        TestTableDataSource table1 = new TestTableDataSource();
        table1.setName("primary table");
        mux.addChildren(table1);
        table1.save();
        table1.init();
        table1.start();
        assertEquals(Status.STARTED, table1.getStatus());
        
        mux.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE).setValue(table1.getPath());
        mux.start();
        assertEquals(Status.STARTED, mux.getStatus());
        
        TestTableDataConsumer table2 = new TestTableDataConsumer();
        table2.setName("secondary table");
        mux.addChildren(table2);
        table2.save();
        table2.init();
        table2.start();
        assertEquals(Status.STARTED, table2.getStatus());
        
        TestDataConsumer tableConsumer = new TestDataConsumer();
        tableConsumer.setName("table consumer");
        tree.getRootNode().addChildren(tableConsumer);
        tableConsumer.save();
        tableConsumer.init();
        tableConsumer.getNodeAttribute(
                AbstractDataConsumer.DATASOURCE_ATTRIBUTE).setValue(mux.getPath());
        tableConsumer.start();
        assertEquals(Status.STARTED, tableConsumer.getStatus());
        
        Object tabObj = tableConsumer.refereshData();
        assertNotNull(tabObj);
        assertTrue(tabObj instanceof Table);
        Table table = (Table) tabObj;
        assertEquals(2, table.getRowCount());
        assertEquals(5, table.getColumnNames().size());
        for (int row=0; row<2; ++row)
        {
            int col=1;
            for (String colName: new String[]{"col1", "col2", "col3", "col4"})
            {
                String val = "val_"+col+"_"+(row+1);
                assertEquals(val, table.getValue(colName, row));
                ++col;
            }
        }
    }
}

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class TableTest extends Assert
{
    @Test
    public void tableImplTest()
    {
        TableImpl table = 
                new TableImpl(new String[]{"col1", "col2"})
                .addRow(new Object[]{"1_1", "2_1"})
                .addRow(new Object[]{"1_2", "2_2"});
        checkTable(table);
    }

    @Test
    public void columnBasedTableTest()
    {
        ColumnBasedTable table = new ColumnBasedTable();
        table.addValue("col1", "1_1");
        table.addValue("col1", "1_2");
        table.addValue("col2", "2_1");
        table.addValue("col2", "2_2");
        table.freeze();
        checkTable(table);
    }

    private void checkTable(Table table)
    {
        assertArrayEquals(new String[]{"col1", "col2"}, table.getColumnNames());
        List<Object[]> rows = new ArrayList<Object[]>();
        for (Iterator<Object[]> it=table.getRowIterator(); it.hasNext();)
            rows.add(it.next());
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{"1_1", "2_1"}, rows.get(0));
        assertArrayEquals(new Object[]{"1_2", "2_2"}, rows.get(1));
    }
}

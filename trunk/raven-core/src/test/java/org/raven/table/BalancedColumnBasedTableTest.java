/*
 * Copyright 2014 Mikhail Titov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.raven.table;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.raven.RavenUtils;

/**
 *
 * @author Mikhail Titov
 */
public class BalancedColumnBasedTableTest extends Assert {
    
    @Test
    public void test1() {
        BalancedColumnBasedTable table = new BalancedColumnBasedTable();
        table.addValue("c1", "c1r1");
        table.addValue("c1", "c1r2");
        table.addValue("c2", "c2r2");
        table.freeze();
        assertArrayEquals(new String[]{"c1","c2"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertArrayEquals(new Object[]{"c1r1",null}, rows.get(0));
        assertArrayEquals(new Object[]{"c1r2","c2r2"}, rows.get(1));
    }    
    
    @Test
    public void test2() {
        BalancedColumnBasedTable table = new BalancedColumnBasedTable();
        table.addValue("c1", "c1r1");
        table.rowAdded();
        table.addValue("c1", "c1r2");
        table.addValue("c2", "c2r2");
        table.freeze();
        assertArrayEquals(new String[]{"c1","c2"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertArrayEquals(new Object[]{"c1r1",null}, rows.get(0));
        assertArrayEquals(new Object[]{"c1r2","c2r2"}, rows.get(1));
    }
        
    @Test
    public void test3() {
        BalancedColumnBasedTable table = new BalancedColumnBasedTable();
        table.addValue("c1", "c1r1");
        table.rowAdded();
        table.addValue("c2", "c2r2");
        table.addValue("c1", "c1r2");
        table.freeze();
        assertArrayEquals(new String[]{"c1","c2"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertArrayEquals(new Object[]{"c1r1",null}, rows.get(0));
        assertArrayEquals(new Object[]{"c1r2","c2r2"}, rows.get(1));
    }    
}

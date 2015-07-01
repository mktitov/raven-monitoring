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

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class HideColumnsTableWrapperTest extends Assert
{
    @Test
    public void test()
    {
        TableImpl table = new TableImpl(new String[]{"col1","col2","col3","col4"});
        HideColumnsTableWrapper wrapper = new HideColumnsTableWrapper(table, new int[]{1,2});
        assertArrayEquals(new String[]{"col1", "col4"}, wrapper.getColumnNames());

        table.addRow(new Object[]{1,2,3,4});
        assertArrayEquals(new Object[]{1,4}, wrapper.getRowIterator().next());
    }

    @Test
    public void test2()
    {
        TableImpl table = new TableImpl(new String[]{"col1","col2","col3","col4"});
        HideColumnsTableWrapper wrapper = new HideColumnsTableWrapper(table, new int[]{1});
        assertArrayEquals(new String[]{"col1", "col3", "col4"}, wrapper.getColumnNames());

        table.addRow(new Object[]{1,2,3,4});
        assertArrayEquals(new Object[]{1,3,4}, wrapper.getRowIterator().next());
    }

    @Test
    public void test3()
    {
        TableImpl table = new TableImpl(new String[]{"col1","col2","col3","col4"});
        HideColumnsTableWrapper wrapper = new HideColumnsTableWrapper(table, new int[]{3});
        assertArrayEquals(new String[]{"col1", "col2", "col3"}, wrapper.getColumnNames());

        table.addRow(new Object[]{1,2,3,4});
        assertArrayEquals(new Object[]{1,2,3}, wrapper.getRowIterator().next());
    }

    @Test
    public void test4()
    {
        TableImpl table = new TableImpl(new String[]{"col1","col2","col3","col4"});
        HideColumnsTableWrapper wrapper = new HideColumnsTableWrapper(table, new int[]{0});
        assertArrayEquals(new String[]{"col2", "col3", "col4"}, wrapper.getColumnNames());

        table.addRow(new Object[]{1,2,3,4});
        assertArrayEquals(new Object[]{2,3,4}, wrapper.getRowIterator().next());
    }

    @Test
    public void test5()
    {
        TableImpl table = new TableImpl(new String[]{"col1","col2","col3","col4"});
        HideColumnsTableWrapper wrapper = new HideColumnsTableWrapper(table, new int[]{0,2});
        assertArrayEquals(new String[]{"col2", "col4"}, wrapper.getColumnNames());

        table.addRow(new Object[]{1,2,3,4});
        assertArrayEquals(new Object[]{2,4}, wrapper.getRowIterator().next());
    }
}
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * Allows to hide some columns from source table
 * @author Mikhail Titov
 */
public class HideColumnsTableWrapper implements Table
{
    private final Table table;
    private final int[] hideColumns;
    private String[] columnNames;

    /**
     * @param table the source table. The table in which we must hide the columns
     * @param hideColumns the array of column indexes (zero based) will hidden in the new table
     */
    public HideColumnsTableWrapper(Table table, int[] hideColumns)
    {
        this.table = table;
        this.hideColumns = hideColumns.clone();
        Arrays.sort(this.hideColumns);

        columnNames = new String[table.getColumnNames().length-hideColumns.length];
        for (int c=0; c<columnNames.length; ++c)
            columnNames[c] = table.getColumnNames()[translateColumn(c)];
    }

    private int translateColumn(int col)
    {
        for (int c: hideColumns)
            if (col>=c)
                ++col;
        return col;
    }

    public String getTitle()
    {
        return table.getTitle();
    }

    public String[] getColumnNames() 
    {
        return columnNames;
    }

    public int getColumnIndex(String columnName)
    {
        for (int c=0; c<columnNames.length; ++c)
            if (columnNames[c].equals(columnName))
                return c;
        return -1;
    }

    public boolean containsColumnTag(int col, String tagId)
    {
        return table.containsColumnTag(translateColumn(col), tagId);
    }

    public boolean containsRowTag(int row, String tagId)
    {
        return table.containsRowTag(row, tagId);
    }

    public Map<String, TableTag> getColumnTags(int col)
    {
        return table.getColumnTags(translateColumn(col));
    }

    public Map<String, TableTag> getRowTags(int row)
    {
        return table.getRowTags(row);
    }

    public Iterator<Object[]> getRowIterator()
    {
        return new RowIteratorWrapper(table.getRowIterator());
    }

    private class RowIteratorWrapper implements Iterator<Object[]>
    {
        private final Iterator<Object[]> iterator;

        public RowIteratorWrapper(Iterator<Object[]> iterator) {
            this.iterator = iterator;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public Object[] next() 
        {
            Object[] row = iterator.next();
            Object[] newRow = new Object[columnNames.length];
            for (int c=0; c<newRow.length; ++c)
                newRow[c] = row[translateColumn(c)];
            return newRow;
        }

        public void remove() {
            iterator.remove();
        }
    }
}

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


/**
 *
 * @author Mikhail Titov
 */
public class TableImpl extends AbstractTable
{
    private final List<Object[]> rows = new ArrayList<Object[]>();

    public TableImpl(String[] columnNames) 
    {
        this.columnNames = columnNames;
    }

    public void replaceColumnNames(String[] columnNames)
    {
        this.columnNames = columnNames;
    }

    public TableImpl addRow(Object[] row)
    {
        if (row==null || row.length!=columnNames.length)
            throw new IllegalArgumentException("Null row or invalid row length");
        rows.add(row);
        return this;
    }

    public Iterator<Object[]> getRowIterator()
    {
        return new RowIterator();
    }

    private class RowIterator implements Iterator<Object[]>
    {
        private int currentRow = 0;
        
        public boolean hasNext() {
            return currentRow<rows.size();
        }

        public Object[] next()
        {
            return rows.get(currentRow++);
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }
        
    }
}

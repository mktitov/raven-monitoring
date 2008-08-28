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

import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Mikhail Titov
 */
public class TableMultiplexer extends AbstractTable
{
    private final List<Table> tables;

    public TableMultiplexer(List<Table> tables)
    {
        this.tables = tables;
        int columnNamesCount = 0;
        for (Table table: tables)
            columnNamesCount+=table.getColumnNames().length;
        columnNames = new String[columnNamesCount];
        int insPos = 0;
        for (Table table: tables)
        {
            int len = table.getColumnNames().length;
            System.arraycopy(table.getColumnNames(), 0, columnNames, insPos, len);
            insPos+=len;
        }
    }

    public Iterator<Object[]> getRowIterator() 
    {
        return new RowIterator();
    }

    private class RowIterator implements Iterator<Object[]>
    {
        private final Iterator<Object[]>[] iterators;

        public RowIterator()
        {
            iterators = new Iterator[tables.size()];
            for (int i=0; i<iterators.length; ++i)
                iterators[i] = tables.get(i).getRowIterator();
        }
        
        public boolean hasNext()
        {
            return iterators[0].hasNext();
        }

        public Object[] next()
        {
            Object[] row = new Object[columnNames.length];
            int insPos = 0;
            for (Iterator<Object[]> it: iterators)
            {
                Object[] tabRow = it.next();
                System.arraycopy(tabRow, 0, row, insPos, tabRow.length);
                insPos+=tabRow.length;
            }
            return row;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported");
        }
    }

}

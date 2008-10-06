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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Mikhail Titov
 */
public class ColumnBasedTable extends AbstractTable
{
    protected final Map<String, List<Object>> cols = new HashMap<String, List<Object>>();
    protected List<String> columnNamesList = new ArrayList<String>();
    private int rowCount;

    public void freeze()
    {
        columnNames = new String[columnNamesList.size()];
        columnNamesList.toArray(columnNames);
        columnNamesList = null;
        rowCount = cols.get(columnNames[0]).size();
    }

    public void addValue(String columnName, Object value)
    {
        List<Object> values = cols.get(columnName);
        if (values==null)
        {
            columnNamesList.add(columnName);
            values = new ArrayList<Object>();
            cols.put(columnName, values);
        }
        values.add(value);
    }

    public Iterator<Object[]> getRowIterator() 
    {
        return new RowIterator();
    }

    private class RowIterator implements Iterator<Object[]>
    {
        private int currentRow = 0;

        public boolean hasNext()
        {
            return currentRow<rowCount;
        }

        public Object[] next()
        {
            Object[] row = new Object[columnNames.length];
            for (int col=0; col<columnNames.length; ++col)
            {
                List<Object> values = cols.get(columnNames[col]);
                Object value = null;
                if (values!=null)
                    value =  currentRow>=values.size()? null : values.get(currentRow);
                row[col] = value;
            }
            ++currentRow;
            return row;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

}

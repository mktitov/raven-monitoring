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
    private final Map<String, List<Object>> cols = new HashMap<String, List<Object>>(); 
    private List<String> columnNamesList = new ArrayList<String>();
    private int rowCount;
    private int curRow = 0;

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
        if (curRow==values.size())
            curRow++;
        else if (values.size()+1<curRow)
        {
            for (int i=1; i<curRow-values.size(); ++i)
                values.add(null);
        }
        values.add(value);
    }

//    private Object getValue(String columnName, int row)
//    {
//        if (ROWNUM_COLUMN_NAME.equals(columnName))
//            return row;
//
//        List<Object> values = cols.get(columnName);
//        if (values==null)
//            return null;
//        else
//            return row>=values.size()? null : values.get(row);
//    }
//
//    public Map<String, Object> getRow(int row)
//    {
//        Map<String, Object> res = new HashMap<String, Object>();
//        res.put(ROWNUM_COLUMN_NAME, row);
//        for (String columnName: columnNamesList)
//            res.put(columnName, getValue(columnName, row));
//        return res;
//    }

//    public int getRowCount()
//    {
//        return cols.size()==0? 0 : cols.get(columnNamesList.get(1)).size();
//    }

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

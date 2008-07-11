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
import java.util.List;
import java.util.Map;

/**
 *
 * @author Mikhail Titov
 */
public class TableImpl implements Table
{
    private final Map<String, List<Object>> cols = new HashMap<String, List<Object>>(); 
    private final List<String> columnNames = new ArrayList<String>();
            
            
    public Map<String, List<Object>> getRows()
    {
        return cols;
    }
    
    public void addValue(String columnName, Object value)
    {
        List<Object> values = cols.get(columnName);
        if (values==null)
        {
            columnNames.add(columnName);
            values = new ArrayList<Object>();
            cols.put(columnName, values);
        }
        values.add(value);
    }

    public List<String> getColumnNames()
    {
        return columnNames;
    }

    public Object getValue(String columnName, int row)
    {
        List<Object> values = cols.get(columnName);
        if (values==null)
            return null;
        else
            return values.get(row);
    }
    
    public Map<String, Object> getRow(int row)
    {
        Map<String, Object> res = new HashMap<String, Object>();
        for (String columnName: columnNames)
            res.put(columnName, getValue(columnName, row));
        return res;
    }

    public int getRowCount()
    {
        return cols.size()==0? 0 : cols.get(columnNames.get(0)).size();
    }

}

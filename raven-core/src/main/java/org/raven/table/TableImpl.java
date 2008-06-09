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
import org.raven.Table;

/**
 *
 * @author Mikhail Titov
 */
public class TableImpl implements Table
{
    private final Map<String, List<Object>> rows = new HashMap<String, List<Object>>(); 
            
            
    public Map<String, List<Object>> getRows()
    {
        return rows;
    }
    
    public void addValue(String columnName, Object value)
    {
        List<Object> values = rows.get(columnName);
        if (values==null)
        {
            values = new ArrayList<Object>();
            rows.put(columnName, values);
        }
        values.add(value);
    }

}

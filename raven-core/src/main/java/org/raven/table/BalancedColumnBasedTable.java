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
import java.util.List;

/**
 *
 * @author Mikhail Titov
 */
public class BalancedColumnBasedTable extends ColumnBasedTable
{
    private int curRow = 0;
    
    @Override
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
}

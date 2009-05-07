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

/**
 *
 * @author Mikhail Titov
 */
public interface Table 
{
//    public final static String ROWNUM_COLUMN_NAME = "#";
    /**
     * Returns the column names
     */
    public String[] getColumnNames();
    /**
     * Returns the column index by its name. If table has not a column with specified name method
     * returns -1.
     * @param columnName the column name
     */
    public int getColumnIndex(String columnName);
    /**
     * Returns rows iterator
     */
    public Iterator<Object[]> getRowIterator();
    /**
     * Returns the value in the specified column and row.
     * @param columnName the name of the column 
     * @param row the row number
     */
//    public Object getValue(String columnName, int row);
//    /**
//     * Returns the row count in the table
//     */
//    public int getRowCount();
//    /**
//     *
//     */
//    public Map<String, List<Object>> getRows();
//    public Map<String, Object> getRow(int row);
//    /**
//     * Adds value for the selected column
//     * @param columnName the name of the column for which value must be added.
//     * @param value the value
//     */
//    public void addValue(String columnName, Object value);
}

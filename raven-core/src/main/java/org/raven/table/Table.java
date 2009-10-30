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
import java.util.Map;

/**
 *
 * @author Mikhail Titov
 */
public interface Table 
{
    /**
     * Returns the table title
     */
    public String getTitle();
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
     * Returns <b>true</b> if the table column contains the tag.
     * @param col column number (zero based)
     * @param tagId unique tag identificator
     */
    public boolean containsColumnTag(int col, String tagId);
    /**
     * Returns <b>true</b> if the row column contains the tag.
     * @param row column number (zero based)
     * @param tagId unique tag identificator
     */
    public boolean containsRowTag(int row, String tagId);
    /**
     * Returns the tags attached to the specefied column or null if no tags where attached to the column.
     * The key of the returned map is {@link TableTag#getId() tag id}
     * @param col column number (zero based)
     */
    public Map<String, TableTag> getColumnTags(int col);
    /**
     * Returns the tags attached to the specefied row or null if no tags where attached to the row.
     * The key of the returned map is {@link TableTag#getId() tag id}
     * @param row row number (zero based)
     */
    public Map<String, TableTag> getRowTags(int row);
}

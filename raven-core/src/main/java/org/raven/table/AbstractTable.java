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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractTable implements Table
{
    private String title;
    private Map<Integer, Map<String, TableTag>> columnsTags;
    private Map<Integer, Map<String, TableTag>> rowsTags;
    
    protected String[] columnNames;

    public String getTitle()
    {
        return title;
    }
    
    public void replaceColumnNames(String[] columnNames)
    {
        this.columnNames = columnNames;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String[] getColumnNames()
    {
        return columnNames;
    }

    public int getColumnIndex(String columnName)
    {
        if (columnNames!=null)
            for (int i=0; i<columnNames.length; ++i)
                if (columnNames[i].equals(columnName))
                    return i;
        return -1;
    }

    public Map<String, TableTag> getColumnTags(int col)
    {
        return getTags(columnsTags, col);
    }

    public Map<String, TableTag> getRowTags(int row)
    {
        return getTags(rowsTags, row);
    }

    public boolean containsColumnTag(int col, String tagId)
    {
        return containsTag(columnsTags, col, tagId);
    }

    public boolean containsRowTag(int row, String tagId)
    {
        return containsTag(rowsTags, row, tagId);
    }

    public void addColumnTag(int col, TableTag tag)
    {
        if (columnsTags==null)
            columnsTags = createTags();
        addTagToTags(columnsTags, col, tag);
    }

    public void addRowTag(int row, TableTag tag)
    {
        if (rowsTags==null)
            rowsTags = createTags();
        addTagToTags(rowsTags, row, tag);
    }

    private Map<Integer, Map<String, TableTag>> createTags()
    {
        return new HashMap<Integer, Map<String, TableTag>>();
    }

    private void addTagToTags(Map<Integer, Map<String, TableTag>> tagsMap, int pos, TableTag tag)
    {
        Map<String, TableTag> tags = tagsMap.get(pos);
        if (tags==null)
        {
            tags = new HashMap<String, TableTag>();
            tagsMap.put(pos, tags);
        }
        tags.put(tag.getId(), tag);
    }

    private Map<String, TableTag> getTags(Map<Integer, Map<String, TableTag>> tagsMap, int pos)
    {
        return tagsMap==null? null : tagsMap.get(pos);
    }

    private boolean containsTag(Map<Integer, Map<String, TableTag>> tagsMap, int pos, String tagId)
    {
        if (tagsMap==null)
            return false;
        else {
            Map<String, TableTag> tags = tagsMap.get(pos);
            return tags==null? false : tags.containsKey(tagId);
        }
    }
}

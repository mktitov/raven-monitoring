/*
 *  Copyright 2010 Mikhail Titov.
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

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Mikhail Titov
 */
public class ColumnGroupImpl implements ColumnGroup
{
    private final String groupName;
    private final int fromColumn;
    private final int toColumn;
    private final List<String> columnNames;

    public ColumnGroupImpl(String groupName, int fromColumn, int toColumn) {
        this.groupName = groupName;
        this.fromColumn = fromColumn;
        this.toColumn = toColumn;
        columnNames = new LinkedList<String>();
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void addColumnName(String columnName)
    {
        columnNames.add(columnName);
    }

    public String getGroupName() {
        return groupName;
    }

    public int getFromColumn() {
        return fromColumn;
    }

    public int getToColumn() {
        return toColumn;
    }

    public boolean isHasNestedColumns()
    {
        return !columnNames.isEmpty();
    }

    public int getColumnCount() {
        return columnNames.size();
    }
}

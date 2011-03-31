/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.ds.impl;

import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemaFieldNode.class, childNodes=ValuePrepareRecordFieldExtension.class)
public class DatabaseRecordFieldExtension extends AbstractRecordFieldExtension
{
    public static final String COLUMN_NAME_ATTR = "columnName";
    @Parameter() @NotNull
    private String columnName;

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();

        NodeAttribute columnNameAttr = getNodeAttribute(COLUMN_NAME_ATTR);
        String colName = columnNameAttr.getValue();
        if (colName==null)
            columnNameAttr.setValue(RavenUtils.nameToDbName(getParent().getName()));
    }

    public String getColumnName()
    {
        return columnName;
    }

    public void setColumnName(String columnName)
    {
        this.columnName = columnName;
    }

    /**
     * Creates the database table column extension for the record schema field. If extention
     * with given name already exists then method will returns null.
     * @param owner the record schema field.
     * @param extensionName the name of the database table column extension.
     * @param columnName the name of the column with which record schema field related
     */
    public static DatabaseRecordFieldExtension create(
            Node owner, String extensionName, String columnName)
    {
        if (owner.getChildren(extensionName)!=null)
            return null;
        DatabaseRecordFieldExtension colExt = new DatabaseRecordFieldExtension();
        colExt.setName(extensionName);
        owner.addAndSaveChildren(colExt);
        colExt.setColumnName(columnName);
        colExt.start();
        
        return colExt;
    }
}

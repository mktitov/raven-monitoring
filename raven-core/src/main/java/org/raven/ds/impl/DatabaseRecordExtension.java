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

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordExtensionsNode.class)
public class DatabaseRecordExtension extends BaseNode
{
    @Parameter @NotNull
    private String tableName;

    public String getTableName()
    {
        return tableName;
    }

    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }

    /**
     * Creates new database record extension node. If extension with given name already exists
     * method returns null.
     * @param owner the owner of the database record extension
     * @param extensionName the node name of the database record extension
     * @param tableName the table name 
     */
    public final static DatabaseRecordExtension create(
            Node owner,  String extensionName, String tableName)
    {
        if (owner.getChildren(extensionName)!=null)
            return null;
        DatabaseRecordExtension dbExt = new DatabaseRecordExtension();
        dbExt.setName(extensionName);
        owner.addAndSaveChildren(dbExt);
        dbExt.setTableName(tableName);
        dbExt.start();

        return dbExt;
    }
}

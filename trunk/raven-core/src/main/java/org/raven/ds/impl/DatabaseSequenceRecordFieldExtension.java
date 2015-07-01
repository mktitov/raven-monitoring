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

package org.raven.ds.impl;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemaFieldNode.class)
public class DatabaseSequenceRecordFieldExtension  extends AbstractRecordFieldExtension
{
    @NotNull @Parameter
    private String sequenceName;

    public String getSequenceName() {
        return sequenceName;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    /**
     * Creates the database table column extension for the record schema field. If extention
     * with given name already exists then method will returns null.
     * @param owner the record schema field.
     * @param extensionName the name of the database table column extension.
     * @param sequenceName the name of the database sequence which related with field
     */
    public final static DatabaseSequenceRecordFieldExtension create(
            Node owner, String extensionName, String sequenceName)
    {
        if (owner.getNode(extensionName)!=null)
            return null;
        DatabaseSequenceRecordFieldExtension seqExt = new DatabaseSequenceRecordFieldExtension();
        seqExt.setName(extensionName);
        owner.addAndSaveChildren(seqExt);
        seqExt.setSequenceName(sequenceName);
        seqExt.start();

        return seqExt;
    }
}

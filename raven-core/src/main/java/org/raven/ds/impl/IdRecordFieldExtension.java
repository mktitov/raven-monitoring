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
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemaFieldNode.class)
public class IdRecordFieldExtension extends BaseNode
{
    /**
     * Creates id extension for the record schema field. If field already has extension with given
     * name method returns null.
     * @param owner the record schema field node
     * @param extenstionName the name of the extension
     */
    public static IdRecordFieldExtension create(Node owner, String extenstionName)
    {
        if (owner.getChildren(extenstionName)!=null)
            return null;
        IdRecordFieldExtension idExt = new IdRecordFieldExtension();
        idExt.setName(extenstionName);
        owner.addAndSaveChildren(idExt);
        idExt.start();

        return idExt;
    }
}

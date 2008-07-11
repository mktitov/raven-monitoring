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

import org.raven.annotations.NodeClass;
import org.raven.template.TemplateEntry;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.Description;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=TableNode.class, anyChildTypes=true)
@Description("The template for creation child nodes in the TableNode")
public class TableNodeTemplate extends TemplateEntry
{
    public final static String NAME="Template";
    public final static String TABLE_COLUMN_NAME = "tableColumnName";
    public final static String TABLE_INDEX_COLUMN_NAME = "indexTableColumnName";

    public TableNodeTemplate()
    {
        setName(NAME);
        setSubtreeListener(true);
    }

    @Override
    public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus)
    {
        if (newStatus==Status.INITIALIZED)
        {
            if (node.getNodeAttribute(TABLE_COLUMN_NAME)==null)
            {
                NodeAttribute attr = new NodeAttributeImpl(
                        TABLE_COLUMN_NAME, String.class, null
                        , "The reference to the table column name");
                attr.setOwner(node);
                node.addNodeAttribute(attr);
                try
                {
                    attr.init();
                } catch (Exception ex)
                {
                    getLogger().error(String.format(
                            "Error creating attribute (%s)", TABLE_COLUMN_NAME, ex));
                }
                attr.save();
                configurator.getTreeStore().saveNodeAttribute(attr);
            }
        }
    }

    
}

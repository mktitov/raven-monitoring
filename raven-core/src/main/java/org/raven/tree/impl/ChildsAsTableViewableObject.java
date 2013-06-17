/*
 *  Copyright 2011 Mikhail Titov.
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

package org.raven.tree.impl;

import java.util.List;
import org.raven.table.Table;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.weda.internal.annotations.Message;

/**
 * The purpose of this viewable object is to show child nodes as a table. The columns of the table can show
 * the values of the specified attributes
 * @author Mikhail Titov
 */
public class ChildsAsTableViewableObject implements ViewableObject
{
    private final Table table;

    @Message
    private static String nodeNameColumn;

    /**
     * Creates table from <b>owner</b> child nodes
     * @param owner the child of this node will be used on table creation
     * @param attrNames the attribute names of child nodes values of which will form the table columns
     * @param attrTitles the title for attributes that will be displayed in the table column headers
     * @param visualizer allows to tune the attribute value
     * @throws Exception when something wrong
     */
    public ChildsAsTableViewableObject(Node owner, String[] attrNames, String[] attrTitles, 
            AttributeValueVisualizer visualizer) 
        throws Exception
    {
        List<Node> childs = owner.getNodes();
        TableImpl _table = null;
        if (!childs.isEmpty()){
            if (attrNames.length!=attrTitles.length)
                throw new Exception("The number of attrNames and attrTitles must be the same and must not be empty");
            String[] colNames = new String[attrTitles.length+1];
            System.arraycopy(attrTitles, 0, colNames, 1, attrTitles.length);
            colNames[0] = nodeNameColumn;
            _table = new TableImpl(colNames);
            for (Node child: childs) {
                Object[] row = new Object[attrNames.length+1];
                row[0] = new ViewableObjectImpl(
                        Viewable.RAVEN_NODE_MIMETYPE, child.getPath(), child.getName());
                for (int i=1; i<row.length; ++i) {
                    NodeAttribute attr = child.getAttr(attrNames[i-1]);                    
                    if (attr!=null)
                        row[i] = visualizer==null? attr.getValue() : visualizer.visualize(child, attr);
                }
                _table.addRow(row);
            }
        }
        table = _table;
    }

    /**
     * Creates table from <b>owner</b> child nodes
     * @param owner the child of this node will be used on table creation
     * @param attrNames the attribute names of child nodes values of which will form the table columns
     * @param attrTitles the title for attributes that will be displayed in the table column headers
     * @throws Exception when something wrong
     */
    public ChildsAsTableViewableObject(Node owner, String[] attrNames, String[] attrTitles) 
        throws Exception
    {
        this(owner, attrNames, attrTitles, null);
    }
    
    public String getMimeType() {
        return Viewable.RAVEN_TABLE_MIMETYPE;
    }

    public Object getData() {
        return table;
    }

    public boolean cacheData() {
        return false;
    }

    public int getWidth() {
        return 0;
    }

    public int getHeight() {
        return 0;
    }
}

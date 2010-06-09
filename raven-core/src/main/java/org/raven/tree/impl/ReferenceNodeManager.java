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

package org.raven.tree.impl;

import java.util.Collection;
import java.util.Iterator;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.table.Table;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class ReferenceNodeManager extends AbstractDataConsumer
{
    @Parameter(defaultValue="1")
    @NotNull
    private Integer tableColumn;

    public Integer getTableColumn() {
        return tableColumn;
    }

    public void setTableColumn(Integer tableColumn) {
        this.tableColumn = tableColumn;
    }

    @Override
    protected void doStart() throws Exception
    {
        if (tableColumn<1)
            throw new Exception(
                    "Invalid value for (tableColumn) attribute. The value must be greater than 0");
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context)
    {
        Collection<Node> childs = getChildrens();
        if (childs!=null)
            for (Node child: childs)
                tree.remove(child);

        if (!(data instanceof Table))
        {
            if (data==null)
                logger.warn(String.format(
                        "Error in the node (%s). Null data recieved from (%s)"
                        , getPath(), dataSource.getPath()));
            else
                logger.warn(String.format(
                        "Error in the node (%s). Invalid data type (%s) recieved from (%s). " +
                        "Expecting (%s) data type"
                        , getPath(), data.getClass().getName(), dataSource.getPath()
                        , Table.class.getName()));
            return;
        }

        Table table = (Table) data;
        int column = tableColumn;
        if (column>table.getColumnNames().length)
            logger.error(String.format(
                    "Error in the node (%s). Invalid column index (%d). Table has (%d) columns"
                    , getPath(), column, table.getColumnNames().length));

        int ind=1;
        for (Iterator<Object[]> it = table.getRowIterator(); it.hasNext();)
        {
            Object[] row = it.next();
            if (!(row[column-1] instanceof Node))
            {
                if (row[column-1]==null)
                    logger.error(String.format(
                            "Error in the node (%s). Null value in the table column (%d)"
                            , getPath(), column));
                else
                    logger.error(String.format(
                            "Error in the node (%s). Invalid value type (%s) in the table " +
                            "column (%d). Expecting (%s) type"
                            , getPath(), row[column-1].getClass().getName(), column
                            , Node.class.getName()));
            }
            else
            {
                Node node = (Node) row[column-1];
                ReferenceNode ref = new ReferenceNode();
                ref.setName(node.getName()+"_"+(ind++));
                addChildren(ref);
                ref.save();
                ref.init();
                ref.setReference(node);
                ref.start();
            }
        }
    }
}

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

package org.raven.tree.impl.objects;

import java.util.Collection;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class NodesDataSource extends BaseNode implements DataSource
{
    private Node[] nodes;

    public void setNodes(Node... nodes)
    {
        this.nodes = nodes;
    }
    
    public Boolean getStopProcessingOnError() {
        return false;
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        TableImpl table = new TableImpl(new String[]{"name", "node"});
        for (Node node: nodes)
            table.addRow(new Object[]{node.getName(), node});
        dataConsumer.setData(this, table, context);
        return true;
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }

}

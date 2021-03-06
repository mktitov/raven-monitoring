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

package org.raven.table.objects;

import java.util.Collection;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.table.ColumnBasedTable;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class TestTableDataSource extends BaseNode implements DataSource
{
    private boolean sendTitle = false;
    private boolean sendTwoTable = false;

    public boolean isSendTwoTable()
    {
        return sendTwoTable;
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    public void setSendTwoTable(boolean sendTwoTable)
    {
        this.sendTwoTable = sendTwoTable;
    }

    public boolean isSendTitle()
    {
        return sendTitle;
    }

    public void setSendTitle(boolean sendTitle)
    {
        this.sendTitle = sendTitle;
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        ColumnBasedTable table = new ColumnBasedTable();
        if (sendTitle)
            table.setTitle("title");
        table.addValue("col1", "val_1_1");
        table.addValue("col2", "val_2_1");
        table.addValue("col1", "val_1_2");
        table.addValue("col2", "val_2_2");
        table.freeze();
        
        dataConsumer.setData(this, table, context);
        if (sendTwoTable)
            dataConsumer.setData(this, table, context);

        return true;
    }

    public Collection<NodeAttribute> generateAttributes() 
    {
        return null;
    }

}

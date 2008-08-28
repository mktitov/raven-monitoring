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
import org.raven.ds.DataSource;
import org.raven.table.Table;
import org.raven.table.ColumnBasedTable;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class TestDataSource extends BaseNode implements DataSource
{

    public TestDataSource()
    {
    }
    
    public TestDataSource(String name)
    {
        super(name);
    }
    
    public void getDataImmediate(DataConsumer dataConsumer)
    {
        Table table = createTable();
        
        dataConsumer.setData(this, table);
    }
    
    public void pushData() throws Exception
    {
        if (getDependentNodes()==null)
            throw new Exception("No dependencies to the data source");
        
        Table table = createTable();
        for (Node node: getDependentNodes())
        {
            if (node instanceof DataConsumer)
                ((DataConsumer)node).setData(this, table);
        }
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }

    public void pushDataWithNewRow() throws Exception
    {
        if (getDependentNodes()==null)
            throw new Exception("No dependencies to the data source");
        
        TableImpl table = createTable().addRow(new Object[]{"value1_3", "value2_3"});
        
        for (Node node: getDependentNodes())
        {
            if (node instanceof DataConsumer)
                ((DataConsumer)node).setData(this, table);
        }
    }
    
    private TableImpl createTable()
    {
        TableImpl table = 
                new TableImpl(new String[]{"column1", "column2"})
                .addRow(new Object[]{"value1_1", "value2_1"})
                .addRow(new Object[]{"value1_2", "value2_2"});
        
        return table;
    }
}

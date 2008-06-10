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
        TableImpl table = new TableImpl();
        table.addValue("column1", "value1_1");
        table.addValue("column1", "value1_2");
        
        table.addValue("column2", "value2_1");
        table.addValue("column2", "value2_2");
        
        dataConsumer.setData(this, table);
    }
    
    public void pushData() throws Exception
    {
        if (getDependentNodes()==null)
            throw new Exception("No dependencies to the data source");
        TableImpl table = new TableImpl();
        table.addValue("column1", "value1_1");
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
}

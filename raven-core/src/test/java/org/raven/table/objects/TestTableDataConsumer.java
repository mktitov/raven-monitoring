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

import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.table.TableImpl;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class TestTableDataConsumer extends BaseNode implements DataConsumer
{
    public void setData(DataSource dataSource, Object data) {
    }

    public Object refereshData() 
    {
        TableImpl table = new TableImpl();
        table.addValue("col3", "val_3_1");
        table.addValue("col4", "val_4_1");
        table.addValue("col3", "val_3_2");
        table.addValue("col4", "val_4_2");
    
        return table;
    }
    
}

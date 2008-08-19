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

import java.util.Collection;
import java.util.List;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataPipe;
import org.raven.ds.impl.AbstractDataMultiplexer;
import org.weda.annotations.Description;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes={DataConsumer.class, DataPipe.class})
@Description("Allows to mulitiplex several tables in one table.")
public class TableMultiplexerNode extends AbstractDataMultiplexer<Table, Table>
{
    public TableMultiplexerNode() 
    {
        super(Table.class);
    }

    @Override
    public Table multiplex(List<Table> listOfData) 
    {
        if (listOfData.size()==1)
            return listOfData.get(0);
        
        TableImpl resTable = new TableImpl();
        for (int i=0; i<listOfData.get(0).getRowCount(); ++i)
        {
            for (Table table: listOfData)
            {
                for (String columnName: table.getColumnNames())
                    if (!Table.ROWNUM_COLUMN_NAME.equals(columnName))
                        resTable.addValue(columnName, table.getValue(columnName, i));
            }
        }
        
        return resTable;
    }
}

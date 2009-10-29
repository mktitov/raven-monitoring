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

package org.raven.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.raven.annotations.NodeClass;
import org.raven.ds.AggregateFunction;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass()
public class TableSummaryNode extends AbstractSafeDataPipe
{
    @Override
    protected void doSetData(DataSource dataSource, Object data) throws Exception
    {
        if (!(data instanceof Table))
            return;

        Table table = (Table) data;
        List<TableValuesAggregatorNode> colAggDefs = null;
        List<TableValuesAggregatorNode> rowAggsDefs = null;
        Collection<Node> childs = getSortedChildrens();
        if (childs!=null)
            for (Node child: childs)
                if (   Status.STARTED.equals(child.getStatus())
                    && child instanceof TableValuesAggregatorNode)
                {
                    TableValuesAggregatorNode agg = (TableValuesAggregatorNode) child;
                    switch(agg.getAggregationDirection())
                    {
                        case COLUMN :
                            if (colAggDefs==null)
                                colAggDefs = new ArrayList<TableValuesAggregatorNode>(2);
                            colAggDefs.add(agg);
                            break;
                        case ROW:
                            if (rowAggsDefs==null)
                                rowAggsDefs = new ArrayList<TableValuesAggregatorNode>(2);
                            rowAggsDefs.add(agg);

                    }
                }
            
        if (colAggDefs==null && rowAggsDefs==null)
            return;

        TableImpl newTable = new TableImpl(table.getColumnNames());

        AggregateFunction[][] colAggs = createColumnAggregations(colAggDefs, table);
        Iterator<Object[]> it = table.getRowIterator();
        while (it!=null && it.hasNext())
        {
            Object[] row = it.next();
            newTable.addRow(row);
            if (colAggDefs!=null)
                for (int i=0; i<row.length; ++i)
                    for (int j=0; j<colAggDefs.size(); ++j)
                        if (colAggs[i][j]!=null)
                            colAggs[i][j].aggregate(row[i]);
        }
        if (colAggDefs!=null)
            for (int j=0; j<colAggDefs.size(); ++j)
            {
                Object[] row = new Object[newTable.getColumnNames().length];
                row[0] = colAggDefs.get(j).getTitle();
                for (int i=0; i<table.getColumnNames().length; ++i)
                {
                    AggregateFunction func = colAggs[i][j];
                    if (func!=null)
                    {
                        func.finishAggregation();
                        row[i]=func.getAggregatedValue();
                        func.close();
                    }
                }
                newTable.addRow(row);
            }

        sendDataToConsumers(newTable);
    }

    private AggregateFunction[][] createColumnAggregations(
            List<TableValuesAggregatorNode> colAggDefs, Table table)
        throws Exception
    {
        if (colAggDefs != null)
        {
            AggregateFunction[][] colAggs =
                    new AggregateFunction[table.getColumnNames().length][colAggDefs.size()];
            try
            {
                for (int i = 0; i < colAggs.length; ++i)
                {
                    bindingSupport.put("columnNumber", i + 1);
                    for (int j = 0; j < colAggDefs.size(); ++j)
                    {
                        TableValuesAggregatorNode aggDef = colAggDefs.get(j);
                        if (aggDef.getSelector())
                        {
                            colAggs[i][j] = aggDef.createAggregateFunction();
                            colAggs[i][j].startAggregation();
                        }
                    }
                }
            } finally
            {
                bindingSupport.reset();
            }
            
            return colAggs;
        }

        return null;
    }
}

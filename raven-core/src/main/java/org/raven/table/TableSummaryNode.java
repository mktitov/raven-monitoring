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
    public static final String COLUMN_NUMBER_BINDING = "columnNumber";
    public static final String AGGREGATION_TAG_ID = "AGGREGATION";
    public static final TableTag AGGREGATION_TAG = new TableTagImpl("AGGREGATION");

    @Override
    protected void doSetData(DataSource dataSource, Object data) throws Exception
    {
        if (!(data instanceof Table))
        {
            sendDataToConsumers(data);
            return;
        }

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
        {
            sendDataToConsumers(data);
            return;
        }

        String[] colNames = table.getColumnNames();
        if (rowAggsDefs!=null)
        {
            colNames = new String[colNames.length+rowAggsDefs.size()];
            System.arraycopy(table.getColumnNames(), 0, colNames, 0, table.getColumnNames().length);
            for (int i=0; i<rowAggsDefs.size(); ++i){
                colNames[table.getColumnNames().length+i]=rowAggsDefs.get(i).getTitle();
            }
        }
        
        TableImpl newTable = new TableImpl(colNames);
        if (rowAggsDefs!=null)
            for (int i=table.getColumnNames().length; i<colNames.length; ++i)
                newTable.addColumnTag(i, AGGREGATION_TAG);

        AggregateFunction[][] colAggs = createColumnAggregations(colAggDefs, colNames.length);
        Iterator<Object[]> it = table.getRowIterator();
        int rowNumber=0;
        while (it!=null && it.hasNext())
        {
            ++rowNumber;
            Object[] row = it.next();
            Object[] newRow = row;
            if (rowAggsDefs!=null)
            {
                newRow = new Object[row.length+rowAggsDefs.size()];
                System.arraycopy(row, 0, newRow, 0, row.length);
                for (int i=0; i<rowAggsDefs.size(); ++i)
                {
                    bindingSupport.put("row", row);
                    bindingSupport.put("rowNumber", rowNumber);
                    try
                    {
                        if (rowAggsDefs.get(i).getSelector())
                        {
                            AggregateFunction func = rowAggsDefs.get(i).createAggregateFunction();
                            try{
                                func.startAggregation();
                                for (int j=0; j<row.length; ++j)
                                    func.aggregate(row[j]);
                                func.finishAggregation();
                                newRow[row.length+i] = func.getAggregatedValue();
                            }finally{
                                func.close();
                            }
                        }
                    }
                    finally
                    {
                        bindingSupport.reset();
                    }
                }
            }
            newTable.addRow(newRow);
            if (colAggDefs!=null)
                for (int i=0; i<newRow.length; ++i)
                    for (int j=0; j<colAggDefs.size(); ++j)
                        if (colAggs[i][j]!=null)
                            colAggs[i][j].aggregate(newRow[i]);
        }
        if (colAggDefs!=null)
            for (int j=0; j<colAggDefs.size(); ++j)
            {
                Object[] row = new Object[newTable.getColumnNames().length];
                row[0] = colAggDefs.get(j).getTitle();
                for (int i=0; i<newTable.getColumnNames().length; ++i)
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
                newTable.addRowTag(rowNumber++, AGGREGATION_TAG);
            }

        sendDataToConsumers(newTable);
    }

    private AggregateFunction[][] createColumnAggregations(
            List<TableValuesAggregatorNode> colAggDefs, int len)
        throws Exception
    {
        if (colAggDefs != null)
        {
            AggregateFunction[][] colAggs =
                    new AggregateFunction[len][colAggDefs.size()];
            try
            {
                for (int i = 0; i < colAggs.length; ++i)
                {
                    bindingSupport.put(COLUMN_NUMBER_BINDING, i + 1);
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

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.ds.AggregateFunction;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass()
public class TableSummaryNode extends AbstractSafeDataPipe
{
    public static final String GROUP_COLUMN_BINDING = "groupColumn";
    public static final String GROUP_FROM_COLUMN_BINDING = "groupFromColumn";
    public static final String GROUP_TO_COLUMN_BINDING = "groupToColumn";
    public static final String GROUP_VALUE_BINDING = "groupValue";
    public static final String COLUMN_NUMBER_BINDING = "columnNumber";
    public static final String ORIG_COLUMN_NAMES_BINDING = "origColumnNames";
    public static final String ROW_BINDING = "row";
    public static final String ORIG_ROW_BINDING = "origRow";
    public static final String COLUMN_NAMES_BINDINGS = "columnNames";
    public static final String COLUMN_NAME_BINDING = "columnName";
    public static final String ROW_NUMBER_BINDING = "rowNumber";
    public static final String AGGREGATION_TAG_ID = "AGGREGATION";
    public static final TableTag AGGREGATION_TAG = new TableTagImpl("AGGREGATION");

    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        if (!(data instanceof Table))
        {
            sendDataToConsumers(data, context);
            return;
        }


        Table table = (Table) data;

        RowAggInfo rowAggInfo = new RowAggInfo(table, context);

        List<TableValuesAggregatorNode> colAggDefs = null;
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
                    }
                }
            
        if (!rowAggInfo.hasAggregators() && colAggDefs==null)
        {
            sendDataToConsumers(data, context);
            return;
        }

        TableImpl newTable = rowAggInfo.table;

        AggregateFunction[][] colAggs = createColumnAggregations(
                colAggDefs, newTable.getColumnNames().length, context);
        Iterator<Object[]> it = table.getRowIterator();
        int rowNumber=0;
        while (it!=null && it.hasNext())
        {
            ++rowNumber;
            Object[] row = it.next();
            Object[] newRow = rowAggInfo.aggregate(row, rowNumber);
            newTable.addRow(newRow);
            if (colAggDefs!=null) {
                bindingSupport.put(ROW_BINDING, newRow);
                bindingSupport.put(ROW_NUMBER_BINDING, rowNumber);
                bindingSupport.put(ORIG_ROW_BINDING, row);
                bindingSupport.put(COLUMN_NAMES_BINDINGS, newTable.getColumnNames());
                bindingSupport.put(ORIG_COLUMN_NAMES_BINDING, table.getColumnNames());
                bindingSupport.put(DATA_CONTEXT_BINDING, context);
                try{
                    for (int i=0; i<newRow.length; ++i)
                        for (int j=0; j<colAggDefs.size(); ++j)
                            if (colAggs[i][j]!=null){
                                bindingSupport.put(COLUMN_NAME_BINDING, newTable.getColumnNames()[i]);
                                bindingSupport.put(COLUMN_NUMBER_BINDING, i+1);
                                try{
                                    colAggs[i][j].aggregate(newRow[i]);
                                }catch(Exception e){
                                    if (isLogLevelEnabled(LogLevel.DEBUG))
                                        debug("Aggregation error", e);
                                }
                            }
                }finally{
                    bindingSupport.reset();
                }
            }
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

        sendDataToConsumers(newTable, context);
    }

    private AggregateFunction[][] createColumnAggregations(
            List<TableValuesAggregatorNode> colAggDefs, int len, DataContext context)
        throws Exception
    {
        if (colAggDefs != null)
        {
            AggregateFunction[][] colAggs =
                    new AggregateFunction[len][colAggDefs.size()];
            try
            {
                bindingSupport.put(DATA_CONTEXT_BINDING, context);
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

    private class RowAggInfo
    {
        private final String[] colnames;
        private final String[] sourceColnames;
        private final RowAgg[] aggDefs;
        private final TableImpl table;
        private final DataContext context;

        private RowAggInfo(Table sourceTable, DataContext context)
        {
            sourceColnames = sourceTable.getColumnNames();
            this.context = context;
            LinkedList<String> namesList = new LinkedList<String>();
            List<Node> childs = getSortedChildrens();
            Map<String, Object> values = new HashMap<String, Object>();
            Map<String, Integer> froms = new HashMap<String, Integer>();
            List<RowAgg> aggDefsList = new LinkedList<RowAgg>();
            boolean lastColumnNameAdded = false;
            int add=0;
            if (childs!=null && !childs.isEmpty())
                for (int i=0; i<sourceColnames.length; ++i)
                {
                    for (Node child: childs)
                        if (   Status.STARTED.equals(child.getStatus())
                            && child instanceof TableValuesAggregatorNode
                            && ((TableValuesAggregatorNode)child).getAggregationDirection()==AggregationDirection.ROW)
                        {
                            TableValuesAggregatorNode aggDef = (TableValuesAggregatorNode)child;
                            bindingSupport.put(COLUMN_NAME_BINDING, sourceColnames[i]);
                            bindingSupport.put(COLUMN_NUMBER_BINDING, (i+1));
                            bindingSupport.put(DATA_CONTEXT_BINDING, context);
                            try{
                                Object val = aggDef.getGroupExpression();
                                Object groupValue=values.get(aggDef.getName());
                                if (!values.containsKey(aggDef.getName()))
                                {
                                    values.put(aggDef.getName(), val);
                                    froms.put(aggDef.getName(), i);
                                }
                                else if (!ObjectUtils.equals(val, groupValue))
                                {
                                    bindingSupport.put(GROUP_VALUE_BINDING, groupValue);
                                    RowAgg rowAgg = new RowAgg(aggDef, i+add, froms.get(aggDef.getName()), i-1);
                                    if (addGroup(rowAgg, aggDefsList, namesList))
                                        ++add;
                                    values.put(aggDef.getName(), val);
                                    froms.put(aggDef.getName(), i);
                                    groupValue = val;
                                }
                                if (i==sourceColnames.length-1)
                                {
                                    bindingSupport.put(GROUP_VALUE_BINDING, groupValue);
                                    if (!lastColumnNameAdded)
                                    {
                                        lastColumnNameAdded = true;
                                        namesList.add(sourceColnames[i]);
                                    }
                                    RowAgg rowAgg = new RowAgg(aggDef, i+1+add, froms.get(aggDef.getName()), i);
                                    if (addGroup(rowAgg, aggDefsList, namesList))
                                        ++add;
                                }
                            }finally{
                                bindingSupport.reset();
                            }
                        }
                    if (!lastColumnNameAdded)
                        namesList.add(sourceColnames[i]);
                }
            colnames = new String[namesList.size()];
            namesList.toArray(colnames);

            table = new TableImpl(colnames);
            table.setTitle(sourceTable.getTitle());
            aggDefs = new RowAgg[aggDefsList.size()];
            aggDefsList.toArray(aggDefs);
            for (RowAgg aggDef: aggDefs)
                table.addColumnTag(aggDef.index, AGGREGATION_TAG);
        }

        private boolean hasAggregators()
        {
            return aggDefs.length>0;
        }

        private Object[] aggregate(Object[] row, int rowNumber) throws Exception
        {
            try
            {
                bindingSupport.put(ROW_BINDING, row);
                bindingSupport.put(COLUMN_NAMES_BINDINGS, sourceColnames);
                bindingSupport.put(ROW_NUMBER_BINDING, rowNumber);
                bindingSupport.put(DATA_CONTEXT_BINDING, context);
                try
                {
                    //start aggregations
                    for (RowAgg rowAgg: aggDefs)
                        rowAgg.start();
                    //aggregate
                    List<Object> newRow = new LinkedList<Object>();
                    for (int i=0; i<row.length; ++i)
                    {
                        newRow.add(row[i]);
                        for (RowAgg rowAgg: aggDefs)
                            if (i>=rowAgg.from && i<=rowAgg.to)
                            {
                                bindingSupport.put(COLUMN_NAME_BINDING, sourceColnames[i]);
                                bindingSupport.put(COLUMN_NUMBER_BINDING, i+1);
                                try{
                                    rowAgg.aggregate(row[i]);
                                }catch(Exception e){
                                    if (isLogLevelEnabled(LogLevel.DEBUG))
                                        debug("Aggregation error", e);
                                }
                            }
                    }
                    //finish aggrgations
                    for (RowAgg aggDef: aggDefs)
                    {
                        aggDef.finish();
                        newRow.add(aggDef.index, aggDef.getAggregatedValue());
                    }

                    return newRow.toArray();
                    
                }
                finally
                {
                    bindingSupport.reset();
                }

            }
            finally
            {
                for (RowAgg rowAgg: aggDefs)
                    rowAgg.resetFunc();
            }
        }

        private boolean addGroup(RowAgg rowAgg, List<RowAgg> aggDefsList, LinkedList<String> namesList)
        {
            bindingSupport.put(GROUP_COLUMN_BINDING, rowAgg.index+1);
            bindingSupport.put(GROUP_FROM_COLUMN_BINDING, rowAgg.from+1);
            bindingSupport.put(GROUP_TO_COLUMN_BINDING, rowAgg.to+1);
            Boolean validGroup = rowAgg.factory.getGroupValidatorExpression();
            if (validGroup!=null && validGroup)
            {
                aggDefsList.add(rowAgg);
                namesList.add(rowAgg.factory.getTitle());
                return true;
            }
            return false;
        }
    }

    private class RowAgg
    {
        private TableValuesAggregatorNode factory;
        private int index;
        private int from, to;
        private AggregateFunction func;

        public RowAgg(TableValuesAggregatorNode factory, int index, int from, int to)
        {
            this.factory = factory;
            this.index = index;
            this.from = from;
            this.to = to;
        }

        void start() throws Exception
        {
            func = null;
            if (factory.getSelector())
            {
                func = factory.createAggregateFunction();
                func.startAggregation();
            }
        }

        void aggregate(Object value)
        {
            if (func!=null)
                func.aggregate(value);
        }

        void finish()
        {
            if (func!=null)
                func.finishAggregation();
        }

        Object getAggregatedValue()
        {
            return func==null? null : func.getAggregatedValue();
        }

        void resetFunc()
        {
            if (func!=null)
                func.close();
            func = null;
        }
    }
}

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

import java.util.Collections;
import java.util.Iterator;
import org.raven.tree.impl.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.impl.SafeDataConsumer;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(anyChildTypes=true)
public class TableViewNode extends SafeDataConsumer implements Viewable
{
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String cellValueExpression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useCellValueExpression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean autoRefresh;

    @Parameter
    private String hideColumns;

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;

        List dataList = (List) refereshData(refreshAttributes==null? null : refreshAttributes.values());
        if (dataList!=null)
        {
            List<ViewableObject> voList = new LinkedList<ViewableObject>();
            for (Object obj: dataList)
            {
                Table table = converter.convert(Table.class, obj, null);
                if (table!=null)
                {
                    if (useCellValueExpression)
                        table = transformTable(table);
                    if (!voList.isEmpty())
                        voList.add(new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, "<br>"));
                    if (table.getTitle()!=null)
                        voList.add(new ViewableObjectImpl(
                                RAVEN_TEXT_MIMETYPE, "<b>"+table.getTitle()+"</b>"));
                    table = hideColumnsIfNeed(table);
                    ViewableObject tableVO = new ViewableObjectImpl(RAVEN_TABLE_MIMETYPE, table);
                    voList.add(new ViewableObjectImpl(RAVEN_TABLE_MIMETYPE, table));
                }
            }
            return voList.isEmpty()? null : voList;
        }
        else
            return null;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return NodeUtils.extractRefereshAttributes(this);
    }

    public void setAutoRefresh(Boolean autoRefresh)
    {
        this.autoRefresh = autoRefresh;
    }

    public Boolean getAutoRefresh()
    {
        return autoRefresh;
    }

    public String getCellValueExpression()
    {
        return cellValueExpression;
    }

    public void setCellValueExpression(String cellValueExpression)
    {
        this.cellValueExpression = cellValueExpression;
    }

    public Boolean getUseCellValueExpression()
    {
        return useCellValueExpression;
    }

    public void setUseCellValueExpression(Boolean useCellValueExpression)
    {
        this.useCellValueExpression = useCellValueExpression;
    }

    public String getHideColumns() {
        return hideColumns;
    }

    public void setHideColumns(String hideColumns) {
        this.hideColumns = hideColumns;
    }

    private Table transformTable(Table table)
    {
        String[] columnNames = table.getColumnNames();
        TableImpl newTable = new TableImpl(columnNames);
        newTable.setTitle(table.getTitle());
        Iterator<Object[]> it = table.getRowIterator();
        int rowNumber = 1;
        NodeAttribute attr = getNodeAttribute("cellValueExpression");
        Map<String, TableTag>[] columnTags = new Map[newTable.getColumnNames().length];
        for (int i=0; i<columnTags.length; ++i)
        {
            Map<String, TableTag> tags = table.getColumnTags(i);
            columnTags[i] = tags==null? Collections.EMPTY_MAP : tags;
        }
        try
        {
            while (it!=null && it.hasNext())
            {
                Object[] row = it.next();
                bindingSupport.put("rowNumber", rowNumber);
                bindingSupport.put("row", row);
                Map<String, TableTag> rowTags = table.getRowTags(rowNumber-1);
                bindingSupport.put("rowTags", rowTags==null? Collections.EMPTY_MAP : rowTags);
                Object[] newRow = new Object[row.length];
                for (int col=0; col<row.length; ++col)
                {
                    bindingSupport.put("columnNumber", (col+1));
                    bindingSupport.put("value", row[col]);
                    bindingSupport.put("columnTags", columnTags[col]);
                    bindingSupport.put("columnName", columnNames[col]);
                    newRow[col] = attr.getRealValue();
                }
                newTable.addRow(newRow);
                ++rowNumber;
            }
        }
        finally
        {
            bindingSupport.reset();
        }

        return newTable;
    }

    private Table hideColumnsIfNeed(Table table)
    {
        String[] strCols = RavenUtils.split(hideColumns);
        if (strCols!=null){
            int[] cols = new int[strCols.length];
            for (int i=0; i<cols.length; ++i)
                cols[i]=new Integer(strCols[i])+1;
            return new HideColumnsTableWrapper(table, cols);
        }
        return table;
    }
}

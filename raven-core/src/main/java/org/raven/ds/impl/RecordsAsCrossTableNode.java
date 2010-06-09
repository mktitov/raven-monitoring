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

package org.raven.ds.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.table.TableImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class RecordsAsCrossTableNode extends AbstractSafeDataPipe
{
    public static final String NEW_TABLE_FIELDS_EXPRESSION_ATTR = "newTableFieldsExpression";
    public static final String CELLVALUE_FIELDS_EXPRESSION_ATTR = "cellValueFieldsExpression";
    public static final String MASTER_FIELDS_EXPRESSION_ATTR = "masterFieldsExpression";
    public static final String SECONDARY_FIELDS_EXPRESSION_ATTR = "secondaryFieldsExpression";

    @Parameter
    private String newTableFields;
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String newTableFieldsExpression;
    @NotNull @Parameter(defaultValue="false")
    private Boolean useNewTableFieldsExpression;
    
    @NotNull @Parameter
    private String masterFields;
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String masterFieldsExpression;    
    @NotNull @Parameter(defaultValue="false")
    private Boolean useMasterFieldsExpression;

    @Parameter
    private String secondaryFields;
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String secondaryFieldsExpression;    
    @NotNull @Parameter(defaultValue="false")
    private Boolean useSecondaryFieldsExpression;

    @NotNull @Parameter
    private String cellValueFields;
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String cellValueFieldsExpression;
    @NotNull @Parameter(defaultValue="false")
    private Boolean useCellValueFieldsExpression;

    @Parameter
    private String firstColumnName;

    private String[] newTableFieldNames;
    private String[] masterFieldNames;
    private String[] secondaryFieldNames;
    private String[] cellValueFieldNames;

    private ThreadLocal<State> states;

    @Override
    protected void initFields()
    {
        super.initFields();
        states = new ThreadLocal<State>();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        if (!useSecondaryFieldsExpression && secondaryFields==null)
            throw new Exception(
                    "One of the attributes \"secondaryFields\" or \"secondaryFieldsExpression\" " +
                    "must be used");
        masterFieldNames = masterFields.split("\\s*,\\s*");
        if (!useSecondaryFieldsExpression)
            secondaryFieldNames = secondaryFields.split("\\s*,\\s*");
        cellValueFieldNames = cellValueFields.split("\\s*,\\s*");
        newTableFieldNames = newTableFields==null? null : newTableFields.split("\\s*,\\s*");
    }

    public String getNewTableFields()
    {
        return newTableFields;
    }

    public void setNewTableFields(String newTableFields)
    {
        this.newTableFields = newTableFields;
    }

    public String getNewTableFieldsExpression()
    {
        return newTableFieldsExpression;
    }

    public void setNewTableFieldsExpression(String newTableFieldsExpression)
    {
        this.newTableFieldsExpression = newTableFieldsExpression;
    }

    public Boolean getUseNewTableFieldsExpression()
    {
        return useNewTableFieldsExpression;
    }

    public void setUseNewTableFieldsExpression(Boolean useNewTableFieldsExpression)
    {
        this.useNewTableFieldsExpression = useNewTableFieldsExpression;
    }

    public String getFirstColumnName()
    {
        return firstColumnName;
    }

    public void setFirstColumnName(String firstColumnName)
    {
        this.firstColumnName = firstColumnName;
    }

    public String getCellValueFields()
    {
        return cellValueFields;
    }

    public void setCellValueFields(String cellValueFields)
    {
        this.cellValueFields = cellValueFields;
    }

    public String getCellValueFieldsExpression()
    {
        return cellValueFieldsExpression;
    }

    public void setCellValueFieldsExpression(String cellValueFieldsExpression)
    {
        this.cellValueFieldsExpression = cellValueFieldsExpression;
    }

    public Boolean getUseCellValueFieldsExpression()
    {
        return useCellValueFieldsExpression;
    }

    public void setUseCellValueFieldsExpression(Boolean useCellValueFieldsExpression)
    {
        this.useCellValueFieldsExpression = useCellValueFieldsExpression;
    }

    public String getMasterFields()
    {
        return masterFields;
    }

    public void setMasterFields(String masterFields)
    {
        this.masterFields = masterFields;
    }

    public String getMasterFieldsExpression()
    {
        return masterFieldsExpression;
    }

    public void setMasterFieldsExpression(String masterFieldsExpression)
    {
        this.masterFieldsExpression = masterFieldsExpression;
    }

    public String getSecondaryFields()
    {
        return secondaryFields;
    }

    public void setSecondaryFields(String secondaryFields)
    {
        this.secondaryFields = secondaryFields;
    }

    public String getSecondaryFieldsExpression()
    {
        return secondaryFieldsExpression;
    }

    public void setSecondaryFieldsExpression(String secondaryFieldsExpression)
    {
        this.secondaryFieldsExpression = secondaryFieldsExpression;
    }

    public Boolean getUseMasterFieldsExpression()
    {
        return useMasterFieldsExpression;
    }

    public void setUseMasterFieldsExpression(Boolean useMasterFieldsExpression)
    {
        this.useMasterFieldsExpression = useMasterFieldsExpression;
    }

    public Boolean getUseSecondaryFieldsExpression()
    {
        return useSecondaryFieldsExpression;
    }

    public void setUseSecondaryFieldsExpression(Boolean useSecondaryFieldsExpression)
    {
        this.useSecondaryFieldsExpression = useSecondaryFieldsExpression;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        if (!Status.STARTED.equals(getStatus()))
            return;

        if (data==null && states.get()!=null)
            formAndSendCrossTable();

        if (!(data instanceof Record))
            return;

        Record rec = (Record) data;
        bindingSupport.put("record", rec);
        try
        {
            boolean newState = states.get()==null;
            State state = getOrCreateState(rec.getSchema(), context);
            Object tableValue = getValue(newTableFieldNames, useNewTableFieldsExpression
                    , NEW_TABLE_FIELDS_EXPRESSION_ATTR, rec);
            if (newState)
                state.setTableValue(tableValue);
            else
            {
                if (!ObjectUtils.equals(tableValue, state.getTableValue()))
                {
                    formAndSendCrossTable();
                    state = getOrCreateState(rec.getSchema(), context);
                    state.setTableValue(tableValue);
                }
            }
            Object masterValue = getValue(masterFieldNames, useMasterFieldsExpression
                    , MASTER_FIELDS_EXPRESSION_ATTR, rec);
            Object detailValue = getDetailValue(secondaryFieldNames, useSecondaryFieldsExpression
                    ,SECONDARY_FIELDS_EXPRESSION_ATTR, rec);
            Object cellValue = getValue(cellValueFieldNames, useCellValueFieldsExpression
                    , CELLVALUE_FIELDS_EXPRESSION_ATTR, rec);
            state.addValueToRow(masterValue, detailValue, cellValue);
        }
        finally
        {
            bindingSupport.reset();
        }
    }

    private Object getValue(
            String[] fieldNames, boolean useExpression, String exprAttr, Record rec)
        throws RecordException
    {
        Object value = null;
        if (useExpression)
            value = getNodeAttribute(exprAttr).getRealValue();
        else
        {
            if (fieldNames!=null)
            {
                if (fieldNames.length==1)
                    value = rec.getValue(fieldNames[0]);
                else
                {
                    String v = converter.convert(
                            String.class, rec.getValue(fieldNames[0]), null);
                    StringBuilder buf = new StringBuilder(v);
                    for (int i=1; i<fieldNames.length; ++i)
                    {
                        v = converter.convert(String.class, rec.getValue(fieldNames[i]), null);
                        buf.append(", "+v);
                    }
                    value = buf.toString();
                }
            }
        }
        return value;
    }

    private Object getDetailValue(
            String[] fieldNames, boolean useExpression, String exprAttr, Record rec)
        throws RecordException
    {
        Object value = null;
        if (useExpression)
            value = getNodeAttribute(exprAttr).getRealValue();
        else
        {
            if (fieldNames!=null)
            {
                if (fieldNames.length==1)
                    value = rec.getValue(fieldNames[0]);
                else
                {
                    Object[] values = new Object[fieldNames.length];
                    for (int i=0; i<fieldNames.length; ++i)
                        values[i] = rec.getValue(fieldNames[i]);
                    value = values;
                }
            }
        }
        return value;
    }

    private State getOrCreateState(RecordSchema schema, DataContext context)
    {
        State state = states.get();
        if (state==null)
        {
            state = new State(schema, context);
            states.set(state);
        }
        return state;
    }

    private void formAndSendCrossTable() throws Exception
    {
        try
        {
            State state = states.get();
            List row = state.getRow(0);
            String[] colNames = new String[row.size()];
            for (int i=0; i<row.size(); ++i)
                colNames[i] = converter.convert(String.class, row.get(i), null);
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Column names: "+printArray(colNames));
            TableImpl table = new TableImpl(colNames);
            table.setTitle(converter.convert(String.class, state.getTableValue(), null));
            for (int i=1; i<state.getRowCount(); ++i)
            {
                Object[] objRow = state.getRow(i).toArray();
                try{
                    table.addRow(objRow);
                }catch(Exception e){
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug(String.format(
                                "Error adding row number (%s): %s", i, printArray(objRow)));
                    throw e;
                }
            }

            sendDataToConsumers(table, state.dataContext);
        }
        finally
        {
            states.remove();
        }
    }
    
    private String printArray(Object[] arr)
    {
        StringBuilder buf = new StringBuilder();
        for (Object elem: arr)
            buf.append((buf.length()>0? ", " : "")+(elem==null? "" : elem.toString()));
        
        return buf.toString();
    }

    private class State
    {
        private int maxRowSize;
        private final Map<Object, Integer> indexes = new HashMap<Object, Integer>();
        private final List<List> rows = new ArrayList<List>(512);
        private final DataContext dataContext;
        private Object lastMasterValue = null;
        private Object tableValue;

        public State(RecordSchema schema, DataContext context)
        {
            this.dataContext = context;
            List firstRow = new ArrayList();
            rows.add(firstRow);
            if (useSecondaryFieldsExpression)
                firstRow.add(firstColumnName);
            else {
                Map<String, RecordSchemaField> fields = RavenUtils.getRecordSchemaFields(schema);
                for (String fieldName: secondaryFieldNames)
                    firstRow.add(fields.get(fieldName).getDisplayName());
            }
        }

        public Object getTableValue()
        {
            return tableValue;
        }

        public void setTableValue(Object tableValue)
        {
            this.tableValue = tableValue;
        }

        public int getMaxRowSize()
        {
            return maxRowSize;
        }

        public void setMaxRowSize(int maxRowSize)
        {
            this.maxRowSize = maxRowSize;
        }

        public void addValueToRow(Object masterValue, Object detailValue, Object cellValue)
        {
            if (!ObjectUtils.equals(masterValue, lastMasterValue))
            {
                rows.get(0).add(masterValue);
                lastMasterValue = masterValue;
                maxRowSize = rows.get(0).size();
            }
            Object detailValueKey = getValueKey(detailValue);
            Integer index = indexes.get(detailValueKey);
            List row = index==null? null : rows.get(index);
            if (index==null)
            {
                row = new ArrayList();
                rows.add(row);
                indexes.put(detailValueKey, rows.size()-1);
                if (detailValue instanceof Object[])
                    for (Object obj: (Object[])detailValue)
                        row.add(obj);
                else
                    row.add(detailValue);
            }
            for (int i=0; maxRowSize>row.size()+1; ++i)
                row.add(null);
            row.add(cellValue);
        }

        private Object getValueKey(Object value)
        {
            if (value instanceof Object[])
            {
                Object[] arr = (Object[]) value;
                StringBuilder builder = new StringBuilder();
                for (Object obj: arr)
                    builder.append(";"+obj);
                return builder.toString();
            }
            else
                return value;
        }

        public int getRowCount()
        {
            return rows.size();
        }

        public List getRow(int index)
        {
            List row = rows.get(index);
            for (int i=0; maxRowSize>row.size(); ++i)
                row.add(null);
            return row;
        }
    }
}

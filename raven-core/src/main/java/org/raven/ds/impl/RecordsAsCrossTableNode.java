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
import org.apache.commons.collections.CollectionUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
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
    public static final String CELLVALUE_FIELDS_EXPRESSION_ATTR = "cellValueFieldsExpression";
    public static final String MASTER_FIELDS_EXPRESSION_ATTR = "masterFieldsExpression";
    public static final String SECONDARY_FIELDS_EXPRESSION_ATTR = "secondaryFieldsExpression";
    
    @NotNull @Parameter
    private String masterFields;
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String masterFieldsExpression;    
    @NotNull @Parameter(defaultValue="false")
    private Boolean useMasterFieldsExpression;

    @NotNull @Parameter
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
        masterFieldNames = masterFields.split("\\s*,\\s*");
        secondaryFieldNames = secondaryFields.split("\\s*,\\s*");
        cellValueFieldNames = cellValueFields.split("\\s*,\\s*");
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
    protected void doSetData(DataSource dataSource, Object data) throws Exception
    {
        if (!Status.STARTED.equals(getStatus()))
            return;

        if (data==null && states.get()!=null)
        {
            try
            {
                formAndSendCrossTable();
                return;
            }
            finally
            {
                states.remove();
            }
        }

        if (!(data instanceof Record))
            return;

        Record rec = (Record) data;
        bindingSupport.put("record", rec);
        try
        {
            State state = getOrCreateState();
            Object masterValue = getValue(masterFieldNames, useMasterFieldsExpression
                    , MASTER_FIELDS_EXPRESSION_ATTR, rec);
            Object detailValue = getValue(secondaryFieldNames, useSecondaryFieldsExpression
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
        return value;
    }

    private State getOrCreateState()
    {
        State state = states.get();
        if (state==null)
        {
            state = new State();
            states.set(state);
        }
        return state;
    }

    private void formAndSendCrossTable() throws Exception
    {
        State state = states.get();
        List row = state.getRow(0);
        String[] colNames = new String[row.size()];
        for (int i=0; i<row.size(); ++i)
            colNames[i] = converter.convert(String.class, row.get(i), null);
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Column names: "+printArray(colNames));
        TableImpl table = new TableImpl(colNames);
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

        sendDataToConsumers(table);
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
        private Object lastMasterValue = null;

        public State()
        {
            rows.add(new ArrayList());
            rows.get(0).add(firstColumnName);
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
            Integer index = indexes.get(detailValue);
            List row = index==null? null : rows.get(index);
            if (index==null)
            {
                row = new ArrayList();
                rows.add(row);
                indexes.put(detailValue, rows.size()-1);
                row.add(detailValue);
            }
            if (row.size()<maxRowSize-1)
                for (int i=0; i<maxRowSize-row.size()-1; ++i)
                    row.add(null);
            row.add(cellValue);
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

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class RecordsAsCrossTableNode extends AbstractSafeDataPipe
{
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
    private String cellValueField;
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String callValueExpression;
    @NotNull @Parameter(defaultValue="false")
    private Boolean useCellValueFieldExpression;

    private String[] masterFieldNames;
    private String[] secondaryFieldNames;

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        masterFieldNames = masterFields.split("\\s*,\\s*");
        secondaryFieldNames = secondaryFields.split("\\s*,\\s*");
    }

    public String getCallValueExpression()
    {
        return callValueExpression;
    }

    public void setCallValueExpression(String callValueExpression)
    {
        this.callValueExpression = callValueExpression;
    }

    public String getCellValueField()
    {
        return cellValueField;
    }

    public void setCellValueField(String cellValueField)
    {
        this.cellValueField = cellValueField;
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

    public Boolean getUseCellValueFieldExpression()
    {
        return useCellValueFieldExpression;
    }

    public void setUseCellValueFieldExpression(Boolean useCellValueFieldExpression)
    {
        this.useCellValueFieldExpression = useCellValueFieldExpression;
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
                value = v.toString();
            }
        }
        return value;
    }

    private static class State
    {
        private int maxRowSize;
        private final Map<Object, Integer> indexes = new HashMap<Object, Integer>();
        private final List<List> rows = new ArrayList<List>(512);

        public int getMaxRowSize()
        {
            return maxRowSize;
        }

        public void setMaxRowSize(int maxRowSize)
        {
            this.maxRowSize = maxRowSize;
        }

        public void addValueToRow(Object detailValue, Object cellValue)
        {
            Integer index = indexes.get(detailValue);
            List row = null;
            if (index==null)
            {
                row = new ArrayList();
                rows.add(row);
                indexes.put(detailValue, rows.size()-1);
            }
            if (row.size()<maxRowSize-1)
                for (int i=0; i<maxRowSize-row.size()-1; ++i)
                    row.add(null);
            row.add(cellValue);
            maxRowSize = Math.max(maxRowSize, row.size());
        }

        public int getRowCount()
        {
            return rows.size();
        }

        public List getRow(int index)
        {
            List row = rows.get(index);
            if (row.size()<maxRowSize)
                for (int i=0; i<maxRowSize-row.size(); ++i)
                    row.add(null);
            return row;
        }
    }
}

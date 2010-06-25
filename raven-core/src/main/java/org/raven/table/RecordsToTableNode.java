/*
 *  Copyright 2010 Mikhail Titov.
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

import java.util.Map;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class RecordsToTableNode extends AbstractSafeDataPipe
{
    @NotNull @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchema recordSchema;
    
    @Parameter
    private String fieldsOrder;

    private ThreadLocal<TableImpl> tableHolder;
    private String[] columnNames;
    private String[] fieldNames;

    @Override
    protected void initFields() {
        super.initFields();
        tableHolder = new ThreadLocal<TableImpl>();
    }

    public RecordSchema getRecordSchema() {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchema recordSchema) {
        this.recordSchema = recordSchema;
    }

    public String getFieldsOrder() {
        return fieldsOrder;
    }

    public void setFieldsOrder(String fieldsOrder) {
        this.fieldsOrder = fieldsOrder;
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        fieldNames = null;
        columnNames = null;
        String _columnsOrder = fieldsOrder;
        if (_columnsOrder==null || _columnsOrder.trim().isEmpty()){
            RecordSchemaField[] fields = recordSchema.getFields();
            fieldNames = new String[fields.length];
            columnNames = new String[fields.length];
            for (int i=0; i<fieldNames.length; ++i){
                fieldNames[i] = fields[i].getName();
                columnNames[i] = fields[i].getDisplayName();
            }
        }else{
            fieldNames = _columnsOrder.split("\\s*,\\s*");
            Map<String, RecordSchemaField> fields = RavenUtils.getRecordSchemaFields(recordSchema);
            columnNames = new String[fieldNames.length];
            for (int i=0; i<fieldNames.length; ++i){
                RecordSchemaField field = fields.get(fieldNames[i]);
                if (field==null)
                    throw new Exception(String.format(
                            "Record schema (%s) does not contains field (%s)"
                            , recordSchema.getName(), fieldNames[i]));
                else
                    columnNames[i] = field.getDisplayName();
            }
                
        }
    }

    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        TableImpl table = tableHolder.get();
        if (data==null){
            sendDataToConsumers(table, context);
            tableHolder.remove();
            return;
        }

        RecordSchema _recordSchema = recordSchema;

        if (!checkData(data, dataSource, _recordSchema))
            return;

        Record rec = (Record) data;
        if (table==null){
            table = new TableImpl(columnNames);
            tableHolder.set(table);
        }

        Object[] row = new Object[fieldNames.length];
        for (int i=0; i<fieldNames.length; ++i)
            row[i]=rec.getValue(fieldNames[i]);
        table.addRow(row);
    }

    private boolean checkData(Object data, DataSource dataSource, RecordSchema _recordSchema)
    {
        if (!(data instanceof Record)) {
            if (isLogLevelEnabled(LogLevel.DEBUG)) {
                getLogger().debug("Data recieved from the {} is not a record", dataSource.getPath());
            }
            return false;
        }
        Record rec = (Record) data;
        if (!_recordSchema.equals(rec.getSchema())) {
            if (isLogLevelEnabled(LogLevel.DEBUG)) {
                getLogger().debug("Invalid record schema ({}) for record recieved from ({})"
                        , rec.getSchema().getName());
            }
            return false;
        }
        return true;
    }

}

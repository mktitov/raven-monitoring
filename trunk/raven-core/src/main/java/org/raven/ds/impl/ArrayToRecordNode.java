/*
 * Copyright 2015 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.ds.impl;

import java.util.Map;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class ArrayToRecordNode extends AbstractSafeDataPipe {
    
    @NotNull @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode recordSchema;
    
    @NotNull @Parameter(defaultValue = "true")
    private Boolean validateRecord;

    @Parameter
    private String csvExtensionName;

    public RecordSchemaNode getRecordSchema() {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema) {
        this.recordSchema = recordSchema;
    }

    public Boolean getValidateRecord() {
        return validateRecord;
    }

    public void setValidateRecord(Boolean validateRecord) {
        this.validateRecord = validateRecord;
    }

    public String getCsvExtensionName() {
        return csvExtensionName;
    }

    public void setCsvExtensionName(String csvExtensionName) {
        this.csvExtensionName = csvExtensionName;
    }
    

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        if (data==null) {
            sendDataToConsumers(null, context);
        } else {
            Object[] arr = converter.convert(Object[].class, data, null);
            RecordSchema _recordSchema = _getRecordSchema(context, data, dataSource);
            Map<String, CsvRecordReaderNode.FieldInfo> fieldsColumns = CsvRecordReaderNode.getFieldsColumns(
                    _recordSchema, csvExtensionName, dataSource, context, tree, bindingSupport);
            if (fieldsColumns==null) {
                if (isLogLevelEnabled(LogLevel.WARN))
                    debug(String.format(
                            "CsvRecordFieldExtension was not defined for fields in the record schema (%s)"
                            , _recordSchema.getName()));
                return;
            }
            bindingSupport.enableScriptExecution();
            try {
                Record record = _recordSchema.createRecord();
                for (Map.Entry<String, CsvRecordReaderNode.FieldInfo> entry: fieldsColumns.entrySet()) {
                    int colNum = entry.getValue().getColumnNumber()-1;
                    if (colNum<arr.length) {
                        Object value = entry.getValue().decode(arr[entry.getValue().getColumnNumber()-1]);
                        record.setValue(entry.getKey(), value);
                    }
                }
                if (!validateRecord || record.validate(this, context)) 
                    sendDataToConsumers(record, context);
                else sendError(data, context);
            } finally {
                bindingSupport.reset();
            }
            
        }
            
    }
    private RecordSchema _getRecordSchema(DataContext context, Object data, DataSource dataSource) {
        bindingSupport.put(BindingNames.DATA_CONTEXT_BINDING, context);
        bindingSupport.put(BindingNames.DATA_BINDING, data);
        bindingSupport.put(BindingNames.DATASOURCE_BINDING, dataSource);
        try {
            return recordSchema;
        } finally {
            bindingSupport.reset();
        }
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport) {
    }
    
}

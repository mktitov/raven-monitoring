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

package org.raven.ds.impl;

import java.util.Map;
import org.apache.commons.lang.text.StrTokenizer;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.impl.CsvRecordReaderNode.FieldInfo;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CsvLineToRecordNode extends AbstractSafeDataPipe
{
    @NotNull @Parameter(defaultValue=",")
    private String delimiter;

    @NotNull @Parameter(defaultValue="\"")
    private String quoteChar;

    @NotNull @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode recordSchema;

    @Parameter
    private String csvExtensionName;

    public String getCsvExtensionName() {
        return csvExtensionName;
    }

    public void setCsvExtensionName(String csvExtensionName) {
        this.csvExtensionName = csvExtensionName;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getQuoteChar() {
        return quoteChar;
    }

    public void setQuoteChar(String quoteChar) {
        this.quoteChar = quoteChar;
    }

    public RecordSchemaNode getRecordSchema() {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema) {
        this.recordSchema = recordSchema;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        if (data!=null) {
            RecordSchema _recordSchema = _getRecordSchema(context, data, dataSource);
            Map<String, FieldInfo> fieldsColumns = CsvRecordReaderNode.getFieldsColumns(
                    _recordSchema, csvExtensionName, dataSource, context, tree, bindingSupport);
            if (fieldsColumns==null) {
                if (isLogLevelEnabled(LogLevel.WARN))
                    debug(String.format(
                            "CsvRecordFieldExtension was not defined for fields in the record schema (%s)"
                            , _recordSchema.getName()));
                return;
            }
            bindingSupport.enableScriptExecution();
//            bindingSupport.put(BindingNames.RECORD_SCHEMA_BINDING, recordSchema);
//            bindingSupport.put(BindingNames.DATA_CONTEXT_BINDING, context);
            try {
                String line = converter.convert(String.class, data, null);
                if (line!=null)
                {
                    StrTokenizer tokenizer = new StrTokenizer();
                    tokenizer.setDelimiterString(delimiter);
                    tokenizer.setQuoteChar(quoteChar.charAt(0));
                    tokenizer.setEmptyTokenAsNull(true);
                    tokenizer.setIgnoreEmptyTokens(false);
                    tokenizer.reset(line);
                    String[] tokens = tokenizer.getTokenArray();
                    Record record = _recordSchema.createRecord();
                    for (Map.Entry<String, FieldInfo> entry: fieldsColumns.entrySet()) {
                        int colNum = entry.getValue().getColumnNumber()-1;
                        if (colNum<tokens.length) {
                            Object value = entry.getValue().decode(tokens[entry.getValue().getColumnNumber()-1]);
                            record.setValue(entry.getKey(), value);
                        }
                    }
                    if (record.validate(this, context)) sendDataToConsumers(record, context);
                    else sendError(data, context);
                }
            } finally {
                bindingSupport.reset();
            }
        } else 
            sendDataToConsumers(null, context);
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
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }
}

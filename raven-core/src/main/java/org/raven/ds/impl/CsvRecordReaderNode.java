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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.text.StrTokenizer;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CsvRecordReaderNode extends AbstractSafeDataPipe
{
    public final static String LINEFILTER_ATTRIBUTE = "lineFilter";

    public final static String LINE_BINDING = "line";
    public final static String LINENUMBER_BINDING = "linenumber";

    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    @NotNull
    private RecordSchemaNode recordSchema;

    @Parameter
    private String cvsExtensionName;

    @Parameter()
    private Charset dataEncoding;
    
    @Parameter(defaultValue=",") @NotNull
    private String delimiter;

    @Parameter(defaultValue="\"") @NotNull
    private String quoteChar;

    @Parameter(defaultValue="true")
    private Boolean lineFilter;

    private AtomicLong validRecords;
    @Parameter(readOnly=true)
    private AtomicLong errorRecords;

    private AtomicLong processingTime;
    
    @Override
    protected void initFields() {
        super.initFields();
        validRecords = new AtomicLong();
        errorRecords = new AtomicLong();
        processingTime = new AtomicLong();
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes) {
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport) {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context)  {
        long start = System.currentTimeMillis();
        if (data==null) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Recieved null data from node (%s)", dataSource.getPath()));
            return;
        }
        Map<String, FieldInfo> fieldsColumns = getFieldsColumns(recordSchema, cvsExtensionName);
        if (fieldsColumns==null) {
            if (isLogLevelEnabled(LogLevel.WARN))
                debug(String.format(
                        "CsvRecordFieldExtension was not defined for fields in the record schema (%s)"
                        , recordSchema.getName()));
            return;
        }
        InputStream dataStream = converter.convert(InputStream.class, data, null);
        Charset _charset = dataEncoding;
        RecordSchemaNode _recordSchema = recordSchema;
        boolean _stopOnError = getStopProcessingOnError();
        DataConsumer _errorConsumer = getErrorConsumer();
        bindingSupport.enableScriptExecution();
        try {
            try {
                LineIterator it = IOUtils.lineIterator(dataStream, _charset==null? null : _charset.name());
                if (isLogLevelEnabled(LogLevel.TRACE))
                    trace(String.format("Lines recieved from the node (%s)", dataSource.getPath()));
                int linenum=1;
                StrTokenizer tokenizer = new StrTokenizer();
                tokenizer.setDelimiterString(delimiter);
                tokenizer.setQuoteChar(quoteChar.charAt(0));
                tokenizer.setEmptyTokenAsNull(true);
                tokenizer.setIgnoreEmptyTokens(false);
                while (it.hasNext()) 
                    if (!processLine(it.nextLine(), linenum++, tokenizer, fieldsColumns, context, _recordSchema, _stopOnError, _errorConsumer))
                        break;
                sendDataAndError(null, context, _stopOnError, _errorConsumer);
            } catch(Exception e) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error(String.format("Error reading data from node (%s).", dataSource.getPath()), e);
            }
        } finally {
            bindingSupport.reset();
            processingTime.addAndGet(System.currentTimeMillis() - start);
        }
    }

    @Parameter(readOnly=true)
    public double getRecordsPerSecond() {
        double count = validRecords.get()+errorRecords.get();
        return count==0.? 0. : processingTime.get()/count*1000;
    }

    @Parameter(readOnly=true)
    public long getErrorRecords() {
        return errorRecords.get();
    }

    @Parameter(readOnly=true)
    public long getValidRecords() {
        return validRecords.get();
    }

    public RecordSchemaNode getRecordSchema() {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema) {
        this.recordSchema = recordSchema;
    }

    public String getCvsExtensionName() {
        return cvsExtensionName;
    }

    public void setCvsExtensionName(String cvsExtensionName) {
        this.cvsExtensionName = cvsExtensionName;
    }

    public Charset getDataEncoding() {
        return dataEncoding;
    }

    public void setDataEncoding(Charset dataEncoding) {
        this.dataEncoding = dataEncoding;
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

    public Boolean getLineFilter() {
        return lineFilter;
    }

    public void setLineFilter(Boolean lineFilter) {
        this.lineFilter = lineFilter;
    }

    static Map<String, FieldInfo> getFieldsColumns(RecordSchema recordSchema, String csvExtensionName) {
        RecordSchemaField[] fields = recordSchema.getFields();
        if (fields==null)
            return null;
        Map<String, FieldInfo> result = new HashMap<String, FieldInfo>();
        for (RecordSchemaField field: fields) {
            CsvRecordFieldExtension extension =
                    field.getFieldExtension(CsvRecordFieldExtension.class, csvExtensionName);
            if (extension!=null)
                result.put(field.getName(), new FieldInfo(extension));
        }
        return result;
    }

    private boolean processLine(String line, int linenum, StrTokenizer tokenizer, 
            Map<String, FieldInfo> fieldsColumns, DataContext context, RecordSchema schema,
            boolean stopProcessingOnError, DataConsumer errorConsumer) 
    {
        if (isLogLevelEnabled(LogLevel.TRACE))
            trace(line);
        bindingSupport.put(LINE_BINDING, line);
        bindingSupport.put(LINENUMBER_BINDING, linenum);
        if (!lineFilter) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Skiping line (%s). FILTERED", line));
        } else {
            try {
                tokenizer.reset(line);
                String[] tokens = tokenizer.getTokenArray();
                Record record = schema.createRecord();
                for (Map.Entry<String, FieldInfo> entry: fieldsColumns.entrySet()) {
                    int colNum = entry.getValue().getColumnNumber()-1;
                    if (colNum<tokens.length) {
                        Object value = entry.getValue().prepareValue(
                                tokens[entry.getValue().getColumnNumber()-1]);
                        record.setValue(entry.getKey(), value);
                    }
                }
                validRecords.incrementAndGet();
                return sendDataAndError(record, context, stopProcessingOnError, errorConsumer);
            } catch(Throwable e) {
                context.addError(this, e);
                sendError(line, context, errorConsumer, stopProcessingOnError);
                errorRecords.incrementAndGet();
                error("Error creating or sending record to consumers. ", e);
                error(line);
                return !stopProcessingOnError;
            }
        }
        return true;
    }

    static class FieldInfo {
        private final int columnNumber;
        private final CsvRecordFieldExtension extension;

        public FieldInfo(CsvRecordFieldExtension extension) {
            this.extension = extension;
            this.columnNumber = extension.getColumnNumber();
        }

        public int getColumnNumber() {
            return columnNumber;
        }

        public Object prepareValue(Object value) {
            return extension.prepareValue(value, null);
        }
    }
}

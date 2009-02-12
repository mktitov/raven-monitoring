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
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.text.StrTokenizer;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaField;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CvsRecordReaderNode extends AbstractDataPipe
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

    @Parameter(readOnly=true)
    private long validRecords;
    @Parameter(readOnly=true)
    private long errorRecords;

    private long processingTime;

    private Bindings localBindings;

    @Override
    protected void initFields()
    {
        super.initFields();

        validRecords = 0;
        errorRecords = 0;
        processingTime = 0;
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data)
    {
        long start = System.currentTimeMillis();
        if (data==null)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Recieved null data from node (%s)", dataSource.getPath()));
            return;
        }

        Map<String, FieldInfo> fieldsColumns = getFieldsColumns();
        if (fieldsColumns==null)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "CsvRecordFieldExtension is not defind for fields in the record schema (%s)"
                        , recordSchema.getName()));
            return;
        }

        InputStream dataStream = converter.convert(InputStream.class, data, null);
        Charset _charset = dataEncoding;
        RecordSchemaNode _recordSchema = recordSchema;
        try
        {
            try
            {
                LineIterator it =
                        IOUtils.lineIterator(dataStream, _charset==null? null : _charset.name());
                if (isLogLevelEnabled(LogLevel.TRACE))
                    trace(String.format("Lines recieved from the node (%s)", dataSource.getPath()));
                localBindings = new SimpleBindings();
                int linenum=1;
                StrTokenizer tokenizer = new StrTokenizer();
                tokenizer.setDelimiterString(delimiter);
                tokenizer.setQuoteChar(quoteChar.charAt(0));
                tokenizer.setEmptyTokenAsNull(true);
                tokenizer.setIgnoreEmptyTokens(false);

                while (it.hasNext())
                {
                    String line = it.nextLine();
                    if (isLogLevelEnabled(LogLevel.TRACE))
                        trace(line);

                    localBindings.put(LINE_BINDING, line);
                    localBindings.put(LINENUMBER_BINDING, linenum);
                    if (!lineFilter)
                    {
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            debug(String.format("Skiping line (%s). FILTERED", line));
                    }
                    else
                    {
                        try
                        {
                            tokenizer.reset(line);
                            String[] tokens = tokenizer.getTokenArray();
                            Record record = recordSchema.createRecord();
                            for (Map.Entry<String, FieldInfo> entry: fieldsColumns.entrySet())
                            {
                                Object value = entry.getValue().prepareValue(
                                        tokens[entry.getValue().getColumnNumber()-1]);
                                record.setValue(entry.getKey(), value);
                            }
                            sendDataToConsumers(record);
                            ++validRecords;
                        }catch(Throwable e)
                        {
                            ++errorRecords;
                            error("Error creating or sending record to consumers. ", e);
                            error(line);
                        }
                    }
                    linenum++;
                }
            }
            catch(Exception e)
            {
                error(String.format(
                        "Error reading data from node (%s). Error message: %s"
                        , dataSource.getPath(), e.getMessage()));
            }
        }finally
        {
            localBindings = null;
            processingTime += System.currentTimeMillis() - start;
        }
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        if (localBindings!=null)
            bindings.putAll(localBindings);
        super.formExpressionBindings(bindings);
    }

    @Parameter(readOnly=true)
    public double getRecordsPerSecond()
    {
        double count = validRecords+errorRecords;
        return count==0.? 0. : processingTime/count*1000;
    }

    public long getErrorRecords()
    {
        return errorRecords;
    }

    public long getValidRecords()
    {
        return validRecords;
    }

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    public String getCvsExtensionName()
    {
        return cvsExtensionName;
    }

    public void setCvsExtensionName(String cvsExtensionName)
    {
        this.cvsExtensionName = cvsExtensionName;
    }

    public Charset getDataEncoding()
    {
        return dataEncoding;
    }

    public void setDataEncoding(Charset dataEncoding)
    {
        this.dataEncoding = dataEncoding;
    }

    public String getDelimiter()
    {
        return delimiter;
    }

    public void setDelimiter(String delimiter)
    {
        this.delimiter = delimiter;
    }

    public String getQuoteChar()
    {
        return quoteChar;
    }

    public void setQuoteChar(String quoteChar)
    {
        this.quoteChar = quoteChar;
    }

    public Boolean getLineFilter()
    {
        return lineFilter;
    }

    public void setLineFilter(Boolean lineFilter)
    {
        this.lineFilter = lineFilter;
    }

    private Map<String, FieldInfo> getFieldsColumns()
    {
        RecordSchemaField[] fields = recordSchema.getFields();
        if (fields==null)
            return null;

        Map<String, FieldInfo> result = new HashMap<String, FieldInfo>();
        for (RecordSchemaField field: fields)
        {
            CvsRecordFieldExtension extension =
                    field.getFieldExtension(CvsRecordFieldExtension.class, cvsExtensionName);
            if (extension!=null)
                result.put(field.getName(), new FieldInfo(extension));
        }
        return result;
    }

    private class FieldInfo
    {
        private final int columnNumber;
        private final CvsRecordFieldExtension extension;

        public FieldInfo(CvsRecordFieldExtension extension)
        {
            this.extension = extension;
            this.columnNumber = extension.getColumnNumber();
        }

        public int getColumnNumber()
        {
            return columnNumber;
        }

        public Object prepareValue(Object value)
        {
            return extension.prepareValue(value);
        }
    }
}

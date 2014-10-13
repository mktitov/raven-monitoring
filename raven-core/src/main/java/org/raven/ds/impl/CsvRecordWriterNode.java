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
import java.util.Collection;
import java.util.Map;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaField;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass()
public class CsvRecordWriterNode extends AbstractSafeDataPipe
{
    public static final int INITIAL_RECORDS_BUFFER_SIZE = 128;

    @NotNull @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode recordSchema;

    @NotNull @Parameter(defaultValue=",")
    private String fieldSeparator;

    @Parameter
    private String fieldsOrder;

//    @Parameter(defaultValue="\"")
//    private String quoteChar;

    private ThreadLocal<Collection<Record>> records;

    @Override
    protected void initFields()
    {
        super.initFields();

        records = new ThreadLocal<Collection<Record>>();
    }

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public String getFieldsOrder()
    {
        return fieldsOrder;
    }

    public void setFieldsOrder(String fieldsOrder)
    {
        this.fieldsOrder = fieldsOrder;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    public String getFieldSeparator()
    {
        return fieldSeparator;
    }

    public void setFieldSeparator(String fieldSeparator)
    {
        this.fieldSeparator = fieldSeparator;
    }

//    public String getQuoteChar()
//    {
//        return quoteChar;
//    }
//
//    public void setQuoteChar(String quoteChar)
//    {
//        this.quoteChar = quoteChar;
//    }
//
    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, DataContext context) throws Exception
    {
        try
        {
            boolean result = super.gatherDataForConsumer(dataConsumer, context);

            return result;
        }
        finally
        {
            records.remove();
        }
    }

    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        if (data==null) {
            Collection<Record> recs = records.get();
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "Recieved the end marker of the records set. " +
                        "Preparing csv string from (%d) records"
                        , recs==null? 0 : recs.size()));
            Object res = null;
            if (recs!=null && !recs.isEmpty()) {
                RecordSchemaNode _recordSchema = recordSchema;
                RecordSchemaField[] fields = null;
                String _fieldsOrder = fieldsOrder;
                if (_fieldsOrder!=null)
                {
                    String[] fieldsList = _fieldsOrder.split("\\s*,\\s*");
                    if (fieldsList!=null && fieldsList.length>0)
                    {
                        Map<String, RecordSchemaField> allFields =
                                RavenUtils.getRecordSchemaFields(_recordSchema);
                        fields = new RecordSchemaField[fieldsList.length];
                        for (int i=0; i<fields.length; ++i)
                        {
                            RecordSchemaField field = allFields.get(fieldsList[i]);
                            if (field==null)
                                throw new Exception(String.format(
                                        "Record schema (%s) does not contains field (%s)"
                                        , _recordSchema.getName(), fieldsList[i]));
                            fields[i] = field;
                        }
                    }
                }
                if (fields==null)
                    fields = _recordSchema.getFields();
                if (fields!=null && fields.length>0)
                {
                    String _fieldSeparator = fieldSeparator;
                    StringBuilder buf = new StringBuilder();
                    boolean firstCycle = true;
                    for (Record rec: recs)
                    {
                        if (!firstCycle)
                            buf.append("\n");
                        addRecordToBuffer(buf, rec, fields, _fieldSeparator);
                        if (firstCycle)
                            firstCycle = false;
                    }
                    res = buf.length()==0? null : buf.toString();
                }
            }
            sendDataToConsumers(res, context);
            records.remove();
        } else {
            if (!(data instanceof Record)) {
                String mess = String.format(
                            "Invalid data type recieved from (%s). Expected (%s) recieved (%s)"
                            , dataSource.getPath(), data.getClass().getName()
                            , Record.class.getName());
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(mess);
                context.addError(this, fieldsOrder);
//                return;
            } else {
                Record record = (Record) data;
                if (!recordSchema.equals(record.getSchema())) {
                    String mess = String.format(
                                "Invalid schema of the record recieved from (%s). " +
                                "Expected (%s) recieved (%s)"
                                , dataSource.getPath(), recordSchema.getName()
                                , record.getSchema().getName());
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug(mess);
//                    return;
                } else {
                    if (records.get()==null)
                        records.set(new ArrayList(INITIAL_RECORDS_BUFFER_SIZE));
                    records.get().add((Record)data);
                }
            }
            DataSourceHelper.executeContextCallbacks(dataSource, context, data);
        }
    }

    private void addRecordToBuffer(
            StringBuilder buf, Record rec, RecordSchemaField[] fields, String fieldSeparator)
        throws RecordException
    {
        for (int i=0; i<fields.length; ++i)
        {
            RecordSchemaField field = fields[i];
            if (i>0) buf.append(fieldSeparator);
            Object fieldValue = rec.getValue(field.getName());
            String value = converter.convert(String.class, fieldValue, field.getPattern());
            buf.append(value);
        }
    }
}

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

package org.raven.statdb.impl;

import java.util.Collection;
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
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.AbstractDataPipe;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.statdb.StatisticsRecord;
import org.raven.statdb.query.KeyValues;
import org.raven.statdb.query.QueryResult;
import org.raven.statdb.query.StatisticsValues;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class SdbQueryResultToRecordsNode extends AbstractDataPipe
{
    @NotNull @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode recordSchema;

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        if (!(data instanceof QueryResult)) {
            String mess = String.format(
                        "Invalid data type recieved from (%s). Exepected (%s) but recieved (%s)"
                        , dataSource.getPath()
                        , QueryResult.class.getName()
                        , data==null? "NULL" : data.getClass().getName());
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(mess);
            DataSourceHelper.executeContextCallbacks(this, context, data);
            return;
        }

        RecordSchema _recordSchema = recordSchema;
        Map<String, RecordSchemaField> fields = RavenUtils.getRecordSchemaFields(_recordSchema);
        QueryResult result = (QueryResult) data;
        long[] timestamps = result.getTimestamps();
        try
        {
            for (KeyValues keyValues: result.getKeyValues())
            {
                Collection<StatisticsValues> values = keyValues.getStatisticsValues();
                if (values==null || values.isEmpty())
                {
                    Record record = createRecord(fields, _recordSchema, keyValues.getKey(), -1);
                    sendDataToConsumers(record, context);
                }
                else
                {
                    for (int i=0; i<timestamps.length; ++i)
                    {
                        Record record = createRecord(
                                fields, _recordSchema, keyValues.getKey(), timestamps[i]);
                        for (StatisticsValues statValues: values)
                            if (fields.containsKey(statValues.getStatisticsName()))
                                record.setValue(
                                        statValues.getStatisticsName(), statValues.getValues()[i]);
                        sendDataToConsumers(record, context);
                    }
                }
            }
        }
        finally
        {
            sendDataToConsumers(null, context);
        }
    }

    private Record createRecord(
            Map<String, RecordSchemaField> fields, RecordSchema recordSchema, String key, long time)
        throws RecordException
    {
        Record record = recordSchema.createRecord();
        if (fields.containsKey(StatisticsRecord.KEY_FIELD_NAME))
            record.setValue(StatisticsRecord.KEY_FIELD_NAME, key);

        if (time>=0)
        {
            RecordSchemaField timeField = fields.get(StatisticsRecord.TIME_FIELD_NAME);
            if (timeField!=null)
            {
                if (timeField.getFieldType().equals(RecordSchemaFieldType.TIMESTAMP))
                    time *= 1000l;
                record.setValue(StatisticsRecord.TIME_FIELD_NAME, time);
            }
        }

        return record;
    }
}

/*
 *  Copyright 2008 Mikhail Titov.
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

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.raven.RavenUtils;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.statdb.StatisticsRecord;
import org.raven.tree.Node;
import org.weda.beans.ObjectUtils;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractStatisticsRecord implements StatisticsRecord
{
	protected final String key;
	protected final long time;
    protected final TypeConverter converter;
    protected final RecordSchema schema;
    protected final String[] keyElements;
    protected final Node owner;
    
	private final Map<String, Double> values;

	public AbstractStatisticsRecord(
            Record record, TypeConverter converter, Node owner, boolean useLocalTime)
        throws Exception
	{
        this.converter = converter;
        this.owner = owner;
        
        schema = record.getSchema();
        Map<String, RecordSchemaField> fields = RavenUtils.getRecordSchemaFields(schema);
        if (!fields.containsKey(StatisticsRecord.KEY_FIELD_NAME))
            throw new Exception(String.format(
                    "Record schema (%s) does not contains field (%s)"
                    , schema.getName(), StatisticsRecord.KEY_FIELD_NAME));

        RecordSchemaField timeField = fields.get(StatisticsRecord.TIME_FIELD_NAME);
        if (timeField==null)
            throw new Exception(String.format(
                    "Record schema (%s) does not contains field (%s)"
                    , record.getSchema().getName(), StatisticsRecord.TIME_FIELD_NAME));
        long _time = converter.convert(
                Long.class, record.getValue(StatisticsRecord.TIME_FIELD_NAME), null);
        if (timeField.getFieldType().equals(RecordSchemaFieldType.TIMESTAMP))
            _time /= 1000l;
        if (useLocalTime)
            _time += TimeZone.getDefault().getOffset(_time*1000l)/1000l;

        key = converter.convert(
                String.class, record.getValue(StatisticsRecord.KEY_FIELD_NAME), null);
        time = _time;

		if (key==null || !key.startsWith("/"))
			throw new Exception(String.format(
					"Invalid statistic record key. Key (%s) must not be null and " +
					"must start from (/)", key));

        keyElements = key.substring(1).split("/");
		if (keyElements==null || keyElements.length==0 || keyElements[0].length()==0)
			throw new Exception(String.format("Invalid statistics record key (%s)", key));

        values = new HashMap<String, Double>();
        for (RecordSchemaField field: fields.values())
        {
            if (!ObjectUtils.in(
                    field.getName(), StatisticsRecord.KEY_FIELD_NAME
                    , StatisticsRecord.TIME_FIELD_NAME)
                && isFieldValueValid(field))
            {
                Object valueObj = record.getValue(field.getName());
                Double value = converter.convert(Double.class, valueObj, null);
                if (value!=null)
                    values.put(field.getName(), value);
            }
        }
		if (values.isEmpty())
            throw new Exception(String.format("Recieved empty statistic record for key (%s)", key));
	}

    protected abstract boolean isFieldValueValid(RecordSchemaField field);

	public String getKey()
	{
		return key;
	}

	public long getTime()
	{
		return time;
	}

	public Map<String, Double> getValues()
	{
		return values;
	}

    public String[] getKeyElements()
    {
        return keyElements;
    }

}

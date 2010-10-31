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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.raven.ds.InvalidRecordFieldException;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.weda.converter.TypeConverterException;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class RecordImpl implements Record
{
    @Service
    private static TypeConverter converter;

    private final RecordSchema schema;
    private final Map<String, Object> values;
    private final Map<String, RecordSchemaField> fields;
    private Map<String, Object> tags;

    public RecordImpl(RecordSchema schema) throws RecordException
    {
        this.schema = schema;
        RecordSchemaField[] schemaFields = schema.getFields();
        if (schemaFields==null || schemaFields.length==0)
            throw new RecordException(String.format(
                    "The record schema (%s) does contains fields", schema.getName()));

        values = new HashMap<String, Object>();
        fields = new HashMap<String, RecordSchemaField>();
        for (RecordSchemaField field: schemaFields)
        {
//            values.put(field.getName(), null);
            fields.put(field.getName(), field);
        }
    }

    public RecordSchema getSchema()
    {
        return schema;
    }

    public void setValue(String fieldName, Object value) throws RecordException
    {
        RecordSchemaField field = fields.get(fieldName);
        if (field==null)
            throw new InvalidRecordFieldException(fieldName, schema.getName());
        try
        {
            value = converter.convert(field.getFieldType().getType(), value, field.getPattern());
            values.put(fieldName, value);
        }
        catch(TypeConverterException e)
        {
            throw new RecordException(String.format(
                    "Error setting value (%s) for field (%s)", value, fieldName), e);
        }
    }

    public Object getValue(String fieldName) throws RecordException
    {
        RecordSchemaField field = fields.get(fieldName);
        if (field==null)
            throw new InvalidRecordFieldException(fieldName, schema.getName());

        return values.get(fieldName);
    }

    public Object getAt(String fieldName) throws RecordException
    {
        return getValue(fieldName);
    }

    public void putAt(String fieldName, Object value) throws RecordException
    {
        setValue(fieldName, value);
    }
    
    public Map<String, Object> getValues()
    {
        return Collections.unmodifiableMap(values);
    }

    public void setValues(Map<String, Object> values) throws RecordException
    {
        if (values==null) return;

        if (values!=null && !values.isEmpty())
            for (Map.Entry<String, Object> entry: values.entrySet())
                try{
                    setValue(entry.getKey(), entry.getValue());
                }catch(InvalidRecordFieldException e){ }
    }

    public void copyFrom(Record record) throws RecordException
    {
        setValues(record.getValues());
    }

    public Object getTag(String tagName)
    {
        return tags==null? null : tags.get(tagName);
    }

    public void setTag(String tagName, Object tag)
    {
        if (tags==null)
            tags = new HashMap<String, Object>();
        tags.put(tagName, tag);
    }

    public void removeTag(String tagName)
    {
        if (tags!=null)
        {
            tags.remove(tagName);
            if (tags.size()==0)
                tags = null;
        }
    }

    public boolean containsTag(String tagName)
    {
        return tags==null? false : tags.containsKey(tagName);
    }

    public Map<String, Object> getTags()
    {
        return tags==null? Collections.EMPTY_MAP : tags;
    }
}

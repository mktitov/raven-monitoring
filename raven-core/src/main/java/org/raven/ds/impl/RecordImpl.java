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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.raven.ds.DataContext;
import org.raven.ds.InvalidRecordFieldException;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordValidationErrors;
import org.raven.tree.Node;
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
    private volatile Map<String, Object> tags;

    public RecordImpl(RecordSchema schema) throws RecordException {
        this.schema = schema;
        RecordSchemaField[] schemaFields = schema.getFields();
        if (schemaFields==null || schemaFields.length==0)
            throw new RecordException(String.format(
                    "The record schema (%s) does contains fields", schema.getName()));

        values = new ConcurrentHashMap<String, Object>();
        fields = new HashMap<String, RecordSchemaField>();
        for (RecordSchemaField field: schemaFields) 
            fields.put(field.getName(), field);
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
        try {
            if (value==null)
                values.remove(fieldName);
            else {
                value = converter.convert(field.getFieldType().getType(), value, field.getPattern());
                values.put(fieldName, value);
            }
        } catch(TypeConverterException e) {
            throw new RecordException(String.format(
                    "Error setting value (%s) for field (%s)", value, fieldName), e);
        }
    }
    
    private Object getDefFieldValue(RecordSchemaField field) {
        Object val = field.getFieldDefaultValue();
        if (val==null)
            return null;
        else 
            return converter.convert(field.getFieldType().getType(), val, field.getPattern());
    }
    
    private Object getFieldValue(Object val, RecordSchemaField field) {
        return val!=null? val : getDefFieldValue(field);
    }

    private Object getFieldValue(RecordSchemaField field) {
        final Object val = values.get(field.getName());
        return val!=null? val : getDefFieldValue(field);
    }

    public Object getValue(String fieldName) throws RecordException
    {
        RecordSchemaField field = fields.get(fieldName);
        if (field==null)
            throw new InvalidRecordFieldException(fieldName, schema.getName());
        return getFieldValue(field);
    }

    public Object getAt(String fieldName) throws RecordException
    {
        return getValue(fieldName);
    }

    public void putAt(String fieldName, Object value) throws RecordException
    {
        setValue(fieldName, value);
    }
    
    public Map<String, Object> getValues() {
        Map<String, Object> res = new HashMap<String, Object>(values);
        for (String fieldName: fields.keySet())
            if (!res.containsKey(fieldName))
                res.put(fieldName, getDefFieldValue(fields.get(fieldName)));
        return res;
    }

    public void setValues(Map<String, Object> values) throws RecordException
    {
        if (values==null) return;

        if (!values.isEmpty())
            for (Map.Entry<String, Object> entry: values.entrySet())
                try {
                    setValue(entry.getKey(), entry.getValue());
                } catch(InvalidRecordFieldException e){ }
    }
    
    public void copyFrom(Map<String, Object> values) throws RecordException {
        if (values==null) return;
        for (String field: fields.keySet())
            if (values.containsKey(field))
                setValue(field, values.get(field));
    }

    public void copyFrom(Record record) throws RecordException
    {
        setValues(record.getValues());
    }
    
    private Map<String, Object> getOrCreateTags() {
        Map<String, Object> _tags = tags;
        if (_tags!=null)
            return _tags;
        else {
            synchronized(this) {
                _tags = tags;
                if (_tags!=null)
                   return _tags;
                _tags = tags = new ConcurrentHashMap<>();
                return _tags;
            }
        }
    }

    public Object getTag(String tagName)
    {
        return tags==null? null : getOrCreateTags().get(tagName);
    }

    public void setTag(String tagName, Object tag)
    {
//        if (tags==null)
//            tags = new HashMap<String, Object>();
        if (tags==null && tag==null)
            return;
        Map<String, Object> _tags = getOrCreateTags();
        if (tag==null)
            _tags.remove(tagName);
        else
            _tags.put(tagName, tag);
    }

    public void removeTag(String tagName)
    {
        if (tags!=null) {
            getOrCreateTags().remove(tagName);
            if (tags.isEmpty())
                tags = null;
        }
    }

    public boolean containsTag(String tagName)
    {
        return tags==null? false : getOrCreateTags().containsKey(tagName);
    }

    public Map<String, Object> getTags()
    {
        return tags==null? Collections.EMPTY_MAP : getOrCreateTags();
    }
    
//    private Object getFieldValue(String field) {

    public RecordValidationErrors validate()
    {
        RecordValidationErrorsImpl errors = null;
        for (RecordSchemaField field: fields.values()) {
            Collection<String> fieldErrors = field.validate(getFieldValue(field));
            if (fieldErrors!=null){
                if (errors==null)
                    errors = new RecordValidationErrorsImpl(schema.getName());
                errors.addValidationErrors(field.getName(), fieldErrors);
            }
        }
        return errors;
    }

    public boolean validate(Node node, DataContext context) {
        RecordValidationErrors errors = validate();
        if (errors==null) return true;
        else {
            context.addError(node, new Exception(errors.toText()));
            return false;
        }
    }
    
    @Override
    public String toString()
    {
        return "Schema: "+schema.getName()+"; field values: "+getValues().toString()+(tags==null?"":"; tags: "+tags.toString());
    }

}

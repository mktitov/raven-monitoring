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
import java.util.Map;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordValidationErrors;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class DatabaseRecordReference extends AbstractDatabaseRecordReference implements Record
{
    private boolean initialized = false;
    private Record wrappedRecord;

    public DatabaseRecordReference(
            ConnectionPool connectionPool, RecordSchema recordSchema, RecordSchemaField schemaField
            , Object value, Class fieldType, TypeConverter converter)
        throws DatabaseRecordReferenceException
    {
        super(connectionPool, recordSchema, schemaField, value, fieldType, converter);
    }

    public RecordSchema getSchema()
    {
        return recordSchema;
    }

    public void setValue(String fieldName, Object value) throws RecordException
    {
        if (!initialized)
            init();
        if (wrappedRecord!=null)
            wrappedRecord.setValue(fieldName, value);
    }

    public Object getValue(String fieldName) throws RecordException
    {
        if (!initialized)
            init();
        return wrappedRecord==null? null : wrappedRecord.getValue(fieldName);
    }

    public Object getAt(String fieldName) throws RecordException
    {
        return getValue(fieldName);
    }

    public void putAt(String fieldName, Object value) throws RecordException
    {
        setValue(fieldName, value);
    }

    public Map<String, Object> getValues() throws RecordException
    {
        if (!initialized)
            init();
        return wrappedRecord==null? Collections.EMPTY_MAP : wrappedRecord.getValues();
    }

    public void copyFrom(Record record) throws RecordException
    {
        if (!initialized)
            init();
        if (wrappedRecord!=null)
            wrappedRecord.copyFrom(record);
    }

    public void setValues(Map<String, Object> values) throws RecordException
    {
        if (!initialized)
            init();
        if (wrappedRecord!=null)
            wrappedRecord.setValues(values);
    }

    private void init() throws RecordException
    {
        try
        {
            Collection<Record> recs = getRecordsCollection();
            if (recs!=null)
                wrappedRecord = recs.iterator().next();
            initialized = true;
        }
        catch (DatabaseRecordQueryException ex)
        {
            throw new RecordException(ex);
        }
    }

    public Object getTag(String tagName) throws RecordException
    {
        if (!initialized)
            init();
        return wrappedRecord==null? null : wrappedRecord.getTag(tagName);
    }

    public void setTag(String tagName, Object tag) throws RecordException
    {
        if (!initialized)
            init();
        if (wrappedRecord!=null)
            wrappedRecord.setTag(tagName, tag);
    }

    public void removeTag(String tagName) throws RecordException
    {
        if (!initialized)
            init();
        if (wrappedRecord!=null)
            wrappedRecord.removeTag(tagName);
    }

    public boolean containsTag(String tagName) throws RecordException
    {
        if (!initialized)
            init();
        return wrappedRecord==null? null : wrappedRecord.containsTag(tagName);
    }

    public Map<String, Object> getTags() throws RecordException
    {
        if (!initialized)
            init();
        return wrappedRecord==null? null : wrappedRecord.getTags();
    }

    public RecordValidationErrors validate() throws RecordException
    {
        if (!initialized)
            init();
        return wrappedRecord==null? null : wrappedRecord.validate();
    }

}

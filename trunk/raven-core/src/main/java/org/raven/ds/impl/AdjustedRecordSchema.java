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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.raven.RavenUtils;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;

/**
 *
 * @author Mikhail Titov
 */
public class AdjustedRecordSchema implements RecordSchema {
    private final String name;
    private final RecordSchema baseSchema;
    private final RecordSchemaField[] fields;
    private final Map<String, RecordSchemaField> fieldsMap;

    public AdjustedRecordSchema(String name, RecordSchema baseSchema, Collection<String> includeFields, Collection<String> excludeFields) {
        this.baseSchema = baseSchema;
        this.name = name;
        RecordSchemaField[] _baseFields = baseSchema.getFields();
        if (_baseFields==null)
            this.fields = null;
        else {
            ArrayList<RecordSchemaField> fieldsList = new ArrayList<>(_baseFields.length);
            for (RecordSchemaField field: _baseFields)
                if (includeFields!=null) {
                    if (includeFields.contains(field.getName()))
                        fieldsList.add(field);
                } else if (excludeFields!=null) {
                    if (!excludeFields.contains(field.getName()))
                        fieldsList.add(field);
                } else {
                    fieldsList.add(field);
                }
            if (fieldsList.isEmpty())
                fields = null;
            else {
                fields = new RecordSchemaField[fieldsList.size()];
                fieldsList.toArray(fields);
            }
        }
        this.fieldsMap = RavenUtils.getRecordSchemaFields(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RecordSchema adjust(String name, Collection<String> includeFields, Collection<String> excludeFields) {
        return new AdjustedRecordSchema(name, this, includeFields, excludeFields);
    }

    @Override
    public RecordSchema adjust(String name, Collection<String> includeFields) {
        return adjust(name, includeFields, null);
    }

    @Override
    public RecordSchemaField[] getFields() {
        return fields;
    }

    @Override
    public Map<String, RecordSchemaField> getFieldsMap() {
        return fieldsMap;
    }

    @Override
    public RecordSchemaField getField(String name) {
        return fieldsMap.get(name);
    }

    @Override
    public Record createRecord() throws RecordException {
        return new RecordImpl(this);
    }

    @Override
    public Record createRecord(Map<String, Object> values) throws RecordException {
        final RecordImpl record = new RecordImpl(this);
        record.setValues(values);
        return record;
    }

    @Override
    public <E> E getRecordExtension(Class<E> extensionType, String extensionName) {
        return baseSchema.getRecordExtension(extensionType, extensionName);
    }
}

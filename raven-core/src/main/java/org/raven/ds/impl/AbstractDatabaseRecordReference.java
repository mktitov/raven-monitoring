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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.impl.DatabaseRecordQuery.RecordIterator;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractDatabaseRecordReference
{
    public static final int FETCH_SIZE = 100;
    public static final int MAX_ROWS = 100;
    protected final ConnectionPool connectionPool;
    protected final TypeConverter converter;
    protected final RecordSchema recordSchema;
    protected final Object value;
    protected final Class fieldType;
    protected final String tableName;
    protected final String columnName;

    public AbstractDatabaseRecordReference(
            ConnectionPool connectionPool, RecordSchema recordSchema, RecordSchemaField schemaField,
            Object value, Class fieldType, TypeConverter converter)
        throws DatabaseRecordReferenceException
    {
        this.connectionPool = connectionPool;
        this.recordSchema = recordSchema;
        this.value = value;
        this.fieldType = fieldType;
        this.converter = converter;

        DatabaseRecordFieldExtension fieldDbExtension =
                schemaField.getFieldExtension(DatabaseRecordFieldExtension.class, null);
        try
        {
            if (fieldDbExtension==null)
                throw new Exception(String.format(
                        "The field does not have (%s) extension"
                        , DatabaseRecordFieldExtension.class.getSimpleName()));
            DatabaseRecordExtension recordDbExtension =
                        recordSchema.getRecordExtension(DatabaseRecordExtension.class, null);
            if (recordDbExtension==null)
                throw new Exception(String.format(
                        "The record schema does not have (%s) extension"
                        , DatabaseRecordExtension.class.getSimpleName()));
            this.columnName = fieldDbExtension.getColumnName();
            this.tableName = recordDbExtension.getTableName();
        }
        catch(Exception e)
        {
            throw new DatabaseRecordReferenceException(
                    String.format(
                        "Error creating reference to the record schema (%s) using field (%s)"
                        , recordSchema.getName(), schemaField.getName())
                    , e);
        }
    }

    protected Collection<Record> getRecordsCollection()
        throws DatabaseRecordQueryException
    {
        DatabaseFilterElement filter = new DatabaseFilterElement(
                columnName, null, fieldType, null, false, converter);
        filter.setValue(
                value, DatabaseFilterElement.ExpressionType.OPERATOR
                , DatabaseFilterElement.OperatorType.SIMPLE, "=");
        DatabaseRecordQuery query = new DatabaseRecordQuery(
                recordSchema, null, null, Arrays.asList(filter), null, null, connectionPool
                , MAX_ROWS, FETCH_SIZE,converter);
        RecordIterator it = query.execute();
        List<Record> recs = new ArrayList<Record>();
        while (it.hasNext())
        {
            recs.add(it.next());
        }

        return recs.isEmpty()? null : recs;
    }
}

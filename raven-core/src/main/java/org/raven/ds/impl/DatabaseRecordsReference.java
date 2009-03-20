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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.Records;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class DatabaseRecordsReference extends AbstractDatabaseRecordReference implements Records
{
    private boolean initialized = false;
    private Collection<Record> records = null;

    public DatabaseRecordsReference(
            ConnectionPool connectionPool, RecordSchema recordSchema, RecordSchemaField schemaField
            , Object value, Class fieldType, TypeConverter converter)
        throws DatabaseRecordReferenceException
    {
        super(connectionPool, recordSchema, schemaField, value, fieldType, converter);
    }

    public Collection<Record> getRecords() throws RecordException
    {
        try
        {
            if (!initialized)
            {
                records = getRecordsCollection();
                records = records==null? Collections.EMPTY_LIST : records;
                initialized = true;
            }
            return records;
        }
        catch (DatabaseRecordQueryException ex)
        {
            throw new RecordException(ex);
        }
    }
}

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.log.LogLevel;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class DatabaseRecordWriterNode extends AbstractDataConsumer
{
    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    @NotNull
    private RecordSchemaNode recordSchema;

    @Parameter
    private String databaseExtensionName;

    @Parameter(valueHandlerType=ConnectionPoolValueHandlerFactory.TYPE)
    @NotNull
    private ConnectionPool connectionPool;

    public ConnectionPool getConnectionPool()
    {
        return connectionPool;
    }

    public void setConnectionPool(ConnectionPool connectionPool)
    {
        this.connectionPool = connectionPool;
    }

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    public String getDatabaseExtensionName()
    {
        return databaseExtensionName;
    }

    public void setDatabaseExtensionName(String databaseExtensionName)
    {
        this.databaseExtensionName = databaseExtensionName;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data)
    {
        if (data==null)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "Recieved the end marker from record source (%s)", dataSource.getPath()));
            return;
        }
        if (!(data instanceof Record))
        {
            error(String.format(
                    "Invalid type (%s) recieved from data source (%s). The valid type is (%s) "
                    , data==null? "null" : data.getClass().getName(), dataSource.getPath()
                    , Record.class.getName()));
            return;
        }

        Record record = (Record) data;

        RecordSchemaNode _recordSchema = recordSchema;
        if (!_recordSchema.equals(record.getSchema()))
        {
            error(String.format(
                    "Invalid record schema recived from data source (%s). " +
                    "Recieved schema is (%s), valid schema is (%s) "
                    , dataSource.getPath(), record.getSchema().getName(), _recordSchema.getName()));
            return;
        }

        DatabaseRecordExtension recordExtension =_recordSchema.getRecordExtension(
                DatabaseRecordExtension.class, databaseExtensionName);
        if (recordExtension==null)
        {
            error(String.format(
                    "Record schema (%s) does not have DatabaseRecordExtension"
                    , _recordSchema.getName()));
            return;
        }

        String tableName = recordExtension.getTableName();

        RecordSchemaField[] fields = _recordSchema.getFields();
        if (fields==null)
        {
            warn("Schema (%s) does not contains fields");
            return;
        }

        try
        {
            processRecord(tableName, fields, record);

        }
        catch(Exception e)
        {
            error("Error writing record to database", e);
        }
    }

    private void processRecord(String tableName, RecordSchemaField[] fields, Record record)
            throws Exception
    {
        List<String> columnNames = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();
        for (RecordSchemaField field : fields)
        {
            DatabaseRecordFieldExtension extension = field.getFieldExtension(
                    DatabaseRecordFieldExtension.class, databaseExtensionName);
            if (extension != null)
            {
                columnNames.add(extension.getColumnName());
                values.add(RecordSchemaFieldType.getSqlObject(
                        field.getFieldType(), record.getValue(field.getName())));
            }
        }
        if (values.size() == 0)
            throw new Exception(
                    "Record does not contain fields that should be writen to the database");

        Connection con = connectionPool.getConnection();
        try
        {
            String query = createQuery(tableName, columnNames, values);
                
            PreparedStatement insert = con.prepareStatement(query);
            try
            {
                for (int i=1; i<=values.size(); ++i)
                    insert.setObject(i, values.get(i-1));

                insert.executeUpdate();
            }
            finally
            {
                insert.close();
            }
        }
        finally
        {
            con.close();
        }
    }

    private String createQuery(String tableName, List<String> columnNames, List<Object> values)
    {
        StringBuilder query = new StringBuilder("insert into " + tableName + " (");
        for (int i = 0; i < columnNames.size(); ++i)
        {
            if (i != 0)
                query.append(", ");
            query.append(columnNames.get(i));
        }
        query.append(") values (");
        for (int i = 0; i < values.size(); ++i)
        {
            if (i != 0)
                query.append(", ");
            query.append("?");
        }
        query.append(")");

        return query.toString();
    }
}

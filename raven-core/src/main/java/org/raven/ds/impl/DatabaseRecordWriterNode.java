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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
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

    @Parameter(defaultValue="100")
    @NotNull
    private Integer recordBufferSize;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean enableUpdates;

    @Parameter(readOnly=true)
    private long recordSetsRecieved;
    @Parameter(readOnly=true)
    private long recordsRecieved;
    @Parameter(readOnly=true)
    private long recordsSaved;

    private List<Record> recordBuffer;

    public Boolean getEnableUpdates()
    {
        return enableUpdates;
    }

    public void setEnableUpdates(Boolean enableUpdates)
    {
        this.enableUpdates = enableUpdates;
    }

    public long getRecordSetsRecieved()
    {
        return recordSetsRecieved;
    }

    public long getRecordsRecieved()
    {
        return recordsRecieved;
    }

    public long getRecordsSaved()
    {
        return recordsSaved;
    }

    public Integer getRecordBufferSize()
    {
        return recordBufferSize;
    }

    public void setRecordBufferSize(Integer recordBufferSize)
    {
        this.recordBufferSize = recordBufferSize;
    }

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
    protected void doStart() throws Exception
    {
        super.doStart();

        recordSetsRecieved = 0l;
        recordsRecieved = 0l;
        recordsSaved = 0l;
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        flushRecords();
    }

    @Override
    protected synchronized void doSetData(DataSource dataSource, Object data)
    {
        try
        {
            if (data==null)
            {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format(
                            "Recieved the end marker from record source (%s)"
                            , dataSource.getPath()));
                ++recordSetsRecieved;
                flushRecords();
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
                        , dataSource.getPath(), record.getSchema().getName()
                        , _recordSchema.getName()));
                return;
            }

            ++recordsRecieved;

            int _recordBufferSize = recordBufferSize;
            if (recordBuffer==null)
                recordBuffer = new ArrayList<Record>(_recordBufferSize);
            recordBuffer.add(record);

            if (recordBuffer.size()>=_recordBufferSize)
                flushRecords();
        }
        catch(Exception e)
        {
            error("Error writing record to database", e);
        }
    }

    private String createQuery(String tableName, List<String> columnNames)
    {
        StringBuilder query = new StringBuilder("insert into " + tableName + " (");
        for (int i = 0; i < columnNames.size(); ++i)
        {
            if (i != 0)
                query.append(", ");
            query.append(columnNames.get(i));
        }
        query.append(") values (");
        for (int i = 0; i < columnNames.size(); ++i)
        {
            if (i != 0)
                query.append(", ");
            query.append("?");
        }
        query.append(")");

        return query.toString();
    }

    private String createSelectQuery(String tableName, String idColumnName)
    {
        return "SELECT count(*) FROM "+tableName+" WHERE "+idColumnName+"=?";
    }

    private String createUpdateQuery(
            String tableName, List<String> columnNames, String idColumnName)
    {
        StringBuilder query = new StringBuilder("UPDATE "+tableName+"\nSET ");
        boolean firstColumn = true;
        for (String columnName: columnNames)
            if (!idColumnName.equals(columnName))
            {
                if (!firstColumn)
                    query.append(", ");
                query.append(columnName+"=?");
                if (firstColumn)
                    firstColumn=false;
            }
        query.append("\nWHERE "+idColumnName+"=?");

        return query.toString();
    }

    private synchronized void flushRecords() throws Exception
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Flushing records to the database");
        
        if (recordBuffer==null || recordBuffer.size()==0)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Nothing to flush. Records buffer is empty.");
            return;
        }

        RecordSchema _recordSchema = recordSchema;
        RecordSchemaField[] fields = recordSchema.getFields();

        if (fields==null || fields.length==0)
            throw new Exception(String.format(
                    "Schema (%s) does not contains fields", _recordSchema.getName()));

        List<String> columnNames = new ArrayList<String>(fields.length);
        List<RecordSchemaField> dbFields =  new ArrayList<RecordSchemaField>(fields.length);
        String idColumnName = null;
        for (RecordSchemaField field : fields)
        {
            DatabaseRecordFieldExtension extension = field.getFieldExtension(
                    DatabaseRecordFieldExtension.class, databaseExtensionName);
            if (extension != null)
            {
                columnNames.add(extension.getColumnName());
                dbFields.add(field);
                IdRecordFieldExtension idExtension =
                        field.getFieldExtension(IdRecordFieldExtension.class, null);
                if (idExtension!=null)
                    idColumnName=extension.getColumnName();
            }
        }

        if (columnNames.size()==0)
            throw new Exception(String.format(
                    "Schema (%s) does not contains fields with (%s) extenstion"
                    , _recordSchema.getName()));

        DatabaseRecordExtension recordExtension =_recordSchema.getRecordExtension(
                DatabaseRecordExtension.class, databaseExtensionName);
        if (recordExtension==null)
            throw new Exception(String.format(
                    "Record schema (%s) does not have DatabaseRecordExtension"
                    , _recordSchema.getName()));

        String tableName = recordExtension.getTableName();

        String insertQuery = createQuery(tableName, columnNames);
        String updateQuery = null;
        String selectQuery = null;
        if (idColumnName!=null && enableUpdates)
        {
            updateQuery = createUpdateQuery(tableName, columnNames, idColumnName);
            selectQuery = createSelectQuery(tableName, idColumnName);
        }

        Connection con = connectionPool.getConnection();
        con.setAutoCommit(false);
        try
        {
            PreparedStatement insert = con.prepareStatement(insertQuery);
            try
            {
                boolean batchInsert = con.getMetaData().supportsBatchUpdates();

                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format(
                            "Database driver %ssupports batch updates"
                            , batchInsert? "" : "does not "));

                for (Record record: recordBuffer)
                {
                    int i=1;
                    for (RecordSchemaField field: dbFields)
                    {
                        Object val = RecordSchemaFieldType.getSqlObject(
                                field.getFieldType(), record.getValue(field.getName()));
                        insert.setObject(i++, val);
                    }
                    if (batchInsert)
                        insert.addBatch();
                    else
                        insert.executeUpdate();
                }

                if (batchInsert)
                    insert.executeBatch();

                con.commit();

                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format(
                            "Flushed (%d) records to the database", recordBuffer.size()));
                recordsSaved+=recordBuffer.size();

                recordBuffer = null;
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

    private class SchemaMeta
    {
        private final List<RecordSchemaField> dbFields;
        private final List<String> columnNames;
        private String idColumnName;
        private final String insertQuery;
        private String updateQuery;
        private String selectQuery;
        private boolean tryUpdate = false;
        private boolean recordFound = false;

        private PreparedStatement select;
        private PreparedStatement insert;
        private PreparedStatement update;

        public SchemaMeta(RecordSchema recordSchema, boolean enableUpdates) throws Exception
        {
            RecordSchemaField[] fields = recordSchema.getFields();

            if (fields==null || fields.length==0)
                throw new Exception(String.format(
                        "Schema (%s) does not contains fields", recordSchema.getName()));

            columnNames = new ArrayList<String>(fields.length);
            dbFields =  new ArrayList<RecordSchemaField>(fields.length);
            idColumnName = null;
            for (RecordSchemaField field : fields)
            {
                DatabaseRecordFieldExtension extension = field.getFieldExtension(
                        DatabaseRecordFieldExtension.class, databaseExtensionName);
                if (extension != null)
                {
                    columnNames.add(extension.getColumnName());
                    dbFields.add(field);
                    IdRecordFieldExtension idExtension =
                            field.getFieldExtension(IdRecordFieldExtension.class, null);
                    if (idExtension!=null)
                        idColumnName=extension.getColumnName();
                }
            }

            if (columnNames.size()==0)
                throw new Exception(String.format(
                        "Schema (%s) does not contains fields with (%s) extenstion"
                        , recordSchema.getName()));

            DatabaseRecordExtension recordExtension = recordSchema.getRecordExtension(
                    DatabaseRecordExtension.class, databaseExtensionName);
            if (recordExtension==null)
                throw new Exception(String.format(
                        "Record schema (%s) does not have DatabaseRecordExtension"
                        , recordSchema.getName()));

            String tableName = recordExtension.getTableName();

            insertQuery = createQuery(tableName, columnNames);
            updateQuery = null;
            selectQuery = null;
            if (idColumnName!=null && enableUpdates)
            {
                updateQuery = createUpdateQuery(tableName, columnNames, idColumnName);
                selectQuery = createSelectQuery(tableName, idColumnName);
                tryUpdate = true;
            }
        }

        public void init(Connection connection) throws SQLException
        {
            insert = connection.prepareStatement(insertQuery);
            if (updateQuery!=null)
            {
                update = connection.prepareStatement(updateQuery);
                select = connection.prepareStatement(selectQuery);
            }
        }

        public void findRecord(Record record)
        {
            if (!tryUpdate)
                recordFound = false;
            else
            {
                
            }
        }
    }
}

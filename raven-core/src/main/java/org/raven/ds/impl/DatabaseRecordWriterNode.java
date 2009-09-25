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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
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

//            RecordSchemaNode _recordSchema = recordSchema;
//            if (!_recordSchema.equals(record.getSchema()))
//            {
//                error(String.format(
//                        "Invalid record schema recived from data source (%s). " +
//                        "Recieved schema is (%s), valid schema is (%s) "
//                        , dataSource.getPath(), record.getSchema().getName()
//                        , _recordSchema.getName()));
//                return;
//            }

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

        Map<RecordSchema, SchemaMeta> metas = new HashMap<RecordSchema, SchemaMeta>();

        Connection con = connectionPool.getConnection();
        con.setAutoCommit(false);
        try
        {
            try
            {
                boolean batchUpdate = con.getMetaData().supportsBatchUpdates();

                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format(
                            "Database driver %ssupports batch updates"
                            , batchUpdate? "" : "does not "));

                for (Record record: recordBuffer)
                {
                    SchemaMeta meta = metas.get(record.getSchema());
                    if (meta==null)
                    {
                        meta = new SchemaMeta(record.getSchema(), enableUpdates, batchUpdate);
                        meta.init(con);
                        metas.put(record.getSchema(), meta);
                    }
                    meta.updateRecord(record);
                }

                if (batchUpdate)
                {
                    for (SchemaMeta meta: metas.values())
                        meta.executeBatch();
                }

                con.commit();

                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format(
                            "Flushed (%d) records to the database", recordBuffer.size()));
                recordsSaved+=recordBuffer.size();

            }
            finally
            {
                recordBuffer = null;
                for (SchemaMeta meta: metas.values())
                    meta.close();
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
        private String idFieldName;
        private final String insertQuery;
        private String updateQuery;
        private String selectQuery;
        private boolean tryUpdate = false;
        private final boolean batchUpdate;

        private PreparedStatement select;
        private PreparedStatement insert;
        private PreparedStatement update;
        private boolean hasUpdates = false;
        private boolean hasInserts = false;

        public SchemaMeta(RecordSchema recordSchema, boolean enableUpdates, boolean batchUpdate)
                throws Exception
        {
            RecordSchemaField[] fields = recordSchema.getFields();

            if (fields==null || fields.length==0)
                throw new Exception(String.format(
                        "Schema (%s) does not contains fields", recordSchema.getName()));

            columnNames = new ArrayList<String>(fields.length);
            dbFields =  new ArrayList<RecordSchemaField>(fields.length);
            idColumnName = null;
            this.batchUpdate = batchUpdate;
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
                    {
                        idColumnName=extension.getColumnName();
                        idFieldName = field.getName();
                    }
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

        private boolean findRecord(Record record) throws RecordException, SQLException
        {
            if (tryUpdate)
            {
                Object indexVal = record.getValue(idFieldName);
                if (indexVal!=null)
                {
                    select.setObject(1, indexVal);
                    ResultSet rs = select.executeQuery();
                    try
                    {
                        rs.next();
                        int count = rs.getInt(1);
                        
                        return count>0;
                    }
                    finally
                    {
                        rs.close();
                    }
                }
            }
            return false;
        }

        public void updateRecord(Record record) throws Exception
        {
            boolean recordFound = findRecord(record);
            PreparedStatement st = recordFound? update : insert;
            int i=1;
            Object idFieldValue = null;
            for (RecordSchemaField field: dbFields)
            {
                Object val = RecordSchemaFieldType.getSqlObject(
                        field, record.getValue(field.getName()));
                if (!recordFound || !field.getName().equals(idFieldName))
                {
                    st.setObject(i++, val);
                }
                else
                    idFieldValue = val;

            }
            if (recordFound)
                st.setObject(dbFields.size(), idFieldValue);
            
            if (batchUpdate)
                st.addBatch();
            else
                st.executeUpdate();
            if (!hasInserts)
                hasInserts = !recordFound;
            if (!hasUpdates)
                hasUpdates = recordFound;
        }

        public void executeBatch() throws Exception
        {
            if (batchUpdate)
            {
                if (hasInserts)
                    insert.executeBatch();
                if (hasUpdates)
                    update.executeBatch();
            }
        }

        public void close() throws SQLException
        {
            insert.close();
            if (tryUpdate)
            {
                select.close();
                update.close();
            }
        }
    }
}

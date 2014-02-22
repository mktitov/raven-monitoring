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
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldCodec;
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

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean enableDeletes;

    @NotNull @Parameter(defaultValue="false")
    private Boolean updateIdField;

    @Parameter(readOnly=true)
    private long recordSetsRecieved;
    @Parameter(readOnly=true)
    private long recordsRecieved;
    @Parameter(readOnly=true)
    private long recordsSaved;

    private List<Record> recordBuffer;

    public Boolean getUpdateIdField()
    {
        return updateIdField;
    }

    public void setUpdateIdField(Boolean updateIdField)
    {
        this.updateIdField = updateIdField;
    }

    public Boolean getEnableUpdates()
    {
        return enableUpdates;
    }

    public void setEnableUpdates(Boolean enableUpdates)
    {
        this.enableUpdates = enableUpdates;
    }

    public Boolean getEnableDeletes()
    {
        return enableDeletes;
    }

    public void setEnableDeletes(Boolean enableDeletes)
    {
        this.enableDeletes = enableDeletes;
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
        recordBuffer = null;
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        flushRecords();
    }

    @Override
    protected synchronized void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        if (data==null) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "Recieved the end marker from record source (%s)"
                        , dataSource.getPath()));
            ++recordSetsRecieved;
            flushRecords();
            return;
        }
        if (!(data instanceof Record)) {
            error(String.format(
                    "Invalid type (%s) recieved from data source (%s). The valid type is (%s) "
                    , data==null? "null" : data.getClass().getName(), dataSource.getPath()
                    , Record.class.getName()));
            return;
        }
        Record record = (Record) data;
        ++recordsRecieved;
        int _recordBufferSize = recordBufferSize;
        if (recordBuffer==null)
            recordBuffer = new ArrayList<Record>(_recordBufferSize);
        recordBuffer.add(record);

        if (recordBuffer.size()>=_recordBufferSize)
            flushRecords();
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

    private String createDeleteQuery(String tableName, String idColumnName)
    {
        return String.format("DELETE FROM %s where %s=?", tableName, idColumnName);
    }

    private synchronized void flushRecords() throws Exception
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Flushing records to the database");
        
        if (recordBuffer==null || recordBuffer.isEmpty()) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Nothing to flush. Records buffer is empty.");
            return;
        }

        Map<RecordSchema, SchemaMeta> metas = new HashMap<RecordSchema, SchemaMeta>();

        Connection con = connectionPool.getConnection();
        con.setAutoCommit(false);
        try {
            try {
                boolean batchUpdate = !updateIdField && con.getMetaData().supportsBatchUpdates();

                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format(
                            "Database driver %ssupports batch updates"
                            , batchUpdate? "" : "does not "));

                for (Record record: recordBuffer) {
                    SchemaMeta meta = metas.get(record.getSchema());
                    if (meta==null) {
                        meta = new SchemaMeta(
                                record.getSchema(), enableUpdates, enableDeletes, batchUpdate, updateIdField);
                        meta.init(con);
                        metas.put(record.getSchema(), meta);
                    }
                    try {
                        meta.updateRecord(record);
                    } catch (Exception e) {
                        if (isLogLevelEnabled(LogLevel.WARN))
                            getLogger().warn("Record updating error: {}", record);
                        throw e;
                    }
                }

                if (batchUpdate)
                    for (SchemaMeta meta: metas.values())
                        try {
                            meta.executeBatch();
                        } catch (Exception e) {
                            if (isLogLevelEnabled(LogLevel.WARN)) {
                                getLogger().warn("Error updating set of records");
                                int i=0;
                                for (Record rec: recordBuffer)
                                    getLogger().warn("[{}] Record: {}", ++i, rec);
                            }
                            throw e;
                        }

                con.commit();

                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format(
                            "Flushed (%d) records to the database", recordBuffer.size()));
                recordsSaved+=recordBuffer.size();

            } finally {
                recordBuffer = null;
                for (SchemaMeta meta: metas.values())
                    meta.close();
            }
        } finally {
            con.close();
        }
    }
    
    private static class FieldInfo {
        private final RecordSchemaField field;
        private final RecordSchemaFieldCodec codec;

        public FieldInfo(RecordSchemaField field, DatabaseRecordFieldExtension dbExt) {
            this.field = field;
            this.codec = dbExt.getCodec();
        }
        
        public Object encode(Object val) {
            return codec==null? val : codec.encode(val, null);
        }
        
    }

    private class SchemaMeta
    {
        private final List<FieldInfo> dbFields;
        private final List<String> columnNames;
//        private final Map<String, RecordSchema> codecs;
        private String idColumnName;
        private String idFieldName;
        private String sequenceName;
        private final String insertQuery;
        private String updateQuery;
        private String selectQuery;
        private String deleteQuery;
        private boolean tryUpdate = false;
        private final boolean batchUpdate;
        private final boolean updateIdField;

        private PreparedStatement select;
        private PreparedStatement insert;
        private PreparedStatement update;
        private PreparedStatement delete;
        private PreparedStatement sequence;
        private boolean hasUpdates = false;
        private boolean hasInserts = false;
        private boolean hasDeletes = false;

        public SchemaMeta(
                RecordSchema recordSchema, boolean enableUpdates, boolean enableDeletes, boolean batchUpdate
                , boolean updateIdField)
            throws Exception
        {
            RecordSchemaField[] fields = recordSchema.getFields();

            if (fields==null || fields.length==0)
                throw new Exception(String.format(
                        "Schema (%s) does not contains fields", recordSchema.getName()));

            columnNames = new ArrayList<String>(fields.length);
            dbFields =  new ArrayList<FieldInfo>(fields.length);
            idColumnName = null;
            this.batchUpdate = batchUpdate;
            this.updateIdField = updateIdField;
            for (RecordSchemaField field : fields)
            {
                DatabaseRecordFieldExtension extension = field.getFieldExtension(
                        DatabaseRecordFieldExtension.class, databaseExtensionName);
                if (extension != null)
                {
                    columnNames.add(extension.getColumnName());
                    dbFields.add(new FieldInfo(field, extension));
                    IdRecordFieldExtension idExtension =
                            field.getFieldExtension(IdRecordFieldExtension.class, null);
                    if (idExtension!=null)
                    {
                        idColumnName=extension.getColumnName();
                        idFieldName = field.getName();
                        DatabaseSequenceRecordFieldExtension seqExt = field.getFieldExtension(
                                DatabaseSequenceRecordFieldExtension.class, null);
                        if (seqExt!=null)
                            sequenceName = seqExt.getSequenceName();
                    }
                }
            }

            if (columnNames.size()==0)
                throw new Exception(String.format(
                        "Schema (%s) does not contains fields with (%s) extenstion"
                        , recordSchema.getName(), DatabaseRecordFieldExtension.class.getName()));

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
            if (idColumnName!=null && enableDeletes)
                deleteQuery = createDeleteQuery(tableName, idColumnName);
                
        }

        public void init(Connection connection) throws SQLException
        {
            insert = connection.prepareStatement(insertQuery);
            if (updateQuery!=null)
            {
                update = connection.prepareStatement(updateQuery);
                select = connection.prepareStatement(selectQuery);
            }
            if (deleteQuery!=null)
                delete = connection.prepareStatement(deleteQuery);
            if (sequenceName!=null && updateIdField)
            {
                sequence = connection.prepareStatement("select "+sequenceName+".nextval from dual");
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

        private Object getNextSequenceValue(Record record) throws RecordException, SQLException {
            ResultSet rs = sequence.executeQuery();
            try {
                rs.next();
                Object nextVal = rs.getObject(1);
                record.setValue(idFieldName, nextVal);
                return nextVal;
            } finally {
                rs.close();
            }
        }

        public void updateRecord(Record record) throws Exception
        {
            PreparedStatement st = null;
            boolean recordFound = false;
            boolean deleteRecord = deleteQuery!=null && record.containsTag(Record.DELETE_TAG);
            try {
                if (!deleteRecord) {
                    recordFound = findRecord(record);
                    st = recordFound? update : insert;
                } else
                    st = delete;
                int i=1;
                Object idFieldValue = null;
                for (FieldInfo fi: dbFields) {
                    Object val = RecordSchemaFieldType.getSqlObject(
                            fi.field, fi.encode(record.getValue(fi.field.getName())));
                    if (   val==null && fi.field.getName().equals(idFieldName) && updateIdField
                        && sequenceName!=null && !recordFound && !deleteRecord)
                    {
                        val = getNextSequenceValue(record);
                    }
                    if ((recordFound || deleteRecord) && fi.field.getName().equals(idFieldName))
                        idFieldValue = val;
                    else if (!deleteRecord)
                        st.setObject(i++, val);
                }
                if (recordFound)
                    st.setObject(dbFields.size(), idFieldValue);
                else if (deleteRecord)
                    st.setObject(1, idFieldValue);

                if (batchUpdate)
                    st.addBatch();
                else
                {
                    st.executeUpdate();
                    if (updateIdField && idColumnName!=null && !recordFound && !deleteRecord && sequenceName==null)
                    {
                        ResultSet rs = st.getGeneratedKeys();
                        rs.next();
                        Object idVal = rs.getObject(1);
                        record.setValue(idFieldName, idVal);
                    }
                }
                if (!hasDeletes)
                    hasDeletes = deleteRecord;
                if (!hasInserts)
                    hasInserts = !recordFound && !deleteRecord;
                if (!hasUpdates)
                    hasUpdates = recordFound && !deleteRecord;
            }
            finally
            {
                if (deleteRecord)
                    record.removeTag(Record.DELETE_TAG);
            }
        }

        public void executeBatch() throws Exception
        {
            if (batchUpdate)
            {
                if (hasInserts)
                    insert.executeBatch();
                if (hasUpdates)
                    update.executeBatch();
                if (hasDeletes)
                    delete.executeBatch();
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

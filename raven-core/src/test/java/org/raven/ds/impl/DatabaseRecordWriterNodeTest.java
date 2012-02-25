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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.dbcp.impl.JDBCConnectionPoolNode;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.log.LogLevel;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.SystemNode;

/**
 *
 * @author Mikhail Titov
 */
public class DatabaseRecordWriterNodeTest extends RavenCoreTestCase
{
    private RecordSchemaNode schema;
    private PushDataSource ds;
    private JDBCConnectionPoolNode pool;
    private DatabaseRecordWriterNode writer;
    private RecordSchemaFieldNode idField;

    @Before
    public void prepare() throws Exception
    {
        Config conf = configurator.getConfig();
        assertNotNull(conf);

        ConnectionPoolsNode poolsNode =
                (ConnectionPoolsNode)
                tree.getNode(SystemNode.NAME).getChildren(ConnectionPoolsNode.NAME);
        assertNotNull(poolsNode);
        pool = new JDBCConnectionPoolNode();
        pool.setName("pool");
        poolsNode.addChildren(pool);
        pool.save();
        pool.init();

        pool.setUserName(conf.getStringProperty(Configurator.TREE_STORE_USER, null));
        pool.setPassword(conf.getStringProperty(Configurator.TREE_STORE_PASSWORD, null));
        pool.setUrl(conf.getStringProperty(Configurator.TREE_STORE_URL, null));
        pool.setDriver("org.h2.Driver");

        pool.start();

        createTable(pool, true);

        schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        schema.start();
        assertEquals(Status.STARTED, schema.getStatus());

        DatabaseRecordExtension recordExtension = new DatabaseRecordExtension();
        recordExtension.setName("db");
        schema.getRecordExtensionsNode().addAndSaveChildren(recordExtension);
        recordExtension.setTableName("record_data");
        recordExtension.start();
        assertEquals(Status.STARTED, recordExtension.getStatus());

        RecordSchemaFieldNode field1;
        DatabaseRecordFieldExtension dbExtension;
        field1 = new RecordSchemaFieldNode();
        field1.setName("field2");
        schema.addAndSaveChildren(field1);
        field1.setFieldType(RecordSchemaFieldType.IP);
        field1.start();
        assertEquals(Status.STARTED, field1.getStatus());

        dbExtension = new DatabaseRecordFieldExtension();
        dbExtension.setName("db");
        field1.addAndSaveChildren(dbExtension);
        dbExtension.setColumnName("col2");
        dbExtension.start();
        assertEquals(Status.STARTED, dbExtension.getStatus());

        field1 = new RecordSchemaFieldNode();
        idField = field1;
        field1.setName("field1");
        schema.addAndSaveChildren(field1);
        field1.setFieldType(RecordSchemaFieldType.INTEGER);
        field1.start();
        assertEquals(Status.STARTED, field1.getStatus());

        dbExtension = new DatabaseRecordFieldExtension();
        dbExtension.setName("db");
        field1.addAndSaveChildren(dbExtension);
        dbExtension.setColumnName("col1");
        dbExtension.start();
        assertEquals(Status.STARTED, dbExtension.getStatus());

        IdRecordFieldExtension idExtension = new IdRecordFieldExtension();
        idExtension.setName("id");
        field1.addAndSaveChildren(idExtension);
        assertTrue(idExtension.start());

        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.start();
        assertEquals(Status.STARTED, ds.getStatus());

        writer = new DatabaseRecordWriterNode();
        writer.setName("writer");
        tree.getRootNode().addAndSaveChildren(writer);
        writer.setConnectionPool(pool);
        writer.setDataSource(ds);
        writer.setLogLevel(LogLevel.DEBUG);
        writer.start();
        assertEquals(Status.STARTED, writer.getStatus());
    }

    @Test
    public void setData_test() throws Exception
    {
        Record record = schema.createRecord();
        record.setValue("field1", 10);
        record.setValue("field2", "10.50.1.1");
        ds.pushData(record);
        ds.pushData(null);

        Connection con = pool.getConnection();
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("select col1, col2 from record_data");
        List<Object[]> rows = new ArrayList<Object[]>();
        while (rs.next())
            rows.add(new Object[]{rs.getObject(1), rs.getObject(2)});
        rs.close();
        st.close();
        con.close();

        assertEquals(1, rows.size());
        assertEquals(10, rows.get(0)[0]);
        assertEquals("10.50.1.1", rows.get(0)[1]);
    }

    @Test
    public void flushRecordsOnStop_test() throws Exception
    {
        Record record = schema.createRecord();
        record.setValue("field1", 10);
        record.setValue("field2", "10.50.1.1");
        ds.pushData(record);
        writer.stop();

        List<Object[]> rows = getRows(pool.getConnection());
        assertEquals(1, rows.size());
        assertEquals(10, rows.get(0)[0]);
        assertEquals("10.50.1.1", rows.get(0)[1]);
    }

    @Test
    public void update_test() throws Exception
    {
        Connection connection = pool.getConnection();
        insertData(connection);
        writer.setEnableUpdates(true);

        List<Object[]> rows = getRows(pool.getConnection());
        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0)[0]);
        assertEquals("aaa", rows.get(0)[1]);

        Record record = schema.createRecord();
        record.setValue("field1", 1);
        record.setValue("field2", "10.50.1.1");
        ds.pushData(record);
        writer.stop();

        rows = getRows(pool.getConnection());
        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0)[0]);
        assertEquals("10.50.1.1", rows.get(0)[1]);
    }

    @Test
    public void updateIdField_test() throws Exception
    {
        writer.setUpdateIdField(true);
        Record record = schema.createRecord();
        record.setValue("field2", "10.50.1.1");
        ds.pushData(record);
        ds.pushData(null);

        writer.stop();
        assertEquals(new Integer(1), record.getValue("field1"));
    }

    @Test
    public void sequencedIdField_test() throws Exception
    {
        createTable(pool, false);
        Connection con = pool.getConnection();
        try{
            Statement st = con.createStatement();
            try{
                st.executeUpdate("drop sequence test_seq");
            }catch (Exception e){}
            st.executeUpdate("create sequence test_seq start with 2");
            st.close();
        }finally{
            con.close();
        }
        DatabaseSequenceRecordFieldExtension seqExt = DatabaseSequenceRecordFieldExtension.create(
                idField, "sequence", "test_seq");
        writer.setUpdateIdField(true);
        Record record = schema.createRecord();
        record.setValue("field2", "10.50.1.1");
        ds.pushData(record);
        ds.pushData(null);

        writer.stop();
        assertEquals(new Integer(2), record.getValue("field1"));
    }

    @Test
    public void delete_test() throws Exception
    {
        writer.setUpdateIdField(true);
        writer.setEnableUpdates(Boolean.TRUE);
        Record record = schema.createRecord();
        record.setValue("field2", "10.50.1.1");
        ds.pushData(record);
        ds.pushData(null);

        List<Object[]> rows = getRows(pool.getConnection());
        assertEquals(1, rows.size());

        record.setTag(Record.DELETE_TAG, null);
        ds.pushData(record);
        ds.pushData(null);
        assertEquals(1, getRows(pool.getConnection()).size());
        assertTrue(record.containsTag(Record.DELETE_TAG));

        writer.setEnableDeletes(Boolean.TRUE);
        ds.pushData(record);
        ds.pushData(null);
        assertEquals(0, getRows(pool.getConnection()).size());
        assertFalse(record.containsTag(Record.DELETE_TAG));

        writer.stop();
    }

    private List<Object[]> getRows(Connection con) throws SQLException
    {
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("select col1, col2 from record_data");
        List<Object[]> rows = new ArrayList<Object[]>();
        while (rs.next())
            rows.add(new Object[]{rs.getObject(1), rs.getObject(2)});
        rs.close();
        st.close();
        con.close();

        return rows;
    }

    private void createTable(JDBCConnectionPoolNode pool, boolean autoIncrement) throws SQLException
    {
        Connection con = pool.getConnection();
        Statement st = con.createStatement();
        st.executeUpdate("drop table if exists record_data");
        if (autoIncrement)
            st.executeUpdate("create table record_data(col1 int auto_increment(1), col2 varchar)");
        else
            st.executeUpdate("create table record_data(col1 int, col2 varchar)");
    }

    private void insertData(Connection connection) throws SQLException
    {
        Statement st = connection.createStatement();
        st.executeUpdate("insert into record_data(col1,col2) values(1, 'aaa')");
        connection.commit();
    }
}
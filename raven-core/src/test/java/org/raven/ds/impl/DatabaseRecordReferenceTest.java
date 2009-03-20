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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.dbcp.impl.JDBCConnectionPoolNode;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.tree.impl.SystemNode;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class DatabaseRecordReferenceTest extends RavenCoreTestCase
{
    private RecordSchemaNode schema;
    private RecordSchemaField recordField;
    private JDBCConnectionPoolNode pool;
    private TypeConverter converter;

    @Before
    public void prepare() throws Exception
    {
        converter = registry.getService(TypeConverter.class);
        assertNotNull(converter);

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
        assertTrue(pool.start());

        schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        DatabaseRecordExtension dbExtension = new DatabaseRecordExtension();
        dbExtension.setName("db");
        schema.getRecordExtensionsNode().addAndSaveChildren(dbExtension);
        dbExtension.setTableName("record_data");
        assertTrue(dbExtension.start());

        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
        field.setName("field1");
        schema.addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.INTEGER);
        assertTrue(field.start());
        recordField = field;

        DatabaseRecordFieldExtension fieldExtension = new DatabaseRecordFieldExtension();
        fieldExtension.setName("db");
        field.addAndSaveChildren(fieldExtension);
        fieldExtension.setColumnName("column1");
        assertTrue(fieldExtension.start());

        field = new RecordSchemaFieldNode();
        field.setName("field2");
        schema.addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.STRING);
        assertTrue(field.start());

        fieldExtension = new DatabaseRecordFieldExtension();
        fieldExtension.setName("db");
        field.addAndSaveChildren(fieldExtension);
        fieldExtension.setColumnName("column2");
        assertTrue(fieldExtension.start());

        createTable(pool);
    }

    @Test
    public void emptyRecordTest() throws DatabaseRecordReferenceException, RecordException
    {
        DatabaseRecordReference rec = new DatabaseRecordReference(
                pool, schema, recordField, 1, Integer.class, converter);
        assertSame(schema, rec.getSchema());
        assertNull(rec.getAt("field1"));
        assertNull(rec.getValue("field2"));
        assertSame(Collections.EMPTY_MAP, rec.getValues());
        rec.setValue("field1", 1);
    }

    @Test
    public void recordTest() throws Exception
    {
        insertData(pool);
        
        DatabaseRecordReference rec = new DatabaseRecordReference(
                pool, schema, recordField, 1, Integer.class, converter);
        assertSame(schema, rec.getSchema());
        assertEquals(1, rec.getAt("field1"));
        assertEquals("test", rec.getValue("field2"));
        rec.setValue("field2", "aa");
        assertEquals("aa", rec.getValue("field2"));
        Map<String, Object> vals = rec.getValues();
        assertNotNull(vals);
        assertEquals(2, vals.size());
    }

    private void createTable(JDBCConnectionPoolNode pool) throws SQLException
    {
        Connection con = pool.getConnection();
        Statement st = con.createStatement();
        st.executeUpdate("drop table if exists record_data");
        st.executeUpdate("create table record_data(column1 int, column2 varchar)");
    }

    private void insertData(JDBCConnectionPoolNode pool) throws SQLException
    {
        Connection con = pool.getConnection();
        Statement st = con.createStatement();
        st.executeUpdate("insert into record_data (column1, column2) values (1, 'test')");
        con.commit();
    }
}
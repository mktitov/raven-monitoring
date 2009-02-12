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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.dbcp.impl.JDBCConnectionPoolNode;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.tree.impl.SystemNode;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class DatabaseRecordQueryTest extends RavenCoreTestCase
{
    private RecordSchemaNode schema;
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

        DatabaseRecordFieldExtension fieldExtension = new DatabaseRecordFieldExtension();
        fieldExtension.setName("db");
        field.addAndSaveChildren(fieldExtension);
        fieldExtension.setColumnName("column1");
        assertTrue(fieldExtension.start());
        
        field = new RecordSchemaFieldNode();
        field.setName("field2");
        schema.addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.INTEGER);
        assertTrue(field.start());

        fieldExtension = new DatabaseRecordFieldExtension();
        fieldExtension.setName("db");
        field.addAndSaveChildren(fieldExtension);
        fieldExtension.setColumnName("column2");
        assertTrue(fieldExtension.start());
    }

    /*
     * whereExpression == null
     * orderExpression == null
     * queryTemplate == null
     * filterElements == null
     */
    @Test
    public void queryConstructionTest_1() throws DatabaseRecordQueryException
    {
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(schema, null, null, null, pool);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1", recordQuery.getQuery());
    }

    /*
     * whereExpression != null
     * orderExpression == null
     * queryTemplate == null
     * filterElements == null
     */
    @Test
    public void queryConstructionTest_2() throws Exception
    {
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(schema, null, null, "2=2", null, pool);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1\n   AND (2=2)"
                , recordQuery.getQuery());
    }

    /*
     * whereExpression == null
     * orderExpression != null
     * queryTemplate == null
     * filterElements == null
     */
    @Test
    public void queryConstructionTest_3() throws Exception
    {
        DatabaseRecordQuery recordQuery =
                new DatabaseRecordQuery(schema, null, null, null, "column1", pool);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1\nORDER BY column1"
                , recordQuery.getQuery());
    }

    /*
     * whereExpression == null
     * orderExpression != null
     * queryTemplate == null
     * filterElements (COMPLETE expressionType)
     */
    @Test
    public void queryConstructionTest_4() throws Exception
    {
        DatabaseFilterElement filterElement = new DatabaseFilterElement(
                "column1", Integer.class, null, converter);
        filterElement.setExpression("#is not null");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, Arrays.asList(filterElement), null, null, pool);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND (column1 is not null)"
                , recordQuery.getQuery());
    }
    
    /*
     * whereExpression == null
     * orderExpression != null
     * queryTemplate == null
     * filterElements (expressionType==OPERATOR, operationType==SIMPLE)
     */
    @Test
    public void queryConstructionTest_5() throws Exception
    {
        DatabaseFilterElement filterElement = new DatabaseFilterElement(
                "column1", Integer.class, null, converter);
        filterElement.setExpression(">10");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, Arrays.asList(filterElement), null, null, pool);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND column1>?"
                , recordQuery.getQuery());
    }
    
    /*
     * whereExpression == null
     * orderExpression != null
     * queryTemplate == null
     * filterElements (expressionType==OPERATOR, operationType==LIKE)
     */
    @Test
    public void queryConstructionTest_6() throws Exception
    {
        DatabaseFilterElement filterElement = new DatabaseFilterElement(
                "column1", Integer.class, null, converter);
        filterElement.setExpression("10%");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, Arrays.asList(filterElement), null, null, pool);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND column1 LIKE '10%'"
                , recordQuery.getQuery());
    }
    
    /*
     * whereExpression == null
     * orderExpression != null
     * queryTemplate == null
     * filterElements (expressionType==OPERATOR, operationType==BETWEEN)
     */
    @Test
    public void queryConstructionTest_7() throws Exception
    {
        DatabaseFilterElement filterElement = new DatabaseFilterElement(
                "column1", Integer.class, null, converter);
        filterElement.setExpression("[1, 2]");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, Arrays.asList(filterElement), null, null, pool);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND column1 BETWEEN ? AND ?"
                , recordQuery.getQuery());
    }
    /*
     * whereExpression == null
     * orderExpression == null
     * queryTemplate == null
     * filterElements (expressionType==OPERATOR, operationType==BETWEEN)
     */
    @Test
    public void queryConstructionTest_8() throws Exception
    {
        DatabaseFilterElement filterElement = new DatabaseFilterElement(
                "column1", Integer.class, null, converter);
        filterElement.setExpression("{1, 2}");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, Arrays.asList(filterElement), null, null, pool);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND column1 IN (?, ?)"
                , recordQuery.getQuery());
    }
    /*
     * whereExpression == null
     * orderExpression == null
     * queryTemplate != null
     * filterElements == null
     */
    @Test
    public void queryConstructionTest_9() throws Exception
    {
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, null, "select * from table", pool);
        assertEquals("select * from table", recordQuery.getQuery());
    }
    /*
     * whereExpression == null
     * orderExpression == null
     * queryTemplate != null
     * filterElements == null
     */
    @Test
    public void queryConstructionTest_10() throws Exception
    {
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, null, "select * from table where 1=1 {#}", pool);
        assertEquals("select * from table where 1=1 ", recordQuery.getQuery());
    }
    /*
     * whereExpression == null
     * orderExpression == null
     * queryTemplate != null
     * filterElements != null
     */
    @Test
    public void queryConstructionTest_11() throws Exception
    {
        DatabaseFilterElement filterElement = new DatabaseFilterElement(
                "column1", Integer.class, null, converter);
        filterElement.setExpression(">10");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                    schema, null, Arrays.asList(filterElement)
                    , "select * from table where 1=1 {#} order by column1", pool);
        assertEquals(
                "select * from table where 1=1 \n   AND column1>? order by column1"
                , recordQuery.getQuery());
    }

    @Test
    public void execute_noRecords() throws Exception
    {
        createTable(pool);
        DatabaseRecordQuery query = new DatabaseRecordQuery(schema, null, null, null, pool);

        DatabaseRecordQuery.RecordIterator it = query.execute();
        assertNotNull(it);
        assertFalse(it.hasNext());

        query.close();
    }

    @Test
    public void execute1Test() throws Exception
    {
        createTable(pool);
        insertData(pool);

        DatabaseFilterElement filterElement1 = new DatabaseFilterElement(
                "column1", Integer.class, null, converter);
        DatabaseFilterElement filterElement2 = new DatabaseFilterElement(
                "column2", Integer.class, null, converter);

        filterElement1.setExpression("[1,2]");
        filterElement2.setExpression("10");

        DatabaseRecordQuery query = new DatabaseRecordQuery(
                schema, null, Arrays.asList(filterElement1, filterElement2), null, "column1", pool);
        
        DatabaseRecordQuery.RecordIterator it = query.execute();
        assertNotNull(it);
        List<Record> recs = iteratorToList(it);
        assertEquals(2, recs.size());

        testRecord(1, 10, recs.get(0));
        testRecord(2, 10, recs.get(1));
    }

    @Test
    public void execute2Test() throws Exception
    {
        createTable(pool);
        insertData(pool);

        DatabaseFilterElement filterElement1 = new DatabaseFilterElement(
                "column1", Integer.class, null, converter);
        DatabaseFilterElement filterElement2 = new DatabaseFilterElement(
                "column2", Integer.class, null, converter);

        filterElement1.setExpression("{1,3}");
        filterElement2.setExpression("10");

        DatabaseRecordQuery query = new DatabaseRecordQuery(
                schema, null, Arrays.asList(filterElement1, filterElement2), null, "column1", pool);

        DatabaseRecordQuery.RecordIterator it = query.execute();
        assertNotNull(it);
        List<Record> recs = iteratorToList(it);
        assertEquals(2, recs.size());

        testRecord(1, 10, recs.get(0));
        testRecord(3, 10, recs.get(1));
    }

    private void createTable(JDBCConnectionPoolNode pool) throws SQLException
    {
        Connection con = pool.getConnection();
        Statement st = con.createStatement();
        st.executeUpdate("drop table if exists record_data");
        st.executeUpdate("create table record_data(column1 int, column2 int)");
    }

    private void insertData(JDBCConnectionPoolNode pool) throws SQLException
    {
        Connection con = pool.getConnection();
        Statement st = con.createStatement();
        st.executeUpdate("insert into record_data (column1, column2) values(1,10)");
        st.executeUpdate("insert into record_data (column1, column2) values(1,11)");
        st.executeUpdate("insert into record_data (column1, column2) values(2,10)");
        st.executeUpdate("insert into record_data (column1, column2) values(3,10)");
    }

    private List<Record> iteratorToList(DatabaseRecordQuery.RecordIterator it)
    {
        List<Record> list = new ArrayList<Record>();
        while (it.hasNext())
            list.add(it.next());

        return list;
    }

    private void testRecord(int col1Value, int col2Value, Record record) throws RecordException
    {
        assertEquals(col1Value, record.getValue("field1"));
        assertEquals(col2Value, record.getValue("field2"));
    }
}
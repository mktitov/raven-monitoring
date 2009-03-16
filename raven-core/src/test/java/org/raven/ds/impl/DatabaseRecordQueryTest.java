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

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
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
    private FilterableRecordFieldExtension filterExtension;

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

        IdRecordFieldExtension idExtension = new IdRecordFieldExtension();
        idExtension.setName("id");
        field.addAndSaveChildren(idExtension);
        assertTrue(idExtension.start());
        
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
        
        filterExtension = new FilterableRecordFieldExtension();
        filterExtension.setName("filter");
        field.addAndSaveChildren(filterExtension);
        assertTrue(filterExtension.start());

        field = new RecordSchemaFieldNode();
        field.setName("field3");
        schema.addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.BINARY);
        assertTrue(field.start());

        fieldExtension = new DatabaseRecordFieldExtension();
        fieldExtension.setName("db");
        field.addAndSaveChildren(fieldExtension);
        fieldExtension.setColumnName("column3");
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
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, null, null, null, pool, null, null);
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
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, null, null, "2=2", null, pool, null, null);
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
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, null, null, null, "column1", pool, null, null);
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
                "column1", Integer.class, null, false, converter);
        filterElement.setExpression("#is not null");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, null, Arrays.asList(filterElement), null, null, pool, null, null);
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
                "column1", Integer.class, null, false, converter);
        filterElement.setExpression(">10");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, null, Arrays.asList(filterElement), null, null, pool, null, null);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND (column1>?)"
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
                "column1", Integer.class, null, false, converter);
        filterElement.setExpression("10%");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, null, Arrays.asList(filterElement), null, null, pool, null, null);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND (column1 LIKE '10%')"
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
                "column1", Integer.class, null, false, converter);
        filterElement.setExpression("[1, 2]");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, null, Arrays.asList(filterElement), null, null, pool, null, null);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND (column1 BETWEEN ? AND ?)"
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
                "column1", Integer.class, null, false, converter);
        filterElement.setExpression("{1, 2}");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                schema, null, null, Arrays.asList(filterElement), null, null, pool, null, null);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND (column1 IN (?, ?))"
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
                schema, null, null, null, "select * from table", pool, null, null);
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
                schema, null, null, null, "select * from table where 1=1 {#}", pool, null, null);
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
                "column1", Integer.class, null, false, converter);
        filterElement.setExpression(">10");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                    schema, null, null, Arrays.asList(filterElement)
                    , "select * from table where 1=1 {#} order by column1", pool, null, null);
        assertEquals(
                "select * from table where 1=1 \n   AND (column1>?) order by column1"
                , recordQuery.getQuery());
    }

    /*
     * case insensitive test in SIMPLE operator
     */
    @Test
    public void queryConstructionTest_12() throws Exception
    {
        filterExtension.setCaseSensitive(false);
        DatabaseFilterElement filterElement = new DatabaseFilterElement(
                "column2", String.class, null, false, converter);
        filterElement.setExpression("a");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                    schema, null, null, Arrays.asList(filterElement), null, null
                    , pool, null, null);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND (upper(column2)=?)"
                , recordQuery.getQuery());
    }

    /*
     * case insensitive test in COMPLETE operator
     */
    @Test
    public void queryConstructionTest_13() throws Exception
    {
        filterExtension.setCaseSensitive(false);
        DatabaseFilterElement filterElement = new DatabaseFilterElement(
                "column2", String.class, null, false, converter);
        filterElement.setExpression("#expr");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                    schema, null, null, Arrays.asList(filterElement), null, null
                    , pool, null, null);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND (upper(column2) expr)"
                , recordQuery.getQuery());
    }

    /*
     * case insensitive test in LIKE operator
     */
    @Test
    public void queryConstructionTest_14() throws Exception
    {
        filterExtension.setCaseSensitive(false);
        DatabaseFilterElement filterElement = new DatabaseFilterElement(
                "column2", String.class, null, false, converter);
        filterElement.setExpression("%val");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                    schema, null, null, Arrays.asList(filterElement), null, null
                    , pool, null, null);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND (upper(column2) LIKE '%VAL')"
                , recordQuery.getQuery());
    }

    /*
     * case insensitive test in IN operator
     */
    @Test
    public void queryConstructionTest_15() throws Exception
    {
        filterExtension.setCaseSensitive(false);
        DatabaseFilterElement filterElement = new DatabaseFilterElement(
                "column2", String.class, null, false, converter);
        filterElement.setExpression("{val}");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                    schema, null, null, Arrays.asList(filterElement), null, null
                    , pool, null, null);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND (upper(column2) IN (?))"
                , recordQuery.getQuery());
    }

    /*
     * Virtual DatabaseFilterElement in the query template
     */
    @Test
    public void queryConstructionTest_16() throws Exception
    {
        filterExtension.setCaseSensitive(false);
        DatabaseFilterElement filterElement = new DatabaseFilterElement(
                "column2", String.class, null, true, converter);
        filterElement.setExpression("val");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                    schema, null, null, Arrays.asList(filterElement)
                    , "SELECT * FROM table WHERE col ${Column2}"
                    , pool, null, null);
        assertEquals("SELECT * FROM table WHERE col =?", recordQuery.getQuery());
    }

    /*
     * Virtual DatabaseFilterElement in the query template
     */
    @Test
    public void queryConstructionTest_17() throws Exception
    {
        filterExtension.setCaseSensitive(false);
        DatabaseFilterElement filterElement = new DatabaseFilterElement(
                "column2", String.class, null, true, converter);
        filterElement.setExpression("val");
        DatabaseRecordQuery recordQuery = new DatabaseRecordQuery(
                    schema, null, null, Arrays.asList(filterElement)
                    , "col ${coLumn2}", null
                    , pool, null, null);
        assertEquals(
                "\nSELECT column1, column2\nFROM record_data\nWHERE 1=1" +
                "\n   AND (col =?)"
                , recordQuery.getQuery());
    }

    @Test
    public void execute_noRecords() throws Exception
    {
        createTable(pool);
        DatabaseRecordQuery query = new DatabaseRecordQuery(
                schema, null, null, null, null, pool, null, null);

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
                "column1", Integer.class, null, false, converter);
        DatabaseFilterElement filterElement2 = new DatabaseFilterElement(
                "column2", String.class, null, false, converter);

        filterElement1.setExpression("[1,2]");
        filterElement2.setExpression("10");

        DatabaseRecordQuery query = new DatabaseRecordQuery(
                schema, null, null, Arrays.asList(filterElement1, filterElement2), null
                , "column1", pool, null, null);
        
        DatabaseRecordQuery.RecordIterator it = query.execute();
        assertNotNull(it);
        List<Record> recs = iteratorToList(it);
        assertEquals(2, recs.size());

        testRecord(1, "10", recs.get(0));
        testRecord(2, "10", recs.get(1));
    }

    @Test
    public void execute2Test() throws Exception
    {
        createTable(pool);
        insertData(pool);

        DatabaseFilterElement filterElement1 = new DatabaseFilterElement(
                "column1", Integer.class, null, false, converter);
        DatabaseFilterElement filterElement2 = new DatabaseFilterElement(
                "column2", String.class, null, false, converter);

        filterElement1.setExpression("{1,3}");
        filterElement2.setExpression("10");

        DatabaseRecordQuery query = new DatabaseRecordQuery(
                schema, null, null, Arrays.asList(filterElement1, filterElement2), null, "column1"
                , pool, null, null);

        DatabaseRecordQuery.RecordIterator it = query.execute();
        assertNotNull(it);
        List<Record> recs = iteratorToList(it);
        assertEquals(2, recs.size());

        testRecord(1, "10", recs.get(0));
        testRecord(3, "10", recs.get(1));
    }

    @Test
    public void execute3Test() throws Exception
    {
        createTable(pool);
        insertData(pool);

        DatabaseFilterElement filterElement1 = new DatabaseFilterElement(
                "column1", Integer.class, null, true, converter);
        DatabaseFilterElement filterElement2 = new DatabaseFilterElement(
                "column2", String.class, null, false, converter);

        filterElement1.setExpression("{1,3}");
        filterElement2.setExpression("10");

        DatabaseRecordQuery query = new DatabaseRecordQuery(
                schema, null, null, Arrays.asList(filterElement1, filterElement2)
                , "column1 ${column1}", "column1"
                , pool, null, null);

        DatabaseRecordQuery.RecordIterator it = query.execute();
        assertNotNull(it);
        List<Record> recs = iteratorToList(it);
        assertEquals(2, recs.size());

        testRecord(1, "10", recs.get(0));
        testRecord(3, "10", recs.get(1));
    }

    @Test
    public void execute4Test() throws Exception
    {
        createTable(pool);
        insertData3(pool);

        DatabaseFilterElement filterElement1 = new DatabaseFilterElement(
                "column1", Integer.class, null, true, converter);
        filterElement1.setExpression("1");

        DatabaseRecordQuery query = new DatabaseRecordQuery(
                schema, null, null, Arrays.asList(filterElement1)
                , null, null
                , pool, null, null);

        DatabaseRecordQuery.RecordIterator it = query.execute();
        assertNotNull(it);
        List<Record> recs = iteratorToList(it);
        assertEquals(1, recs.size());

        Record rec = recs.get(0);
        assertNotNull(rec);
        assertEquals(1, rec.getValue("field1"));
        Object val = rec.getValue("field3");
        assertNotNull(val);
        assertTrue(val instanceof DatabaseBinaryDataReader);
        DatabaseBinaryDataReader reader = (DatabaseBinaryDataReader) val;
        byte[] res = IOUtils.toByteArray(reader.getData());
        assertArrayEquals(new byte[]{1,2,3}, res);
    }

    /*
     * SIMPLE operator test 
     */
    @Test
    public void executeWithcaseSensitive_test_1() throws Exception
    {
        filterExtension.setCaseSensitive(false);
        createTable(pool);
        insertData2(pool);
        
        DatabaseFilterElement filterElement2 = new DatabaseFilterElement(
                "column2", String.class, null, false, converter);

        filterElement2.setExpression("aa");

        DatabaseRecordQuery query = new DatabaseRecordQuery(
                schema, null, null, Arrays.asList(filterElement2), null, null
                , pool, null, null);
        
        DatabaseRecordQuery.RecordIterator it = query.execute();
        assertNotNull(it);
        List<Record> recs = iteratorToList(it);
        assertEquals(1, recs.size());

        testRecord(1, "Aa", recs.get(0));
    }

    /*
     * IN operator test
     */
    @Test
    public void executeWithcaseSensitive_test_2() throws Exception
    {
        filterExtension.setCaseSensitive(false);
        createTable(pool);
        insertData2(pool);

        DatabaseFilterElement filterElement2 = new DatabaseFilterElement(
                "column2", String.class, null, false, converter);

        filterElement2.setExpression("{aa}");

        DatabaseRecordQuery query = new DatabaseRecordQuery(
                schema, null, null, Arrays.asList(filterElement2), null, null
                , pool, null, null);

        DatabaseRecordQuery.RecordIterator it = query.execute();
        assertNotNull(it);
        List<Record> recs = iteratorToList(it);
        assertEquals(1, recs.size());

        testRecord(1, "Aa", recs.get(0));
    }

    @Test
    public void maxRowsTest() throws Exception
    {
        createTable(pool);
        insertData(pool);

        DatabaseRecordQuery query = new DatabaseRecordQuery(
                schema, null, null, null, null, "column1", pool, 2, null);

        DatabaseRecordQuery.RecordIterator it = query.execute();
        assertNotNull(it);
        List<Record> recs = iteratorToList(it);
        assertEquals(2, recs.size());

        testRecord(1, "10", recs.get(0));
        testRecord(1, "11", recs.get(1));
    }

    private void createTable(JDBCConnectionPoolNode pool) throws SQLException
    {
        Connection con = pool.getConnection();
        Statement st = con.createStatement();
        st.executeUpdate("drop table if exists record_data");
        st.executeUpdate("create table record_data(column1 int, column2 varchar, column3 blob)");
    }

    private void insertData(JDBCConnectionPoolNode pool) throws SQLException
    {
        Connection con = pool.getConnection();
        Statement st = con.createStatement();
        st.executeUpdate("insert into record_data (column1, column2) values(1,'10')");
        st.executeUpdate("insert into record_data (column1, column2) values(1,'11')");
        st.executeUpdate("insert into record_data (column1, column2) values(2,'10')");
        st.executeUpdate("insert into record_data (column1, column2) values(3,'10')");
    }

    private void insertData2(JDBCConnectionPoolNode pool) throws SQLException
    {
        Connection con = pool.getConnection();
        Statement st = con.createStatement();
        st.executeUpdate("insert into record_data (column1, column2) values(1,'Aa')");
        st.executeUpdate("insert into record_data (column1, column2) values(1,'bb')");
    }

    private void insertData3(JDBCConnectionPoolNode pool) throws SQLException
    {
        Connection con = pool.getConnection();
        PreparedStatement st = con.prepareStatement(
                "insert into record_data (column1, column3) values(?,?)");
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1,2,3});
        st.setInt(1, 1);
//        st.setBytes(2, new byte[]{1,2,3});
        st.setObject(2, is);
        st.executeUpdate();
        con.commit();
    }

    private List<Record> iteratorToList(DatabaseRecordQuery.RecordIterator it)
    {
        List<Record> list = new ArrayList<Record>();
        while (it.hasNext())
            list.add(it.next());

        return list;
    }

    private void testRecord(int col1Value, String col2Value, Record record) throws RecordException
    {
        assertEquals(col1Value, record.getValue("field1"));
        assertEquals(col2Value, record.getValue("field2"));
    }
}
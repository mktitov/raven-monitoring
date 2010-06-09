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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.DataCollector;
import org.raven.test.RavenCoreTestCase;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.dbcp.impl.JDBCConnectionPoolNode;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.SystemNode;

/**
 *
 * @author Mikhail Titov
 */
public class DatabaseRecordReaderNodeTest extends RavenCoreTestCase
{
    private JDBCConnectionPoolNode pool;
    private DatabaseRecordReaderNode reader;
    private RecordSchemaNode schema;
    private FilterableRecordFieldExtension filterExtension;
    private DatabaseRecordFieldExtension dbExtension;
    private RecordSchemaFieldNode field;

    private DataCollector collector;

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
        assertEquals(Status.STARTED, pool.getStatus());
        
        schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        schema.start();
        assertEquals(Status.STARTED, schema.getStatus());

        DatabaseRecordExtension dbRecExtension = new DatabaseRecordExtension();
        dbRecExtension.setName("db");
        schema.getRecordExtensionsNode().addAndSaveChildren(dbRecExtension);
        dbRecExtension.setTableName("record_data");
        assertTrue(dbRecExtension.start());

        field = new RecordSchemaFieldNode();
        field.setName("field1");
        schema.addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.STRING);
        field.setDisplayName("field1 display name");
        field.start();
        assertEquals(Status.STARTED, field.getStatus());

        dbExtension = new DatabaseRecordFieldExtension();
        dbExtension.setName("db");
        field.addAndSaveChildren(dbExtension);
        dbExtension.setColumnName("COL1");
        dbExtension.save();
        dbExtension.start();
        assertEquals(Status.STARTED, dbExtension.getStatus());

        filterExtension = new FilterableRecordFieldExtension();
        filterExtension.setName("filter");
        field.addAndSaveChildren(filterExtension);
        filterExtension.setDefaultValue("test");
        filterExtension.setFilterValueRequired(false);
        filterExtension.save();
        filterExtension.start();
        assertEquals(Status.STARTED, filterExtension.getStatus());

        reader = new DatabaseRecordReaderNode();
        reader.setName("reader");
        tree.getRootNode().addAndSaveChildren(reader);
        reader.setConnectionPool(pool);
    }

    @Test
    public void setRecordSchemaAttributeTest()
    {
        assertTrue(getFilterAttributes().isEmpty());

        reader.setRecordSchema(schema);
        List<NodeAttribute> filterAttrs = getFilterAttributes();
        assertEquals(1, filterAttrs.size());
        checkFilterAttribute(filterAttrs.get(0));

        reader.setRecordSchema(null);
        assertTrue(getFilterAttributes().isEmpty());

        reader.setRecordSchema(schema);
        assertTrue(reader.start());

        filterAttrs = getFilterAttributes();
        assertEquals(1, filterAttrs.size());
        checkFilterAttribute(filterAttrs.get(0));
    }

    @Test
    public void schemaChangeTest()
    {
        reader.setRecordSchema(schema);

        assertEquals(1, getFilterAttributes().size());

        field.stop();
        assertTrue(getFilterAttributes().isEmpty());
        field.start();
        checkFilterAttributes();

        dbExtension.stop();
        assertFalse(getFilterAttributes().isEmpty());
        dbExtension.start();
        checkFilterAttributes();

        filterExtension.stop();
        assertTrue(getFilterAttributes().isEmpty());
        filterExtension.start();
        checkFilterAttributes();
    }

    @Test
    public void fieldNameChangeTest()
    {
        reader.setRecordSchema(schema);
        field.setName("newField");

        List<NodeAttribute> filterAttrs = getFilterAttributes();
        assertEquals(1, filterAttrs.size());
        assertEquals("newField", filterAttrs.get(0).getName());
    }

    @Test
    public void requiredFilterValueChangeTest() throws Exception
    {
        reader.setRecordSchema(schema);

        checkFilterAttributes();
        filterExtension.setFilterValueRequired(true);
        List<NodeAttribute> filterAttrs = getFilterAttributes();
        assertEquals(1, filterAttrs.size());
        assertTrue(filterAttrs.get(0).isRequired());

        filterExtension.setFilterValueRequired(false);
        filterAttrs = getFilterAttributes();
        assertEquals(1, filterAttrs.size());
        assertFalse(filterAttrs.get(0).isRequired());
        filterAttrs.get(0).setValue(null);

        reader.start();
        assertEquals(Status.STARTED, reader.getStatus());
        filterExtension.setFilterValueRequired(true);
        assertEquals(Status.INITIALIZED, reader.getStatus());
        filterAttrs = getFilterAttributes();
        assertEquals(1, filterAttrs.size());
        assertTrue(filterAttrs.get(0).isRequired());
    }

    @Test
    public void provideFilterAttributesToConsumersTest()
    {
//        reader.setRecordSchema(schema);
//        filterExtension.setFilterValueRequired(true);
//        assertTrue(reader.start());
//        reader.setProvideFilterAttributesToConsumers(true);
//
//        checkFilterAttributes();
    }

    @Test
    public void gatherDataTest() throws Exception
    {
        prepareCollector();
        prepareData();

        reader.setRecordSchema(schema);
        reader.getNodeAttribute("field1").setValue(null);
        assertTrue(reader.start());
        reader.getDataImmediate(collector, new DataContextImpl());

        assertEquals(5, collector.getDataList().size());
    }

    @Test
    public void gatherDataWithOrderByTest() throws Exception
    {
        prepareCollector();
        prepareData();

        reader.setRecordSchema(schema);
        reader.getNodeAttribute("field1").setValue(null);
        reader.setOrderByExpression("col1");
        assertTrue(reader.start());
        reader.getDataImmediate(collector, new DataContextImpl());

        checkRecords(collector.getDataList(), "1", "3", "4", "5");
    }

    @Test
    public void gatherDataWithWhereExpressionTest() throws Exception
    {
        prepareCollector();
        prepareData();

        reader.setRecordSchema(schema);
        reader.getNodeAttribute("field1").setValue(null);
        reader.setOrderByExpression("col1");
        reader.setWhereExpression("col1 in ('3', '5')");
        assertTrue(reader.start());
        reader.getDataImmediate(collector, new DataContextImpl());

        checkRecords(collector.getDataList(), "3", "5");
    }

    @Test
    public void gatherDataWithWhereExpressionTest2() throws Exception
    {
        RecordSchemaFieldNode virtualField = new RecordSchemaFieldNode();
        virtualField.setName("field2");
        schema.addAndSaveChildren(virtualField);
        virtualField.setFieldType(RecordSchemaFieldType.STRING);
        assertTrue(virtualField.start());
        FilterableRecordFieldExtension filterExt = new FilterableRecordFieldExtension();
        filterExt.setName("filter");
        virtualField.addAndSaveChildren(filterExt);
        assertTrue(filterExt.start());

        prepareCollector();
        prepareData();

        reader.setRecordSchema(schema);
        reader.getNodeAttribute("field1").setValue(null);
        reader.getNodeAttribute("field2").setValue("5");
        reader.setOrderByExpression("col1");
        reader.setWhereExpression("col1 ${field2}");
        assertTrue(reader.start());
        reader.getDataImmediate(collector, new DataContextImpl());

        checkRecords(collector.getDataList(), "5");
    }

    @Test
    public void gatherDataWithMaxRowsTest() throws Exception
    {
        prepareCollector();
        prepareData();

        reader.setRecordSchema(schema);
        reader.getNodeAttribute("field1").setValue(null);
        reader.setOrderByExpression("col1");
        reader.setFetchSize(2);
        reader.setMaxRows(2);
        assertTrue(reader.start());
        reader.getDataImmediate(collector, new DataContextImpl());

        checkRecords(collector.getDataList(), "1", "3");
    }

    @Test
    public void gatherDataWithQueryTemplateTest() throws Exception
    {
        prepareCollector();
        prepareData();

        reader.setRecordSchema(schema);
        reader.getNodeAttribute("field1").setValue(null);
        reader.setQuery("select * from record_data where col1 in ('1', '4') order by col1");
        assertTrue(reader.start());
        reader.getDataImmediate(collector, new DataContextImpl());

        checkRecords(collector.getDataList(), "1", "4");
    }

    @Test
    public void gatherDataWithFilterAttributesTest() throws SQLException, RecordException, Exception
    {
        prepareCollector();
        prepareData();

        reader.setRecordSchema(schema);
        reader.setOrderByExpression("col1");
        reader.getNodeAttribute("field1").setValue("{3, 5}");
        assertTrue(reader.start());
        reader.getDataImmediate(collector, new DataContextImpl());

        checkRecords(collector.getDataList(), "3", "5");
    }

    @Test
    public void gatherDataWithQueryTemplateAndFilterAttributesTest() throws Exception
    {
        prepareCollector();
        prepareData();

        reader.setRecordSchema(schema);
        reader.setQuery("select * from record_data where 1=1 {#} order by col1");
        reader.getNodeAttribute("field1").setValue("{1, 4}");
        assertTrue(reader.start());
        reader.getDataImmediate(collector, new DataContextImpl());

        checkRecords(collector.getDataList(), "1", "4");
    }

    //provideFilterAttributesToConsumer==true
    @Test
    public void generateAttributesTest1() throws Exception
    {
        filterExtension.setFilterValueRequired(true);
        reader.setRecordSchema(schema);
        reader.setProvideFilterAttributesToConsumers(true);

        Collection<NodeAttribute> attrs = reader.generateAttributes();
        assertNotNull(attrs);
        assertEquals(1, attrs.size());

        NodeAttribute attr = attrs.iterator().next();
        assertNotNull(attr);
        assertEquals("field1", attr.getName());
        assertEquals("field1 display name", attr.getDisplayName());
        assertEquals(String.class, attr.getType());
        assertEquals("test", attr.getRawValue());
        assertTrue(attr.isRequired());
        assertNotNull(attr.getDescription());
    }

    //provideFilterAttributesToConsumer==false
    @Test
    public void generateAttributesTest2() throws Exception
    {
        reader.setRecordSchema(schema);
        reader.setProvideFilterAttributesToConsumers(false);

        assertNull(reader.generateAttributes());
    }

    private void checkRecords(Collection records, String... values) throws RecordException
    {
        int i=0;
        assertEquals(values.length+1, records.size());
        Iterator it = records.iterator();
        for (i=0; i<values.length; ++i)
            assertEquals(values[i], ((Record)it.next()).getValue("field1"));

        assertNull(it.next());
    }

    private void checkRecord(Record record, String field1Value) throws RecordException
    {
        assertEquals(field1Value, record.getValue("field1"));
    }

    private void prepareCollector()
    {
        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(reader);
        assertTrue(collector.start());
    }

    private void prepareData() throws SQLException
    {
        Connection con = pool.getConnection();
        Statement st = con.createStatement();
        st.executeUpdate("drop table if exists record_data");
        st.executeUpdate("create table record_data(col1 varchar)");
        
        st.executeUpdate("insert into record_data (col1) values('3')");
        st.executeUpdate("insert into record_data (col1) values('4')");
        st.executeUpdate("insert into record_data (col1) values('1')");
        st.executeUpdate("insert into record_data (col1) values('5')");
    }

    private void checkFilterAttribute(NodeAttribute attr)
    {
        assertEquals("field1", attr.getName());
        assertEquals(String.class, attr.getType());
        assertEquals("test", attr.getValue());
        assertFalse(attr.isRequired());
    }

    private void checkFilterAttributes()
    {
        List<NodeAttribute> filterAttrs = getFilterAttributes();
        assertEquals(1, filterAttrs.size());
        checkFilterAttribute(filterAttrs.get(0));
    }

    private List<NodeAttribute> getFilterAttributes()
    {
        Collection<NodeAttribute> attrs = reader.getNodeAttributes();
        if (attrs==null || attrs.size()==0)
            return Collections.EMPTY_LIST;

        List<NodeAttribute> filterAttrs = new ArrayList<NodeAttribute>();
        for (NodeAttribute attr: attrs)
            if (DatabaseRecordReaderNode.RECORD_SCHEMA_ATTR.equals(attr.getParentAttribute()))
                filterAttrs.add(attr);

        return filterAttrs;
    }
}
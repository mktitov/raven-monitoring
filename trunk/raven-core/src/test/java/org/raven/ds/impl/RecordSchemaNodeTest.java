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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.raven.RavenUtils;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.dbcp.ConnectionPool;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.dbcp.impl.JDBCConnectionPoolNode;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.LeafNode;
import org.raven.tree.impl.SystemNode;

/**
 *
 * @author Mikhail Titov
 */
public class RecordSchemaNodeTest extends RavenCoreTestCase
{
    @Test
    public void getTableMetadataTest() throws Exception
    {
        ConnectionPool pool = createConnectionPool();
        RecordSchemaNode record = new RecordSchemaNode();
        record.setName("rec");
        tree.getRootNode().addAndSaveChildren(record);
        assertTrue(record.start());
        record.setConnectionPool(pool);
        DatabaseRecordExtension dbExt = new DatabaseRecordExtension();
        dbExt.setName("dbTable");
        record.getRecordExtensionsNode().addAndSaveChildren(dbExt);
        dbExt.setTableName("NODES");
        assertTrue(dbExt.start());

        List<ViewableObject> vos = record.getViewableObjects(null);
        assertNotNull(vos);
        assertEquals(1, vos.size());
        ViewableObject vo = vos.get(0);
        assertTrue(vo instanceof RecordSchemaNode.CreateRecordFromTableAction);

        assertEquals("Record successfully created", vo.getData());
        checkField(record, "id", RecordSchemaFieldType.INTEGER);
        checkField(record, "name", RecordSchemaFieldType.STRING);
        checkField(record, "nodeType", RecordSchemaFieldType.STRING);
    }

    @Test
    public void getFieldsTest() throws Exception
    {
        RecordSchemaNode schemaNode = new RecordSchemaNode();
        schemaNode.setName("schema");
        tree.getRootNode().addAndSaveChildren(schemaNode);
        schemaNode.start();
        assertEquals(Status.STARTED, schemaNode.getStatus());

        assertNull(schemaNode.getFields());
        assertTrue(schemaNode.getFieldsMap().isEmpty());
        assertNull(schemaNode.getField("field"));

        RecordSchemaFieldNode fieldNode = new RecordSchemaFieldNode();
        fieldNode.setName("field");
        schemaNode.addAndSaveChildren(fieldNode);

        assertNull(schemaNode.getFields());
        assertTrue(schemaNode.getFieldsMap().isEmpty());
        assertNull(schemaNode.getField("field"));

        fieldNode.setFieldType(RecordSchemaFieldType.INTEGER);
        fieldNode.start();
        assertEquals(Status.STARTED, fieldNode.getStatus());
        assertNotNull(schemaNode.getFields());
        assertEquals(1, schemaNode.getFields().length);
        assertSame(fieldNode, schemaNode.getFields()[0]);
        Map<String, RecordSchemaField> fields = schemaNode.getFieldsMap();
        assertNotNull(fields);
        assertEquals(1, fields.size());
        assertSame(fieldNode, fields.get("field"));
        assertSame(fieldNode, schemaNode.getField("field"));
    }
    
    @Test public void adjustTest() {
        RecordSchemaNode schema = createSchema("Test schema");
        RecordSchemaField field1 = addField(schema, "field1", RecordSchemaFieldType.STRING);
        RecordSchemaField field2 = addField(schema, "field2", RecordSchemaFieldType.STRING);
        RecordSchema adjustedSchema = schema.adjust("adjusted schema", Arrays.asList("field2"));
        assertEquals("adjusted schema", adjustedSchema.getName());
        assertArrayEquals(new RecordSchemaField[]{field2}, adjustedSchema.getFields());
        adjustedSchema = schema.adjust("adjusted schema", null, Arrays.asList("field1"));
        assertEquals("adjusted schema", adjustedSchema.getName());
        assertArrayEquals(new RecordSchemaField[]{field2}, adjustedSchema.getFields());
    }
    
    private RecordSchemaNode createSchema(String name) {
        RecordSchemaNode schemaNode = new RecordSchemaNode();
        schemaNode.setName(name);
        testsNode.addAndSaveChildren(schemaNode);
        schemaNode.start();
        assertEquals(Status.STARTED, schemaNode.getStatus());
        return schemaNode;
    }
    
    private RecordSchemaFieldNode addField(RecordSchemaNode schema, String fieldName, RecordSchemaFieldType fieldType) {
        RecordSchemaFieldNode fieldNode = new RecordSchemaFieldNode();
        fieldNode.setName(fieldName);
        schema.addAndSaveChildren(fieldNode);
        fieldNode.setFieldType(fieldType);
        assertTrue(fieldNode.start());
        return fieldNode;
    }

    @Test
    public void extendsSchemaTest() throws Exception
    {
        RecordSchemaNode schemaNode = new RecordSchemaNode();
        schemaNode.setName("schema");
        tree.getRootNode().addAndSaveChildren(schemaNode);
        schemaNode.start();
        assertEquals(Status.STARTED, schemaNode.getStatus());

        assertNull(schemaNode.getFields());

        RecordSchemaFieldNode fieldNode = new RecordSchemaFieldNode();
        fieldNode.setName("field");
        schemaNode.addAndSaveChildren(fieldNode);
        fieldNode.setFieldType(RecordSchemaFieldType.INTEGER);
        fieldNode.start();

        RecordSchemaNode schemaNode2 = new RecordSchemaNode();
        schemaNode2.setName("schema2");
        tree.getRootNode().addAndSaveChildren(schemaNode2);
        schemaNode2.setExtendsSchema(schemaNode);
        schemaNode2.start();
        assertEquals(Status.STARTED, schemaNode2.getStatus());

        assertNotNull(schemaNode2.getFields());
        assertEquals(1, schemaNode2.getFields().length);
        assertSame(fieldNode, schemaNode2.getFields()[0]);
    }

    @Test
    public void includeFieldsTest() throws Exception
    {
        RecordSchemaNode schemaNode = new RecordSchemaNode();
        schemaNode.setName("schema");
        tree.getRootNode().addAndSaveChildren(schemaNode);
        schemaNode.start();
        assertEquals(Status.STARTED, schemaNode.getStatus());

        assertNull(schemaNode.getFields());

        RecordSchemaFieldNode fieldNode = new RecordSchemaFieldNode();
        fieldNode.setName("field");
        schemaNode.addAndSaveChildren(fieldNode);
        fieldNode.setFieldType(RecordSchemaFieldType.INTEGER);
        fieldNode.start();

        RecordSchemaFieldNode fieldNode2 = new RecordSchemaFieldNode();
        fieldNode2.setName("field2");
        schemaNode.addAndSaveChildren(fieldNode2);
        fieldNode2.setFieldType(RecordSchemaFieldType.INTEGER);
        fieldNode2.start();

        RecordSchemaFieldNode fieldNode3 = new RecordSchemaFieldNode();
        fieldNode3.setName("field3");
        schemaNode.addAndSaveChildren(fieldNode3);
        fieldNode3.setFieldType(RecordSchemaFieldType.INTEGER);
        fieldNode3.start();

        RecordSchemaNode schemaNode2 = new RecordSchemaNode();
        schemaNode2.setName("schema2");
        tree.getRootNode().addAndSaveChildren(schemaNode2);
        schemaNode2.setExtendsSchema(schemaNode);
        schemaNode2.setIncludeFields("field3, field2");
        schemaNode2.start();
        assertEquals(Status.STARTED, schemaNode2.getStatus());

        assertNotNull(schemaNode2.getFields());
        assertEquals(2, schemaNode2.getFields().length);
        assertSame(fieldNode2, schemaNode2.getFields()[0]);
        assertSame(fieldNode3, schemaNode2.getFields()[1]);
    }

    @Test
    public void excludeFieldsTest() throws Exception
    {
        RecordSchemaNode schemaNode = new RecordSchemaNode();
        schemaNode.setName("schema");
        tree.getRootNode().addAndSaveChildren(schemaNode);
        schemaNode.start();
        assertEquals(Status.STARTED, schemaNode.getStatus());

        assertNull(schemaNode.getFields());

        RecordSchemaFieldNode fieldNode = new RecordSchemaFieldNode();
        fieldNode.setName("field");
        schemaNode.addAndSaveChildren(fieldNode);
        fieldNode.setFieldType(RecordSchemaFieldType.INTEGER);
        fieldNode.start();

        RecordSchemaFieldNode fieldNode2 = new RecordSchemaFieldNode();
        fieldNode2.setName("field2");
        schemaNode.addAndSaveChildren(fieldNode2);
        fieldNode2.setFieldType(RecordSchemaFieldType.INTEGER);
        fieldNode2.start();

        RecordSchemaFieldNode fieldNode3 = new RecordSchemaFieldNode();
        fieldNode3.setName("field3");
        schemaNode.addAndSaveChildren(fieldNode3);
        fieldNode3.setFieldType(RecordSchemaFieldType.INTEGER);
        fieldNode3.start();

        RecordSchemaNode schemaNode2 = new RecordSchemaNode();
        schemaNode2.setName("schema2");
        tree.getRootNode().addAndSaveChildren(schemaNode2);
        schemaNode2.setExtendsSchema(schemaNode);
        schemaNode2.setExcludeFields("field, field2");
        schemaNode2.start();
        assertEquals(Status.STARTED, schemaNode2.getStatus());

        assertNotNull(schemaNode2.getFields());
        assertEquals(1, schemaNode2.getFields().length);
        assertSame(fieldNode3, schemaNode2.getFields()[0]);
    }

    @Test
    public void parentFieldSubstituteTest() throws Exception
    {
        RecordSchemaNode schemaNode = new RecordSchemaNode();
        schemaNode.setName("schema");
        tree.getRootNode().addAndSaveChildren(schemaNode);
        schemaNode.start();
        assertEquals(Status.STARTED, schemaNode.getStatus());

        assertNull(schemaNode.getFields());

        RecordSchemaFieldNode fieldNode = new RecordSchemaFieldNode();
        fieldNode.setName("field");
        schemaNode.addAndSaveChildren(fieldNode);
        fieldNode.setFieldType(RecordSchemaFieldType.INTEGER);
        fieldNode.start();

        RecordSchemaNode schemaNode2 = new RecordSchemaNode();
        schemaNode2.setName("schema2");
        tree.getRootNode().addAndSaveChildren(schemaNode2);
        schemaNode2.setExtendsSchema(schemaNode);
        schemaNode2.start();
        assertEquals(Status.STARTED, schemaNode2.getStatus());

        RecordSchemaFieldNode fieldNode2 = new RecordSchemaFieldNode();
        fieldNode2.setName("field");
        schemaNode2.addAndSaveChildren(fieldNode2);
        fieldNode2.setFieldType(RecordSchemaFieldType.INTEGER);
        fieldNode2.start();

        assertNotNull(schemaNode2.getFields());
        assertEquals(1, schemaNode2.getFields().length);
        assertSame(fieldNode2, schemaNode2.getFields()[0]);
    }

    @Test
    public void getRecordExtensionTest() throws Exception
    {
        RecordSchemaNode schemaNode = new RecordSchemaNode();
        schemaNode.setName("schema");
        tree.getRootNode().addAndSaveChildren(schemaNode);
        schemaNode.start();
        assertEquals(Status.STARTED, schemaNode.getStatus());

        RecordExtensionsNode extensionsNode = schemaNode.getRecordExtensionsNode();
        assertNotNull(extensionsNode);
        assertEquals(Status.STARTED, extensionsNode.getStatus());

        assertNull(schemaNode.getRecordExtension(ContainerNode.class, null));

        ContainerNode ext1 = new ContainerNode("ext1");
        extensionsNode.addAndSaveChildren(ext1);

        assertNull(schemaNode.getRecordExtension(ContainerNode.class, null));

        ext1.start();

        assertNotNull(schemaNode.getRecordExtension(ContainerNode.class, null));
        assertSame(ext1, schemaNode.getRecordExtension(ContainerNode.class, null));

        LeafNode ext2 = new LeafNode("ext2");
        extensionsNode.addAndSaveChildren(ext2);
        ext2.start();

        assertNotNull(schemaNode.getRecordExtension(ContainerNode.class, null));
        assertSame(ext1, schemaNode.getRecordExtension(ContainerNode.class, null));
        assertNotNull(schemaNode.getRecordExtension(LeafNode.class, null));
        assertSame(ext2, schemaNode.getRecordExtension(LeafNode.class, null));

        ContainerNode ext3 = new ContainerNode("ext3");
        extensionsNode.addAndSaveChildren(ext3);
        assertTrue(ext3.start());

        assertNotNull(schemaNode.getRecordExtension(ContainerNode.class, null));
        assertSame(ext1, schemaNode.getRecordExtension(ContainerNode.class, "ext1"));
        assertSame(ext3, schemaNode.getRecordExtension(ContainerNode.class, "ext3"));
    }

    @Test
    public void getRecordExtensionWithExtendsSchema() throws Exception
    {
        RecordSchemaNode parentSchema = new RecordSchemaNode();
        parentSchema.setName("parent");
        tree.getRootNode().addAndSaveChildren(parentSchema);
        parentSchema.start();
        assertEquals(Status.STARTED, parentSchema.getStatus());

        ContainerNode parentExt1 = new ContainerNode("ext1");
        parentSchema.getRecordExtensionsNode().addAndSaveChildren(parentExt1);
        parentExt1.start();
        assertEquals(Status.STARTED, parentExt1.getStatus());

        LeafNode parentExt2 = new LeafNode("ext2");
        parentSchema.getRecordExtensionsNode().addAndSaveChildren(parentExt2);
        parentExt2.start();
        assertEquals(Status.STARTED, parentExt2.getStatus());

        RecordSchemaNode schemaNode = new RecordSchemaNode();
        schemaNode.setName("schema");
        tree.getRootNode().addAndSaveChildren(schemaNode);
        schemaNode.setExtendsSchema(parentSchema);
        schemaNode.start();
        assertEquals(Status.STARTED, schemaNode.getStatus());

        RecordExtensionsNode extensionsNode = schemaNode.getRecordExtensionsNode();
        assertNotNull(extensionsNode);
        assertEquals(Status.STARTED, extensionsNode.getStatus());

        ContainerNode ext1 = new ContainerNode("ext1");
        extensionsNode.addAndSaveChildren(ext1);
        ext1.start();
        assertEquals(Status.STARTED, ext1.getStatus());

        assertNotNull(schemaNode.getRecordExtension(ContainerNode.class, null));
        assertSame(ext1, schemaNode.getRecordExtension(ContainerNode.class, null));

        assertNotNull(schemaNode.getRecordExtension(LeafNode.class, null));
        assertSame(parentExt2, schemaNode.getRecordExtension(LeafNode.class, null));
    }

    private ConnectionPool createConnectionPool() throws Exception
    {
        Config conf = configurator.getConfig();
        assertNotNull(conf);

        ConnectionPoolsNode poolsNode =
                (ConnectionPoolsNode)
                tree.getNode(SystemNode.NAME).getChildren(ConnectionPoolsNode.NAME);
        assertNotNull(poolsNode);
        JDBCConnectionPoolNode pool = new JDBCConnectionPoolNode();
        pool.setName("pool");
        poolsNode.addChildren(pool);
        pool.save();
        pool.init();

        pool.setUserName(conf.getStringProperty(Configurator.TREE_STORE_USER, null));
        pool.setPassword(conf.getStringProperty(Configurator.TREE_STORE_PASSWORD, null));
        pool.setUrl(conf.getStringProperty(Configurator.TREE_STORE_URL, null));
        pool.setDriver("org.h2.Driver");
        assertTrue(pool.start());

        return pool;
    }

    private void checkField(RecordSchemaNode rec, String name, RecordSchemaFieldType type)
    {
        RecordSchemaFieldNode field = (RecordSchemaFieldNode) rec.getChildren(name);
        assertNotNull(field);
        assertEquals(type, field.getFieldType());
        DatabaseRecordFieldExtension dbExt = field.getFieldExtension(
                DatabaseRecordFieldExtension.class, "dbColumn");
        assertNotNull(dbExt);
        assertEquals(RavenUtils.nameToDbName(name), dbExt.getColumnName());
        Node notNullValidator = rec.getNodeByPath(name+"/notNull");
        assertNotNull(notNullValidator);
        assertTrue(notNullValidator.isStarted());
        assertTrue(notNullValidator instanceof RequiredValueValidatorNode);
        
    }
}
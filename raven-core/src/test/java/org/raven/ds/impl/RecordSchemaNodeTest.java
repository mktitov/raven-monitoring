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

import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.LeafNode;

/**
 *
 * @author Mikhail Titov
 */
public class RecordSchemaNodeTest extends RavenCoreTestCase
{
    @Test
    public void getFieldsTest() throws Exception
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

        assertNull(schemaNode.getFields());

        fieldNode.setFieldType(RecordSchemaFieldType.INTEGER);
        fieldNode.start();
        assertEquals(Status.STARTED, fieldNode.getStatus());
        assertNotNull(schemaNode.getFields());
        assertEquals(1, schemaNode.getFields().length);
        assertSame(fieldNode, schemaNode.getFields()[0]);
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
        schemaNode2.setIncludeFields("field2, field3");
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
        assertEquals(Status.STARTED, extensionsNode);

        assertNull(schemaNode.getRecordExtension(ContainerNode.class));

        ContainerNode ext1 = new ContainerNode("ext1");
        extensionsNode.addAndSaveChildren(ext1);
        
        assertNull(schemaNode.getRecordExtension(ContainerNode.class));

        ext1.start();

        assertNotNull(schemaNode.getRecordExtension(ContainerNode.class));
        assertSame(ext1, schemaNode.getRecordExtension(ContainerNode.class));

        LeafNode ext2 = new LeafNode("ext2");
        extensionsNode.addAndSaveChildren(ext2);
        ext2.start();
        
        assertNotNull(schemaNode.getRecordExtension(ContainerNode.class));
        assertSame(ext1, schemaNode.getRecordExtension(ContainerNode.class));
        assertNotNull(schemaNode.getRecordExtension(LeafNode.class));
        assertSame(ext2, schemaNode.getRecordExtension(LeafNode.class));
    }
}
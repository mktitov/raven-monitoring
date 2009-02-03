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
}
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
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.LeafNode;

/**
 *
 * @author Mikhail Titov
 */
public class RecordSchemaFieldNodeTest extends RavenCoreTestCase
{
    @Test
    public void getFieldExtensionTest()
    {
        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
        field.setName("field");
        tree.getRootNode().addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.LONG);
        field.start();
        assertEquals(Status.STARTED, field.getStatus());

        assertNull(field.getFieldExtension(ContainerNode.class, null));

        ContainerNode node1 = new ContainerNode("node1");
        field.addAndSaveChildren(node1);

        assertNull(field.getFieldExtension(ContainerNode.class, null));

        node1.start();

        assertSame(node1, field.getFieldExtension(ContainerNode.class, null));

        LeafNode node2 = new LeafNode("node2");
        field.addAndSaveChildren(node2);
        node2.start();

        assertSame(node1, field.getFieldExtension(ContainerNode.class, null));
        assertSame(node2, field.getFieldExtension(LeafNode.class, null));

        ContainerNode node3 = new ContainerNode("node3");
        field.addAndSaveChildren(node3);
        assertTrue(node3.start());

        assertSame(node1, field.getFieldExtension(ContainerNode.class, "node1"));
        assertSame(node3, field.getFieldExtension(ContainerNode.class, "node3"));

    }
}
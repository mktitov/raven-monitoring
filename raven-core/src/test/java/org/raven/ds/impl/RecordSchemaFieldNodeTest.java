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

import java.util.Collection;
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

    @Test
    public void validateTest()
    {
        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
        field.setName("field");
        tree.getRootNode().addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.INTEGER);
        assertTrue(field.start());

        assertNull(field.validate(1));
        assertNull(field.validate(null));

        CustomValueValidatorNode validator = new CustomValueValidatorNode();
        validator.setName("validator");
        field.addAndSaveChildren(validator);
        validator.setValidateExpression("value==null?'empty':null");
        assertTrue(validator.start());

        Collection<String> errors = field.validate(null);
        assertNotNull(errors);
        assertArrayEquals(new Object[]{"empty"}, errors.toArray());
        assertNull(field.validate(1));

        validator.stop();
        assertNull(field.validate(null));
        validator.start();

        CustomValueValidatorNode validator2 = new CustomValueValidatorNode();
        validator2.setName("validator2");
        field.addAndSaveChildren(validator2);
        validator2.setValidateExpression("value==null?'empty2':null");
        assertTrue(validator2.start());

        errors = field.validate(null);
        assertNotNull(errors);
        assertArrayEquals(new Object[]{"empty", "empty2"}, errors.toArray());
    }
}
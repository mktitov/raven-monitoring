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
import org.raven.DataCollector;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaFieldType;

/**
 *
 * @author Mikhail Titov
 */
public class RecordGeneratorNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws RecordException
    {
        RecordSchemaNode schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);

        createField(schema, "f1");
        createField(schema, "f2");

        RecordGeneratorNode recordGenerator = new RecordGeneratorNode();
        recordGenerator.setName("recordGenerator");
        tree.getRootNode().addAndSaveChildren(recordGenerator);
        recordGenerator.setRecordSchema(schema);
        assertTrue(recordGenerator.start());

        createFieldValue(recordGenerator, "f1", "v1", true);
        createFieldValue(recordGenerator, "f2", "v2", true);
        createFieldValue(recordGenerator, "f3", "v1", false);

        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(recordGenerator);
        collector.setResetDataPolicy(AbstractDataConsumer.ResetDataPolicy.DONT_RESET_DATA);
        assertTrue(collector.start());

        Object data = collector.refereshData(null);
        assertNotNull(data);
        assertTrue(data instanceof Record);
        Record rec = (Record) data;
        assertEquals("v1", rec.getValue("f1"));
        assertEquals("v2", rec.getValue("f2"));
    }

    private void createField(RecordSchemaNode schema, String name)
    {
        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
        field.setName(name);
        schema.addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.STRING);
        assertTrue(field.start());
    }

    private void createFieldValue(
            RecordGeneratorNode recordGenerator, String name, String value, boolean start)
    {
        AttributeRecordFieldValueGenerator fieldValue = new AttributeRecordFieldValueGenerator();
        fieldValue.setName(name);
        recordGenerator.addAndSaveChildren(fieldValue);
        fieldValue.setValue(value);
        if (start)
            assertTrue(fieldValue.start());
    }
}
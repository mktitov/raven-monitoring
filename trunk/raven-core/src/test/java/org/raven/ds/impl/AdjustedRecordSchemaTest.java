/*
 * Copyright 2015 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.ds.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.test.ServiceTestCase;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class AdjustedRecordSchemaTest extends ServiceTestCase {

    @Test
    public void includeFieldsTest(
            @Mocked final RecordSchema baseSchema,
            @Mocked final RecordSchemaField field1,
            @Mocked final RecordSchemaField field2
    ) throws Exception 
    {
        new Expectations() {{
            baseSchema.getFields(); result = new RecordSchemaField[]{field1, field2};
            field1.getName(); result = "field1";
            field2.getName(); result = "field2";
        }};
        AdjustedRecordSchema schema = new AdjustedRecordSchema("schema", baseSchema, Arrays.asList("field1"), null);
        assertEquals("schema", schema.getName());
        assertArrayEquals(new RecordSchemaField[]{field1}, schema.getFields());
        assertTrue(schema.getFieldsMap().size()==1);
        assertEquals(field1, schema.getFieldsMap().get("field1"));
        assertEquals(field1, schema.getField("field1"));
    }
    
    @Test
    public void excludeFieldsTest(
            @Mocked final RecordSchema baseSchema,
            @Mocked final RecordSchemaField field1,
            @Mocked final RecordSchemaField field2
    ) throws Exception 
    {
        new Expectations() {{
            baseSchema.getFields(); result = new RecordSchemaField[]{field1, field2};
            field1.getName(); result = "field1";
            field2.getName(); result = "field2";
        }};
        AdjustedRecordSchema schema = new AdjustedRecordSchema("schema", baseSchema, null, Arrays.asList("field2"));
        assertEquals("schema", schema.getName());
        assertArrayEquals(new RecordSchemaField[]{field1}, schema.getFields());
        assertTrue(schema.getFieldsMap().size()==1);
        assertEquals(field1, schema.getFieldsMap().get("field1"));
        assertEquals(field1, schema.getField("field1"));
    }
    
    @Test
    public void getExtensionTest(
            @Mocked final RecordSchema baseSchema
    ) {
        new Expectations(){{
            baseSchema.getFields(); result = null;
            baseSchema.getRecordExtension((Class)any, anyString); result = "test-ext"; 
        }};
        AdjustedRecordSchema schema = new AdjustedRecordSchema("schema", baseSchema, null, Arrays.asList("field2"));
        assertEquals("test-ext", schema.getRecordExtension(String.class, "ext"));
        new Verifications() {{
            baseSchema.getRecordExtension((Class<String>)any, "ext");
        }};
    }
    
    @Test
    public void createRecordTest(
            @Mocked final RecordSchema baseSchema,
            @Mocked final RecordSchemaField field1
    ) throws RecordException 
    {
        new Expectations() {{
            baseSchema.getFields(); result = new RecordSchemaField[]{field1};
            field1.getName(); result = "field1";
//            field1.getFieldType(); result = RecordSchemaFieldType.STRING;
        }};
        AdjustedRecordSchema schema = new AdjustedRecordSchema("schema", baseSchema, null, Arrays.asList("field2"));
        Record rec = schema.createRecord();
        assertNotNull(rec);
        assertEquals(schema, rec.getSchema());
    }
    
    @Test
    public void createRecordWithValuesTest(
            @Mocked final RecordSchema baseSchema,
            @Mocked final RecordSchemaField field1
    ) throws RecordException 
    {
        new Expectations() {{
            baseSchema.getFields(); result = new RecordSchemaField[]{field1};
            field1.getName(); result = "field1";
            field1.getFieldType(); result = RecordSchemaFieldType.STRING;
        }};
        AdjustedRecordSchema schema = new AdjustedRecordSchema("schema", baseSchema, null, Arrays.asList("field2"));
        Map<String, Object> values = new HashMap<>();
        values.put("field1", "test");
        Record rec = schema.createRecord(values);
        assertNotNull(rec);
        assertEquals(schema, rec.getSchema());
        assertEquals("test", rec.getValue("field1"));
    }
    
    @Test
    public void adjustRecordTest(
            @Mocked final RecordSchema baseSchema,
            @Mocked final RecordSchemaField field1,
            @Mocked final RecordSchemaField field2
    ) throws Exception 
    {
        new Expectations() {{
            baseSchema.getFields(); result = new RecordSchemaField[]{field1, field2};
            field1.getName(); result = "field1";
            field2.getName(); result = "field2";
        }};
        AdjustedRecordSchema schema = new AdjustedRecordSchema("schema", baseSchema, null, null);
        RecordSchema adjustedSchema = schema.adjust("schema2", Arrays.asList("field1"));
        assertEquals("schema2", adjustedSchema.getName());
        assertArrayEquals(new RecordSchemaField[]{field1}, adjustedSchema.getFields());
        assertTrue(adjustedSchema.getFieldsMap().size()==1);
        assertEquals(field1, adjustedSchema.getFieldsMap().get("field1"));
        assertEquals(field1, adjustedSchema.getField("field1"));
    }
}

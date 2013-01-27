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

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.raven.ds.InvalidRecordFieldException;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.RecordValidationErrors;
import org.raven.test.RavenCoreTestCase;
import static org.easymock.EasyMock.*;

/**
 * @author Mikhail Titov
 */
public class RecordImplTest extends RavenCoreTestCase
{
    @Test(expected=RecordException.class)
    public void noFieldsInSchemaTest() throws Exception
    {
        RecordSchema schema = createMock(RecordSchema.class);
        expect(schema.getFields()).andReturn(null);
        expect(schema.getName()).andReturn("schema");
        replay(schema);

        try
        {
            RecordImpl record = new RecordImpl(schema);
        }
        finally
        {
            verify(schema);
        }
    }

    @Test(expected=InvalidRecordFieldException.class)
    public void setUndefinedFieldValue_test() throws Exception
    {
        RecordSchema schema = createMock(RecordSchema.class);
        RecordSchemaField field = createMock(RecordSchemaField.class);
        expect(schema.getFields()).andReturn(new RecordSchemaField[]{field});
        expect(field.getName()).andReturn("field");
        expect(schema.getName()).andReturn("schema");
        replay(schema, field);

        try
        {
            RecordImpl record = new RecordImpl(schema);
            record.setValue("field1", "val");
        }
        finally
        {
            verify(schema, field);
        }
    }

    @Test
    public void setFieldValue_test() throws Exception
    {
        RecordSchema schema = createMock(RecordSchema.class);
        RecordSchemaField field = createMock(RecordSchemaField.class);
        expect(schema.getFields()).andReturn(new RecordSchemaField[]{field});
        expect(field.getName()).andReturn("field1");
        expect(field.getFieldType()).andReturn(RecordSchemaFieldType.INTEGER);
        expect(field.getPattern()).andReturn(null);
        replay(schema, field);

        RecordImpl record = new RecordImpl(schema);
        record.setValue("field1", 1);

        assertEquals(1, record.getValue("field1"));
        
        record.setValue("field1", null);
        assertNull(record.getValue("field1"));
        
        verify(schema, field);
    }

    @Test
    public void setFieldValueWithValueConvertingTest() throws Exception
    {
        RecordSchema schema = createMock(RecordSchema.class);
        RecordSchemaField field = createMock(RecordSchemaField.class);
        expect(schema.getFields()).andReturn(new RecordSchemaField[]{field});
        expect(field.getName()).andReturn("field1");
        expect(field.getFieldType()).andReturn(RecordSchemaFieldType.INTEGER);
        expect(field.getPattern()).andReturn(null);
        replay(schema, field);

        RecordImpl record = new RecordImpl(schema);
        record.setValue("field1", "1");

        assertEquals(1, record.getValue("field1"));

        verify(schema, field);
    }

    @Test
    public void setFieldValueWithValueConvertingWithPatternTest() throws Exception
    {
        RecordSchema schema = createMock(RecordSchema.class);
        RecordSchemaField field = createMock(RecordSchemaField.class);
        expect(schema.getFields()).andReturn(new RecordSchemaField[]{field});
        expect(field.getName()).andReturn("field1");
        expect(field.getFieldType()).andReturn(RecordSchemaFieldType.DATE);
        expect(field.getPattern()).andReturn("dd.MM.yyyy");
        replay(schema, field);

        RecordImpl record = new RecordImpl(schema);
        record.setValue("field1", "01.01.2009");

        assertEquals(
                new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2009")
                , record.getValue("field1"));

        verify(schema, field);
    }

    @Test(expected=RecordException.class)
    public void setFieldValueWithConversionErrorTest() throws Exception
    {
        RecordSchema schema = createMock(RecordSchema.class);
        RecordSchemaField field = createMock(RecordSchemaField.class);
        expect(schema.getFields()).andReturn(new RecordSchemaField[]{field});
        expect(field.getName()).andReturn("field1");
        expect(field.getFieldType()).andReturn(RecordSchemaFieldType.INTEGER);
        expect(field.getPattern()).andReturn(null);
        replay(schema, field);

        try
        {
            RecordImpl record = new RecordImpl(schema);
            record.setValue("field1", "a1-");
        }
        finally
        {
            verify(schema, field);
        }
    }

    @Test
    public void copyFromTest() throws Exception
    {
        RecordSchema schema1 = createMock("schema1", RecordSchema.class);
        RecordSchema schema2 = createMock("schema2", RecordSchema.class);
        RecordSchemaField field = createMock("field1_schema1", RecordSchemaField.class);
        expect(schema1.getFields()).andReturn(new RecordSchemaField[]{field});
        expect(field.getName()).andReturn("field1");
        expect(field.getFieldType()).andReturn(RecordSchemaFieldType.INTEGER);
        expect(field.getPattern()).andReturn(null);

        RecordSchemaField field2 = createMock("field1_schema2", RecordSchemaField.class);
        expect(field2.getName()).andReturn("field1");
        expect(field2.getFieldType()).andReturn(RecordSchemaFieldType.INTEGER);
        expect(field2.getPattern()).andReturn(null);

        RecordSchemaField field3 = createMock("field2_schema2", RecordSchemaField.class);
        expect(field3.getName()).andReturn("field2");

        expect(schema2.getFields()).andReturn(new RecordSchemaField[]{field2, field3});

        replay(schema1, field, schema2, field2, field3);

        Record rec1 = new RecordImpl(schema1);
        rec1.setValue("field1", 10);
        Record rec2 = new RecordImpl(schema2);
        rec2.copyFrom(rec1);

        assertEquals(new Integer(10), rec2.getValue("field1"));
        assertNull(rec2.getValue("field2"));
        
        verify(schema1, field, schema2, field2, field3);
    }

    @Test
    public void tag_test() throws RecordException
    {
        RecordSchema schema = createMock(RecordSchema.class);
        RecordSchemaField field = createMock(RecordSchemaField.class);
        expect(schema.getFields()).andReturn(new RecordSchemaField[]{field});
        expect(field.getName()).andReturn("field");
//        expect(schema.getName()).andReturn("schema");
        replay(schema, field);

        Record rec = new RecordImpl(schema);
        assertFalse(rec.containsTag("tag1"));
        assertNull(rec.getTag("tag1"));
        assertSame(Collections.EMPTY_MAP, rec.getTags());
        assertFalse(rec.getTags().containsKey("tag1"));

        rec.setTag("tag1", "value1");
        rec.setTag("tag2", "value2");
        assertEquals(2, rec.getTags().size());
        assertEquals("value1", rec.getTags().get("tag1"));
        assertEquals("value2", rec.getTags().get("tag2"));
        assertTrue(rec.containsTag("tag1"));
        assertEquals("value1", rec.getTag("tag1"));
        assertTrue(rec.containsTag("tag2"));
        assertEquals("value2", rec.getTag("tag2"));

        rec.removeTag("tag1");
        assertFalse(rec.containsTag("tag1"));
        assertNull(rec.getTag("tag1"));
        assertTrue(rec.containsTag("tag2"));
        assertEquals("value2", rec.getTag("tag2"));

        rec.removeTag("tag2");
        assertFalse(rec.containsTag("tag2"));
        assertNull(rec.getTag("tag2"));
        assertSame(Collections.EMPTY_MAP, rec.getTags());
        
        verify(schema, field);
    }
    
    @Test
    public void getValuesTest() throws Exception {
        RecordSchemaNode schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        RecordSchemaFieldNode.create(schema, "field1", null, RecordSchemaFieldType.INTEGER, null);
        RecordSchemaFieldNode.create(schema, "field2", null, RecordSchemaFieldType.INTEGER, null);
        Record rec = schema.createRecord();
        rec.setValue("field1", 10);
        Map<String, Object> values = rec.getValues();
        assertNotNull(values);
        assertEquals(2, values.size());
        assertEquals(10, values.get("field1"));
        assertNull(values.get("field2"));
    }

    @Test
    public void validateTest() throws RecordException
    {
        RecordSchemaNode schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        RecordSchemaFieldNode field = RecordSchemaFieldNode.create(
                schema, "field", null, RecordSchemaFieldType.INTEGER, null);
        CustomValueValidatorNode validator = new CustomValueValidatorNode();
        validator.setName("validator");
        field.addAndSaveChildren(validator);
        validator.setValidateExpression("value?null:'empty'");
        assertTrue(validator.start());

        Record record = schema.createRecord();

        RecordValidationErrors errors = record.validate();
        assertNotNull(errors);
        assertEquals("Record of schema (schema) has validation errors: \nfield:\n  empty\n"
                , errors.toText());
    }
}
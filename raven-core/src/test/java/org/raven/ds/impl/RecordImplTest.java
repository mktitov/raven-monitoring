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
import org.raven.ds.InvalidRecordFieldException;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import static org.easymock.EasyMock.*;

/**
 * @author Mikhail Titov
 */
public class RecordImplTest extends RavenCoreTestCase
{
    @Test(expected=InvalidRecordFieldException.class)
    public void setUndefinedFieldValue_test() throws Exception
    {
        RecordSchema schema = createMock(RecordSchema.class);
        expect(schema.getField("field1")).andReturn(null);
        expect(schema.getName()).andReturn("schema");
        replay(schema);

        try
        {
            RecordImpl record = new RecordImpl(schema);
            record.setValue("field1", "val");
        }
        finally
        {
            verify(schema);
        }
    }

    @Test
    public void setFieldValue_test() throws Exception
    {
        RecordSchema schema = createMock(RecordSchema.class);
        RecordSchemaField field = createMock(RecordSchemaField.class);
        expect(schema.getField("field1")).andReturn(field).times(2);
        expect(field.getFieldType()).andReturn(RecordSchemaFieldType.INTEGER);
        replay(schema, field);

        RecordImpl record = new RecordImpl(schema);
        record.setValue("field1", 1);

        assertEquals(1, record.getValue("field1"));
        
        verify(schema, field);
    }

    @Test
    public void setFieldValueWithValueConvertingTest() throws Exception
    {
        RecordSchema schema = createMock(RecordSchema.class);
        RecordSchemaField field = createMock(RecordSchemaField.class);
        expect(schema.getField("field1")).andReturn(field).times(2);
        expect(field.getFieldType()).andReturn(RecordSchemaFieldType.INTEGER);
        replay(schema, field);

        RecordImpl record = new RecordImpl(schema);
        record.setValue("field1", "1");

        assertEquals(1, record.getValue("field1"));

        verify(schema, field);
    }

    @Test(expected=RecordException.class)
    public void setFieldValueWithConversionErrorTest() throws Exception
    {
        RecordSchema schema = createMock(RecordSchema.class);
        RecordSchemaField field = createMock(RecordSchemaField.class);
        expect(schema.getField("field1")).andReturn(field);
        expect(field.getFieldType()).andReturn(RecordSchemaFieldType.INTEGER);
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
}
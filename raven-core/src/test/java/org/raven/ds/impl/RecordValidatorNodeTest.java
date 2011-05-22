/*
 *  Copyright 2011 Mikhail Titov.
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

import org.junit.Before;
import org.junit.Test;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class RecordValidatorNodeTest extends RavenCoreTestCase
{
    private RecordValidatorNode recordValidator;
    private PushDataSource ds;
    private DataCollector collector;
    private RecordSchemaNode schema;

    @Before
    public void prepare()
    {
        schema = new RecordSchemaNode();
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

        ds = new PushDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        recordValidator = new RecordValidatorNode();
        recordValidator.setName("recordValidator");
        tree.getRootNode().addAndSaveChildren(recordValidator);
        recordValidator.setDataSource(ds);
        assertTrue(recordValidator.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(recordValidator);
        assertTrue(collector.start());
    }

    @Test
    public void dataNotRecordTest()
    {
        ds.pushData("test");
        assertEquals(1, collector.getDataListSize());
        assertEquals("test", collector.getDataList().get(0));
    }

    @Test
    public void nullDataTest()
    {
        ds.pushData(null);
        assertEquals(1, collector.getDataListSize());
        assertNull(collector.getDataList().get(0));
    }

    @Test
    public void noErrorsTest() throws Exception
    {
        Record record = schema.createRecord();
        record.setValue("field", 1);

        ds.pushData(record);
        assertEquals(1, collector.getDataListSize());
        assertSame(record, collector.getDataList().get(0));
    }

    @Test
    public void validationErrorTest() throws Exception
    {
        Record record = schema.createRecord();

        ds.pushData(record);
        assertEquals(0, collector.getDataListSize());
    }

    @Test
    public void onRecordValidationErrorTest() throws Exception
    {
        recordValidator.setUseOnRecordValidationError(Boolean.TRUE);
        recordValidator.setOnRecordValidationError("'error'");
        Record record = schema.createRecord();

        ds.pushData(record);
        assertEquals(1, collector.getDataListSize());
        assertEquals("error", collector.getDataList().get(0));
    }

    @Test
    public void onRecordValidationErrorTest2() throws Exception
    {
        recordValidator.setUseOnRecordValidationError(Boolean.TRUE);
        recordValidator.setOnRecordValidationError("null");
        Record record = schema.createRecord();

        ds.pushData(record);
        assertEquals(0, collector.getDataListSize());
    }
}
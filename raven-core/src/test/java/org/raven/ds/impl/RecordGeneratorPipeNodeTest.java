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
import org.raven.PushOnDemandDataSource;
import org.raven.RavenCoreTestCase;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaFieldType;

/**
 *
 * @author Mikhail Titov
 */
public class RecordGeneratorPipeNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws RecordException
    {
        RecordSchemaNode schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
        field.setName("field");
        schema.addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.STRING);
        assertTrue(field.start());

        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        RecordGeneratorPipeNode pipe = new RecordGeneratorPipeNode();
        pipe.setName("pipe");
        tree.getRootNode().addAndSaveChildren(pipe);
        pipe.setDataSource(ds);
        pipe.setRecordSchema(schema);
        pipe.setUseExpression(true);
        pipe.setExpression("record['field']=data+' value'");
        assertTrue(pipe.start());

        SafeDataConsumer consumer = new SafeDataConsumer();
        consumer.setName("consumer");
        tree.getRootNode().addAndSaveChildren(consumer);
        consumer.setDataSource(pipe);
        assertTrue(consumer.start());

        //testing
        ds.addDataPortion("test");
        Object val = consumer.refereshData(null);
        assertNotNull(val);
        assertTrue(val instanceof Record);
        Record record = (Record) val;
        assertEquals("test value", record.getValue("field"));
    }
}
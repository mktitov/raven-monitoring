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

import org.junit.Before;
import org.junit.Test;
import org.raven.PushDataSource;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.test.DataCollector;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class RecordsAggregatorNodeTest extends RavenCoreTestCase
{
    PushDataSource ds;
    RecordSchemaNode schema;
    RecordsAggregatorNode aggregator;
    DataCollector collector;

    @Before
    public void prepare() throws Exception
    {
        schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        ds = new PushDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        schema.createField("grpField1", RecordSchemaFieldType.STRING, null);
        schema.createField("grpField2", RecordSchemaFieldType.INTEGER, null);
        schema.createField("value1", RecordSchemaFieldType.INTEGER, null);
        schema.createField("value2", RecordSchemaFieldType.DOUBLE, null);

        aggregator = new RecordsAggregatorNode();
        aggregator.setName("aggregator");
        tree.getRootNode().addAndSaveChildren(aggregator);
        aggregator.setDataSource(ds);
        aggregator.setRecordSchema(schema);

        createGroupField("grpField1", null);
        createGroupField("grpField2", "data['grpField2']");



    }

    @Test
    public void test()
    {
        
    }


    private void createGroupField(String fieldName, String fieldExpression) throws Exception
    {
        RecordsAggregatorGroupFieldNode groupField = new RecordsAggregatorGroupFieldNode();
        groupField.setName(fieldName);
        aggregator.addAndSaveChildren(groupField);
        if (fieldExpression!=null)
        {
            groupField.setUseFieldValueExpression(Boolean.TRUE);
            groupField.getNodeAttribute(RecordsAggregatorField.FIELD_VALUE_EXPRESSION_ATTR)
                    .setValue(fieldExpression);
        } else
            groupField.setFieldName(fieldName);
    }
}
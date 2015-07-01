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

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.PushDataSource;
import org.raven.ds.AggregateFunctionType;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
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
    RecordsAggregatorValueFieldNode value2FieldNode;

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
        assertTrue(aggregator.start());

        createGroupField("grpField1", null);
        createGroupField("grpField2", "record['grpField2']");

        createValueField("value1", null, AggregateFunctionType.SUM, null);
        value2FieldNode = createValueField("value2", "record['value1']+record['value2']"
                , AggregateFunctionType.CUSTOM, "(value?:0)+nextValue");

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(aggregator);
        assertTrue(collector.start());
    }

    @Test
    public void test() throws RecordException
    {
        ds.pushData(createRecord("g1", 1, 1, 10.));
        ds.pushData(createRecord("g1", 1, 2, 20.));
        ds.pushData(createRecord("g2", 2, 2, 20.));
        ds.pushData(null);

        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(3, data.size());
        assertNull(data.get(2));

        assertTrue(data.get(0) instanceof Record);
        assertTrue(data.get(1) instanceof Record);

        boolean foundG1 = false;
        boolean foundG2 = false;
        for (int i=0; i<2; ++i)
        {
            Record rec = (Record) data.get(i);
            if (rec.getValue("grpField1").equals("g1"))
            {
                foundG1 = true;
                assertEquals(new Integer(1), rec.getValue("grpField2"));
                assertEquals(new Integer(3), rec.getValue("value1"));
                assertEquals(new Double(33.), rec.getValue("value2"));
            } else if (rec.getValue("grpField1").equals("g2"))
            {
                foundG2 = true;
                assertEquals(new Integer(2), rec.getValue("grpField2"));
                assertEquals(new Integer(2), rec.getValue("value1"));
                assertEquals(new Double(22.), rec.getValue("value2"));
                
            }
        }
        assertTrue(foundG1);
        assertTrue(foundG2);
    }

    @Test
    public void sortedRecordsTest() throws RecordException
    {
        aggregator.setIsRecordsSorted(Boolean.TRUE);
        ds.pushData(createRecord("g1", 1, 1, 10.));
        ds.pushData(createRecord("g1", 1, 2, 20.));

        ds.pushData(createRecord("g2", 2, 2, 20.));

        assertEquals(1, collector.getDataListSize());
        Record rec = (Record) collector.getDataList().get(0);
        assertEquals(new Integer(3), rec.getValue("value1"));
        assertEquals(new Double(33.), rec.getValue("value2"));

        collector.getDataList().clear();
        ds.pushData(null);
        assertEquals(2, collector.getDataListSize());
        rec = (Record) collector.getDataList().get(0);
        assertEquals(new Integer(2), rec.getValue("value1"));
        assertEquals(new Double(22.), rec.getValue("value2"));
        assertNull(collector.getDataList().get(1));
    }

    @Test
    public void testCustom() throws RecordException
    {
        value2FieldNode.setUseStartAggregationExpression(Boolean.TRUE);
        value2FieldNode.setUseFinishAggregationExpression(Boolean.TRUE);
        value2FieldNode.setStartAggregationExpression("params.v=100;1");
        value2FieldNode.setFinishAggregationExpression("params.v+value");
        ds.pushData(createRecord("g1", 1, 1, 10.));
        ds.pushData(createRecord("g1", 1, 2, 20.));
        ds.pushData(createRecord("g2", 2, 2, 20.));
        ds.pushData(null);

        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(3, data.size());
        assertNull(data.get(2));

        assertTrue(data.get(0) instanceof Record);
        assertTrue(data.get(1) instanceof Record);

        boolean foundG1 = false;
        boolean foundG2 = false;
        for (int i=0; i<2; ++i)
        {
            Record rec = (Record) data.get(i);
            if (rec.getValue("grpField1").equals("g1"))
            {
                foundG1 = true;
                assertEquals(new Integer(1), rec.getValue("grpField2"));
                assertEquals(new Integer(3), rec.getValue("value1"));
                assertEquals(new Double(134.), rec.getValue("value2"));
            } else if (rec.getValue("grpField1").equals("g2"))
            {
                foundG2 = true;
                assertEquals(new Integer(2), rec.getValue("grpField2"));
                assertEquals(new Integer(2), rec.getValue("value1"));
                assertEquals(new Double(123.), rec.getValue("value2"));

            }
        }
        assertTrue(foundG1);
        assertTrue(foundG2);
    }

    @Test
    public void avgFuncTest() throws RecordException, Exception
    {
        testFunction(AggregateFunctionType.AVG, new Integer[]{10, 2}, 6);
    }

    @Test
    public void minFuncTest() throws RecordException, Exception
    {
        testFunction(AggregateFunctionType.MIN, new Integer[]{10, 2}, 2);
    }

    @Test
    public void maxFuncTest() throws RecordException, Exception
    {
        testFunction(AggregateFunctionType.MAX, new Integer[]{10, 2}, 10);
    }

    @Test
    public void resetOldValuesTest() throws Exception
    {
        testFunction(AggregateFunctionType.MAX, new Integer[]{10, 2}, 10);
        collector.getDataList().clear();
        testFunction(AggregateFunctionType.MAX, new Integer[]{1}, 1);
    }

    private void testFunction(AggregateFunctionType func, Integer[] values, Integer res)
            throws Exception
    {
        RecordsAggregatorValueFieldNode valueField =
                (RecordsAggregatorValueFieldNode) aggregator.getChildren("value1");
        valueField.setAggregateFunction(func);

        for (Integer value: values)
            ds.pushData(createRecord("g1", 1, value, 10.));
        ds.pushData(null);

        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(2, data.size());
        assertNull(data.get(1));
        assertTrue(data.get(0) instanceof Record);
        Record rec = (Record) data.get(0);
        assertEquals(res, rec.getValue("value1"));
    }

    private Record createRecord(String grpField1, Integer grpField2, Integer value1, Double value2)
            throws RecordException
    {
        Record rec = schema.createRecord();
        rec.setValue("grpField1", grpField1);
        rec.setValue("grpField2", grpField2);
        rec.setValue("value1", value1);
        rec.setValue("value2", value2);

        return rec;
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
        assertTrue(groupField.start());
    }

    private RecordsAggregatorValueFieldNode createValueField(
            String fieldName, String fieldExpression, AggregateFunctionType aggType
            , String aggregationExpression)
        throws Exception
    {
        RecordsAggregatorValueFieldNode valueField = new RecordsAggregatorValueFieldNode();
        valueField.setName(fieldName);
        aggregator.addAndSaveChildren(valueField);
        if (fieldExpression!=null)
        {
            valueField.setUseFieldValueExpression(Boolean.TRUE);
            valueField.getNodeAttribute(RecordsAggregatorField.FIELD_VALUE_EXPRESSION_ATTR)
                    .setValue(fieldExpression);
        } else
            valueField.setFieldName(fieldName);
        valueField.setAggregateFunction(aggType);
        valueField.setAggregationExpression(aggregationExpression);
        assertTrue(valueField.start());

        return valueField;
    }
}
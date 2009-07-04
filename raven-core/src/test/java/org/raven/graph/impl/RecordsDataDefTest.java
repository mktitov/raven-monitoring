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

package org.raven.graph.impl;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.graph.GraphData;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class RecordsDataDefTest extends RavenCoreTestCase
{
    RecordSchemaNode schema;
    PushOnDemandDataSource ds;
    RecordsDataDef dataDef;

    @Before
    public void prepare()
    {
        ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        RecordSchemaFieldNode tsField = new RecordSchemaFieldNode();
        tsField.setName("ts");
        schema.addAndSaveChildren(tsField);
        tsField.setFieldType(RecordSchemaFieldType.TIMESTAMP);
        tsField.setPattern("dd.MM.yyyy HH:mm");
        assertTrue(tsField.start());

        RecordSchemaFieldNode valueField = new RecordSchemaFieldNode();
        valueField.setName("value");
        schema.addAndSaveChildren(valueField);
        valueField.setFieldType(RecordSchemaFieldType.LONG);
        assertTrue(valueField.start());

        dataDef = new RecordsDataDef();
        dataDef.setName("dataDef");
        tree.getRootNode().addAndSaveChildren(dataDef);
        dataDef.setDataSource(ds);
        dataDef.setRecordSchema(schema);
        dataDef.setTimestampFieldName("ts");
        dataDef.setValueFieldName("value");
        assertTrue(dataDef.start());

    }

    @Test
    public void test() throws Exception
    {
        Timestamp ts1 = new Timestamp(System.currentTimeMillis()*1000l);
        Timestamp ts2 = new Timestamp(ts1.getTime()+5000);
        Timestamp ts3 = new Timestamp(ts1.getTime()+400);

        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String tsStr = String.format(
                "[%s, %s]", fmt.format(new Date(0)), fmt.format(new Date(10000)));

        Record rec = schema.createRecord();
        rec.setValue("ts", ts1);
        rec.setValue("value", 1l);
        ds.addDataPortion(rec);

        //this record will be skipped by the RecordsDataDef because of ts3 rounded to seconds
        //equals to ts
        rec = schema.createRecord();
        rec.setValue("ts", ts3);
        rec.setValue("value", 10l);
        ds.addDataPortion(rec);

        rec = schema.createRecord();
        rec.setValue("ts", ts2);
        rec.setValue("value", 2l);
        ds.addDataPortion(rec);

        GraphData data = dataDef.getData(0l, 10l);
        assertEquals(1., data.getPlottable().getValue(ts1.getTime()/1000), 0.);
        assertEquals(2., data.getPlottable().getValue(ts2.getTime()/1000), 0.);

        Map<String, NodeAttribute> sessAttrs = ds.getLastSessionAttributes();
        assertNotNull(sessAttrs);
        NodeAttribute tsAttr = sessAttrs.get("ts");
        assertNotNull(tsAttr);
        assertEquals(String.class, tsAttr.getType());
        assertEquals(tsStr, tsAttr.getValue());
    }

    @Test
    public void filterTest() throws Exception
    {
        dataDef.setUseRecordFilter(true);
        NodeAttribute filterAttr = dataDef.getNodeAttribute("recordFilter");
        filterAttr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        filterAttr.setValue("record['value']<2");

        Timestamp ts1 = new Timestamp(System.currentTimeMillis()*1000l);
        Timestamp ts2 = new Timestamp(ts1.getTime()+5000);
        Timestamp ts3 = new Timestamp(ts2.getTime()+5000);

        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String tsStr = String.format(
                "[%s, %s]", fmt.format(new Date(0)), fmt.format(new Date(10000)));

        Record rec = schema.createRecord();
        rec.setValue("ts", ts1);
        rec.setValue("value", 1l);
        ds.addDataPortion(rec);

        rec = schema.createRecord();
        rec.setValue("ts", ts2);
        rec.setValue("value", 2l);
        ds.addDataPortion(rec);

        rec = schema.createRecord();
        rec.setValue("ts", ts3);
        rec.setValue("value", 1l);
        ds.addDataPortion(rec);

        GraphData data = dataDef.getData(0l, 10l);
        assertEquals(1., data.getPlottable().getValue(ts1.getTime()/1000), 0.);
        assertEquals(1., data.getPlottable().getValue(ts2.getTime()/1000), 0.);
        assertEquals(1., data.getPlottable().getValue(ts3.getTime()/1000), 0.);
    }
}
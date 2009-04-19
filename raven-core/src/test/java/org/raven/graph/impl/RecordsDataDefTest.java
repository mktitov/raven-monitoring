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
import java.util.concurrent.TimeUnit;
import org.jrobin.data.Plottable;
import org.junit.Test;
import org.raven.PushOnDemandDataSource;
import org.raven.RavenCoreTestCase;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class RecordsDataDefTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        RecordSchemaNode schema = new RecordSchemaNode();
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

        RecordsDataDef dataDef = new RecordsDataDef();
        dataDef.setName("dataDef");
        tree.getRootNode().addAndSaveChildren(dataDef);
        dataDef.setDataSource(ds);
        dataDef.setRecordSchema(schema);
        dataDef.setTimestampFieldName("ts");
        dataDef.setValueFieldName("value");
        assertTrue(dataDef.start());

        Timestamp ts1 = new Timestamp(System.currentTimeMillis());
//        TimeUnit.MILLISECONDS.sleep(100);
        Timestamp ts2 = new Timestamp(ts1.getTime()+5000);
        Timestamp ts3 = new Timestamp(ts1.getTime()+500);

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

        Plottable data = dataDef.getData(0, 10);
        assertEquals(1., data.getValue(ts1.getTime()/1000), 0.);
        assertEquals(2., data.getValue(ts2.getTime()/1000), 0.);

        Map<String, NodeAttribute> sessAttrs = ds.getLastSessionAttributes();
        assertNotNull(sessAttrs);
        NodeAttribute tsAttr = sessAttrs.get("ts");
        assertNotNull(tsAttr);
        assertEquals(String.class, tsAttr.getType());
        assertEquals(tsStr, tsAttr.getValue());
    }
}
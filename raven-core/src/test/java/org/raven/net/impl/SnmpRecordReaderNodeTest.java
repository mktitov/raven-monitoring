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

package org.raven.net.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.IdRecordFieldExtension;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.test.DataCollector;
import org.raven.test.RavenCoreTestCase;
import static org.raven.net.impl.SnmpRecordReaderNode.*;
/**
 *
 * @author Mikhail Titov
 */
@Ignore
public class SnmpRecordReaderNodeTest extends RavenCoreTestCase
{
    private SnmpRecordReaderNode snmpReader;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        snmpReader = new SnmpRecordReaderNode();
        snmpReader.setName("snmpReader");
        tree.getRootNode().addAndSaveChildren(snmpReader);
        assertTrue(snmpReader.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(snmpReader);
    }

    @Test(timeout=10000)
    public void readSimpleValueTest() throws Exception
    {
        RecordSchemaNode schema = createSimpleSchema();
        collector.getNodeAttribute(HOST_ATTR).setValue("10.50.1.16");
        collector.getNodeAttribute(RECORD_SCHEMA_ATTR).setValue(schema.getPath());
        collector.getNodeAttribute(INTERVAL_ATTRIBUTE).setValue("100");
        collector.getNodeAttribute(INTERVAL_UNIT_ATTRIBUTE).setValue(TimeUnit.DAYS.toString());
        assertTrue(collector.start());

        Thread.sleep(9000);
        List data = collector.getDataList();
        assertNotNull(data);
        assertTrue(data.size()>=2);
        assertNull(data.get(1));

        assertTrue(data.get(0) instanceof Record);
        Record rec = (Record) data.get(0);
        assertEquals(
                "HP ETHERNET MULTI-ENVIRONMENT,ROM none,JETDIRECT,JD128,EEPROM " +
                "V.28.59,CIDATE 05/10/2005"
                , rec.getValue("sysDescr"));
        assertEquals("kompr00205", rec.getValue("sysName"));
    }

//    @Test(timeout=10000)
    public void readAllRecordsFromTableTest() throws Exception
    {
        RecordSchemaNode schema = createTableSchema();
        collector.getNodeAttribute(HOST_ATTR).setValue("10.50.1.16");
        collector.getNodeAttribute(RECORD_SCHEMA_ATTR).setValue(schema.getPath());
        collector.getNodeAttribute(OID_TYPE_ATTR).setValue(OidType.TABLE.toString());
        collector.getNodeAttribute(INTERVAL_ATTRIBUTE).setValue("100");
        collector.getNodeAttribute(INTERVAL_UNIT_ATTRIBUTE).setValue(TimeUnit.DAYS.toString());
        assertTrue(collector.start());

        Thread.sleep(9000);
        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(3, data.size());
        assertNull(data.get(2));

        assertTrue(data.get(0) instanceof Record);
        Record rec = (Record) data.get(0);
        assertEquals("1", rec.getValue("ifIndex"));
        assertEquals("HP ETHERNET MULTI-ENVIRONMENT,ROM none,JETDIRECT,JD128,EEPROM V.28.59"
                , rec.getValue("ifDescr"));
        rec = (Record) data.get(1);
        assertEquals("2", rec.getValue("ifIndex"));
        assertEquals("HP ETHERNET MULTI-ENVIRONMENT,ROM none,JETDIRECT,JD128,EEPROM V.28.59"
                , rec.getValue("ifDescr"));
    }

//    @Test(timeout=10000)
    public void readAllRecordsFromTableWithIndexTest() throws Exception
    {
        RecordSchemaNode schema = createTableSchemaWithId();
        collector.getNodeAttribute(HOST_ATTR).setValue("10.50.1.16");
        collector.getNodeAttribute(RECORD_SCHEMA_ATTR).setValue(schema.getPath());
        collector.getNodeAttribute(OID_TYPE_ATTR).setValue(OidType.TABLE.toString());
        collector.getNodeAttribute(INTERVAL_ATTRIBUTE).setValue("100");
        collector.getNodeAttribute(INTERVAL_UNIT_ATTRIBUTE).setValue(TimeUnit.DAYS.toString());
        assertTrue(collector.start());

        Thread.sleep(9000);
        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(3, data.size());
        assertNull(data.get(2));

        assertTrue(data.get(0) instanceof Record);
        Record rec = (Record) data.get(0);
        assertEquals("1", rec.getValue("id"));
        assertEquals("1", rec.getValue("ifIndex"));
        assertEquals("HP ETHERNET MULTI-ENVIRONMENT,ROM none,JETDIRECT,JD128,EEPROM V.28.59"
                , rec.getValue("ifDescr"));
        rec = (Record) data.get(1);
        assertEquals("2", rec.getValue("id"));
        assertEquals("2", rec.getValue("ifIndex"));
        assertEquals("HP ETHERNET MULTI-ENVIRONMENT,ROM none,JETDIRECT,JD128,EEPROM V.28.59"
                , rec.getValue("ifDescr"));
    }

    @Test
    public void readRecordsByIds() throws Exception
    {
        RecordSchemaNode schema = createTableSchema();
        collector.getNodeAttribute(HOST_ATTR).setValue("10.50.1.16");
        collector.getNodeAttribute(RECORD_SCHEMA_ATTR).setValue(schema.getPath());
        collector.getNodeAttribute(OID_TYPE_ATTR).setValue(OidType.TABLE.toString());
        collector.getNodeAttribute(INTERVAL_ATTRIBUTE).setValue("100");
        collector.getNodeAttribute(INTERVAL_UNIT_ATTRIBUTE).setValue(TimeUnit.DAYS.toString());
        collector.getNodeAttribute(ROW_IDS_ATTR).setValue("1");
        assertTrue(collector.start());

        Thread.sleep(9000);
        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(2, data.size());
        assertNull(data.get(1));

        assertTrue(data.get(0) instanceof Record);
        Record rec = (Record) data.get(0);
        assertEquals("1", rec.getValue("ifIndex"));
        assertEquals("HP ETHERNET MULTI-ENVIRONMENT,ROM none,JETDIRECT,JD128,EEPROM V.28.59"
                , rec.getValue("ifDescr"));
    }

    private RecordSchemaNode createSimpleSchema()
    {
        RecordSchemaNode schema = new RecordSchemaNode();
        schema.setName("schema1");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        addRecordSchemaField(schema, "sysDescr", RecordSchemaFieldType.STRING, ".1.3.6.1.2.1.1.1");
        addRecordSchemaField(schema, "sysName", RecordSchemaFieldType.STRING, ".1.3.6.1.2.1.1.5");

        return schema;
    }

    private void addRecordSchemaField(
            RecordSchemaNode schema, String name, RecordSchemaFieldType type, String oid)
    {
        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
        field.setName(name);
        schema.addAndSaveChildren(field);
        field.setFieldType(type);
        assertTrue(field.start());

        SnmpRecordFieldExtension ext = new SnmpRecordFieldExtension();
        ext.setName("oid");
        field.addAndSaveChildren(ext);
        ext.setOid(oid);
        assertTrue(ext.start());
    }

    private RecordSchemaNode createTableSchema()
    {
        RecordSchemaNode schema = new RecordSchemaNode();
        schema.setName("schema1");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        SnmpRecordExtension ext = new SnmpRecordExtension();
        ext.setName("table oid");
        schema.getRecordExtensionsNode().addAndSaveChildren(ext);
        ext.setOid("1.3.6.1.2.1.2.2");
        assertTrue(ext.start());

        addRecordSchemaField(schema, "ifIndex", RecordSchemaFieldType.STRING, ".1.3.6.1.2.1.2.2.1.1");
        addRecordSchemaField(schema, "ifDescr", RecordSchemaFieldType.STRING, ".1.3.6.1.2.1.2.2.1.2");

        return schema;
    }

    private RecordSchemaNode createTableSchemaWithId()
    {
        RecordSchemaNode schema = createTableSchema();
        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
        field.setName("id");
        schema.addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.STRING);
        assertTrue(field.start());

        IdRecordFieldExtension idExt = new IdRecordFieldExtension();
        idExt.setName("id");
        field.addAndSaveChildren(idExt);
        assertTrue(idExt.start());

        return schema;
    }
}
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
import org.raven.PushDataSource;
import org.raven.RavenUtils;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.log.LogLevel;
import org.raven.table.Table;
import org.raven.test.DataCollector;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class RecordsAsCrossTableNodeTest extends RavenCoreTestCase
{
    private PushDataSource ds;
    private DataCollector collector;
    private RecordsAsCrossTableNode crossTable;
    private RecordSchemaNode schema;

    @Before
    public void prepare()
    {
        schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());
        schema.createField("M1", RecordSchemaFieldType.STRING, null);
        schema.createField("M2", RecordSchemaFieldType.STRING, null);
        schema.createField("S1", RecordSchemaFieldType.STRING, null);
        schema.createField("S2", RecordSchemaFieldType.STRING, null);
        schema.createField("V1", RecordSchemaFieldType.STRING, null);
        schema.createField("V2", RecordSchemaFieldType.STRING, null);

        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        crossTable = new RecordsAsCrossTableNode();
        crossTable.setName("crossTable");
        tree.getRootNode().addAndSaveChildren(crossTable);
        crossTable.setDataSource(ds);
        crossTable.setLogLevel(LogLevel.DEBUG);
        
        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(crossTable);
        assertTrue(collector.start());
    }

    @Test
    public void simpleTest() throws RecordException
    {
        crossTable.setMasterFields("M1");
        crossTable.setSecondaryFields("S1");
        crossTable.setCellValueFields("V1");
        crossTable.setFirstColumnName("c0");
        assertTrue(crossTable.start());

        ds.pushData(createRecord("c1", null, "r1", null, "v1 1", null));
        ds.pushData(createRecord("c1", null, "r2", null, "v1 2", null));
        ds.pushData(createRecord("c2", null, "r1", null, "v2 1", null));
        ds.pushData(createRecord("c2", null, "r2", null, "v2 2", null));
        ds.pushData(null);

        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Table);

        Table table = (Table) data.get(0);
        assertArrayEquals(new String[]{"c0", "c1", "c2"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{"r1", "v1 1", "v2 1"}, rows.get(0));
        assertArrayEquals(new Object[]{"r2", "v1 2", "v2 2"}, rows.get(1));
    }

    @Test
    public void simpleTest2() throws RecordException
    {
        crossTable.setMasterFields("M1");
        crossTable.setSecondaryFields("S1");
        crossTable.setCellValueFields("V1");
        crossTable.setFirstColumnName("c0");
        assertTrue(crossTable.start());

        ds.pushData(createRecord("c1", null, "r1", null, "v1 1", null));
        ds.pushData(createRecord("c2", null, "r1", null, "v2 1", null));
        ds.pushData(createRecord("c2", null, "r2", null, "v2 2", null));
        ds.pushData(createRecord("c3", null, "r1", null, "v3 1", null));
        ds.pushData(null);

        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Table);

        Table table = (Table) data.get(0);
        assertArrayEquals(new String[]{"c0", "c1", "c2", "c3"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{"r1", "v1 1", "v2 1", "v3 1"}, rows.get(0));
        assertArrayEquals(new Object[]{"r2", null, "v2 2", null}, rows.get(1));
    }

    @Test
    public void manyFieldsTest() throws RecordException
    {
        crossTable.setMasterFields("M1, M2");
        crossTable.setSecondaryFields("S1, S2");
        crossTable.setCellValueFields("V1, V2");
        crossTable.setFirstColumnName("c0");
        assertTrue(crossTable.start());

        ds.pushData(createRecord("c1", "1", "r1", "1", "v1 1", "v"));
        ds.pushData(createRecord("c1", "1", "r2", "2", "v1 2", "v"));
        ds.pushData(createRecord("c1", "2", "r2", "2", "v2 2", "v"));
        ds.pushData(createRecord("c2", "2", "r1", "1", "v3 1", "v"));
        ds.pushData(createRecord("c2", "2", "r2", "2", "v3 2", "v"));
        ds.pushData(null);

        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Table);

        Table table = (Table) data.get(0);
        assertArrayEquals(new String[]{"c0", "c1, 1", "c1, 2","c2, 2"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{"r1, 1", "v1 1, v", null, "v3 1, v"}, rows.get(0));
        assertArrayEquals(new Object[]{"r2, 2", "v1 2, v", "v2 2, v", "v3 2, v"}, rows.get(1));
    }

    @Test
    public void expressionsTest() throws Exception
    {
        crossTable.setMasterFields("M1");
        crossTable.setMasterFieldsExpression("record['M1']+'_'");
        crossTable.setUseMasterFieldsExpression(Boolean.TRUE);
        crossTable.setSecondaryFields("S1");
        crossTable.setSecondaryFieldsExpression("record['S1']+'_'");
        crossTable.setUseSecondaryFieldsExpression(Boolean.TRUE);
        crossTable.setCellValueFields("V1");
        crossTable.setCellValueFieldsExpression("record['V1']+'_'");
        crossTable.setUseCellValueFieldsExpression(Boolean.TRUE);
        crossTable.setFirstColumnName("c0");
        assertTrue(crossTable.start());

        ds.pushData(createRecord("c1", null, "r1", null, "v1 1", null));
        ds.pushData(createRecord("c1", null, "r2", null, "v1 2", null));
        ds.pushData(createRecord("c2", null, "r1", null, "v2 1", null));
        ds.pushData(createRecord("c2", null, "r2", null, "v2 2", null));
        ds.pushData(null);

        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Table);

        Table table = (Table) data.get(0);
        assertArrayEquals(new String[]{"c0", "c1_", "c2_"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{"r1_", "v1 1_", "v2 1_"}, rows.get(0));
        assertArrayEquals(new Object[]{"r2_", "v1 2_", "v2 2_"}, rows.get(1));
    }

    private Record createRecord(String m1, String m2, String s1, String s2, String v1, String v2)
            throws RecordException
    {
        Record rec = schema.createRecord();
        rec.setValue("M1", m1);
        rec.setValue("M2", m2);
        rec.setValue("S1", s1);
        rec.setValue("S2", s2);
        rec.setValue("V1", v1);
        rec.setValue("V2", v2);

        return rec;
    }
}
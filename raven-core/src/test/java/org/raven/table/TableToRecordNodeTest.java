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

package org.raven.table;

import java.util.List;
import org.junit.Test;
import org.raven.DataCollector;
import org.raven.PushDataSource;
import org.raven.RavenCoreTestCase;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.ValuePrepareRecordFieldExtension;

/**
 *
 * @author Mikhail Titov
 */
public class TableToRecordNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws RecordException
    {
        PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        RecordSchemaNode schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        createField(schema, "f1", 0, null);
        createField(schema, "f2", 2, "value+'_prepared'");

        TableToRecordNode tab2rec = new TableToRecordNode();
        tab2rec.setName("pipe");
        tree.getRootNode().addAndSaveChildren(tab2rec);
        tab2rec.setDataSource(ds);
        tab2rec.setRecordSchema(schema);
        assertTrue(tab2rec.start());

        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(tab2rec);
        assertTrue(collector.start());

        TableImpl table = new TableImpl(new String[]{"col1", "col2", "col3"});
        table.addRow(new Object[]{"val1", "val2", "val3"});
        ds.pushData(table);

        List dataList = collector.getDataList();
        assertNotNull(dataList);
        assertEquals(2, dataList.size());

        assertTrue(dataList.get(0) instanceof Record);
        Record rec = (Record) dataList.get(0);
        assertEquals("val1", rec.getValue("f1"));
        assertEquals("val3_prepared", rec.getValue("f2"));

        assertNull(dataList.get(1));
    }

    private void createField(RecordSchemaNode schema, String name, int colNum, String expression)
    {
        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
        field.setName(name);
        schema.addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.STRING);
        assertTrue(field.start());

        TableColumnRecordFieldExtension colExt = new TableColumnRecordFieldExtension();
        colExt.setName("table");
        field.addAndSaveChildren(colExt);
        colExt.setColumnNumber(colNum);
        assertTrue(colExt.start());

        if (expression!=null)
        {
            ValuePrepareRecordFieldExtension valPrep = new ValuePrepareRecordFieldExtension();
            valPrep.setName("prepare");
            colExt.addAndSaveChildren(valPrep);
            valPrep.setExpression(expression);
            valPrep.setUseExpression(true);
            assertTrue(valPrep.start());
        }
    }
}
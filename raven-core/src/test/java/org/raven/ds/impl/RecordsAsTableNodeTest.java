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

import java.io.ByteArrayInputStream;
import java.util.*;
import org.raven.auth.UserContext;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.ds.impl.RecordsAsTableNode.DeleteRecordAction;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.RavenUtils;
import org.raven.TestUserContext;
import org.raven.ds.BinaryFieldType;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.table.Table;
import org.raven.test.DataCollector;
import org.raven.test.UserContextServiceModule;
import org.raven.tree.ActionViewableObject;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.ActionAttributeValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.RefreshAttributeValueHandlerFactory;
import org.weda.constraints.ReferenceValue;
import org.weda.internal.Messages;
import org.weda.internal.services.MessagesRegistry;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class RecordsAsTableNodeTest extends RavenCoreTestCase
{
    private RecordSchemaNode schema;
    private PushOnDemandDataSource ds;
    private RecordsAsTableNode tableNode;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        RecordSchemaFieldNode field1 = new RecordSchemaFieldNode();
        field1.setName("field1");
        schema.addAndSaveChildren(field1);
        field1.setDisplayName("field1 displayName");
        field1.setFieldType(RecordSchemaFieldType.INTEGER);
        assertTrue(field1.start());

        RecordSchemaFieldNode field2 = new RecordSchemaFieldNode();
        field2.setName("field2");
        schema.addAndSaveChildren(field2);
        field2.setFieldType(RecordSchemaFieldType.INTEGER);
        assertTrue(field2.start());

        ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        tableNode = new RecordsAsTableNode();
        tableNode.setName("table");
        tree.getRootNode().addAndSaveChildren(tableNode);
        tableNode.setDataSource(ds);
        tableNode.setRecordSchema(schema);
        assertTrue(tableNode.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(tableNode);
        assertTrue(collector.start());
    }

    //fieldsOrder==null
    @Test
    public void tableGeneration1() throws Exception
    {
        Record record = createMock(Record.class);

        expect(record.getSchema()).andReturn(schema).times(2);
        expect(record.getValue("field1")).andReturn(1);
        expect(record.getValue("field1")).andReturn(2);
        expect(record.getValue("field2")).andReturn("test1");
        expect(record.getValue("field2")).andReturn("test2");

        replay(record);

        ds.addDataPortion(record);
        ds.addDataPortion(record);
        ds.addDataPortion(null);

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        assertArrayEquals(new String[]{"field1 displayName", "field2"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(2, rows.size());
        assertArrayEquals(new String[]{"1", "test1"}, rows.get(0));
        assertArrayEquals(new String[]{"2", "test2"}, rows.get(1));

        verify(record);
    }
    
    //fieldsOrder!=null
    @Test
    public void tableGeneration2() throws Exception
    {
        Record record = createMock(Record.class);

        expect(record.getSchema()).andReturn(schema).times(2);
        expect(record.getValue("field1")).andReturn(1);
        expect(record.getValue("field1")).andReturn(2);
        expect(record.getValue("field2")).andReturn("test1");
        expect(record.getValue("field2")).andReturn("test2");

        replay(record);

        ds.addDataPortion(record);
        ds.addDataPortion(record);
        ds.addDataPortion(null);

        tableNode.setFieldsOrder("field2, field1");

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        assertArrayEquals(new String[]{"field2", "field1 displayName"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(2, rows.size());
        assertArrayEquals(new String[]{"test1", "1"}, rows.get(0));
        assertArrayEquals(new String[]{"test2", "2"}, rows.get(1));

        verify(record);
    }
    
    //fieldsOrder!=null (fieldsOrder generates by script)
    @Test
    public void tableGeneration2_1() throws Exception
    {
        Record record = createMock(Record.class);

        expect(record.getSchema()).andReturn(schema).times(2);
        expect(record.getValue("field1")).andReturn(1);
        expect(record.getValue("field1")).andReturn(2);
        expect(record.getValue("field2")).andReturn("test1");
        expect(record.getValue("field2")).andReturn("test2");

        replay(record);

        ds.addDataPortion(record);
        ds.addDataPortion(record);
        ds.addDataPortion(null);

        NodeAttribute attr = tableNode.getNodeAttribute("fieldsOrder");
        assertNotNull(attr);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.save();
        tableNode.setFieldsOrder("refreshAttributes.containsKey('test')? 'field2, field1':null");

        Map<String, NodeAttribute> refAttrs = new HashMap<String, NodeAttribute>();
        refAttrs.put("test", new NodeAttributeImpl("test", String.class, null, null));
        Collection<ViewableObject> objects = tableNode.getViewableObjects(refAttrs);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        assertArrayEquals(new String[]{"field2", "field1 displayName"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(2, rows.size());
        assertArrayEquals(new String[]{"test1", "1"}, rows.get(0));
        assertArrayEquals(new String[]{"test2", "2"}, rows.get(1));

        verify(record);
    }
    
    @Test
    public void binaryFieldTypeTest() throws Exception
    {
        RecordSchemaFieldNode.create(schema, "field3", null, RecordSchemaFieldType.BINARY, null);
        byte[] arr = "content".getBytes();
        
        BinaryFieldType binaryValue = createMock(BinaryFieldType.class);
        Record record = createMock(Record.class);
        expect(record.getSchema()).andReturn(schema).anyTimes();
        expect(binaryValue.getData()).andReturn(new ByteArrayInputStream(arr));
        expect(record.getValue("field3")).andReturn(binaryValue);
        
        replay(binaryValue, record);
        
        tableNode.setFieldsOrder("field3");
        
        ds.addDataPortion(record);
        ds.addDataPortion(null);
        
        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(1, rows.size());
        assertTrue(rows.get(0)[0] instanceof ViewableObject);
        ViewableObject vo = (ViewableObject) rows.get(0)[0];
        assertEquals("file", vo.toString());
        assertEquals("application/octet-stream", vo.getMimeType());
        Object data = vo.getData();
        assertTrue(data instanceof BinaryFieldType);
        assertArrayEquals(arr, IOUtils.toByteArray(((BinaryFieldType)data).getData()));

        verify(record, binaryValue);
    }

    @Test
    public void binaryFieldTypeWithFileExtensionTest() throws Exception
    {
        RecordSchemaFieldNode field = RecordSchemaFieldNode.create(
                schema, "field3", null, RecordSchemaFieldType.BINARY, null);
        FileRecordFieldExtension fileExt = new FileRecordFieldExtension();
        fileExt.setName("file");
        field.addAndSaveChildren(fileExt);
        fileExt.setMimeType("text/plain");
        NodeAttribute attr = fileExt.getNodeAttribute(FileRecordFieldExtension.FILENAME_ATTR);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("record['filename']");
        assertTrue(fileExt.start());
        
        RecordSchemaFieldNode.create(schema, "filename", null, RecordSchemaFieldType.STRING, null);

        byte[] arr = "content".getBytes();

        BinaryFieldType binaryValue = createMock(BinaryFieldType.class);
        Record record = createMock(Record.class);
        expect(record.getSchema()).andReturn(schema).anyTimes();
        expect(binaryValue.getData()).andReturn(new ByteArrayInputStream(arr));
        expect(record.getValue("field3")).andReturn(binaryValue);
        expect(record.getAt("filename")).andReturn("filename.txt");

        replay(binaryValue, record);

        tableNode.setFieldsOrder("field3");

        ds.addDataPortion(record);
        ds.addDataPortion(null);

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(1, rows.size());
        assertTrue(rows.get(0)[0] instanceof ViewableObject);
        ViewableObject vo = (ViewableObject) rows.get(0)[0];
        assertEquals("filename.txt", vo.toString());
        assertEquals("text/plain", vo.getMimeType());
        Object data = vo.getData();
        assertTrue(data instanceof BinaryFieldType);
        assertArrayEquals(arr, IOUtils.toByteArray(((BinaryFieldType)data).getData()));

        verify(record, binaryValue);
    }

    @Test
    public void getRefreshAttributesTest() throws Exception
    {
        assertNull(tableNode.getRefreshAttributes());

        NodeAttributeImpl attr = new NodeAttributeImpl("field1", String.class, null, null);
        attr.setValueHandlerType(RefreshAttributeValueHandlerFactory.TYPE);
        attr.setOwner(tableNode);
        attr.setParentAttribute(RecordsAsTableNode.DATA_SOURCE_ATTR);
        tableNode.addNodeAttribute(attr);
        attr.init();
        attr.save();

        Map<String, NodeAttribute> refreshAttrs = tableNode.getRefreshAttributes();
        assertNotNull(refreshAttrs);
        assertEquals(1, refreshAttrs.size());
        NodeAttribute refAttr = refreshAttrs.get("field1");
        assertNotNull(refAttr);
        assertEquals("field1 displayName", refAttr.getDisplayName());
    }

    @Test
    public void getRefreshAttributesWithSchemaReferenceValuesTest() throws Exception
    {
        CustomReferenceValuesSourceNode valuesSource = new CustomReferenceValuesSourceNode();
        valuesSource.setName("values source");
        schema.getChildren("field1").addAndSaveChildren(valuesSource);
        valuesSource.setReferenceValuesExpression("[1:'one']");
        assertTrue(valuesSource.start());

        assertNull(tableNode.getRefreshAttributes());

        NodeAttributeImpl attr = new NodeAttributeImpl("field1", String.class, null, null);
        attr.setValueHandlerType(RefreshAttributeValueHandlerFactory.TYPE);
        attr.setOwner(tableNode);
        attr.setParentAttribute(RecordsAsTableNode.DATA_SOURCE_ATTR);
        tableNode.addNodeAttribute(attr);
        attr.init();
        attr.save();

        Map<String, NodeAttribute> refreshAttrs = tableNode.getRefreshAttributes();
        assertNotNull(refreshAttrs);
        assertEquals(1, refreshAttrs.size());
        NodeAttribute refAttr = refreshAttrs.get("field1");
        assertNotNull(refAttr);
        assertEquals("field1 displayName", refAttr.getDisplayName());
        List<ReferenceValue> refValues = refAttr.getReferenceValues();
        assertNotNull(refValues);
        assertEquals(1, refValues.size());
        assertEquals("1", refValues.get(0).getValue());
        assertEquals("one", refValues.get(0).getValueAsString());
    }

    @Test
    public void getRefreshAttributesWithFieldReferenceValuesTest() throws Exception
    {
        CustomReferenceValuesSourceNode valuesSource = new CustomReferenceValuesSourceNode();
        valuesSource.setName("values source");
        tree.getRootNode().addAndSaveChildren(valuesSource);
        valuesSource.setReferenceValuesExpression("[1:'one']");
        assertTrue(valuesSource.start());

        RecordFieldReferenceValuesNode fieldValues = new RecordFieldReferenceValuesNode();
        fieldValues.setName("fieldValues");
        tableNode.addAndSaveChildren(fieldValues);
        fieldValues.setFieldName("field3");
        fieldValues.setReferenceValuesSource(valuesSource);
        assertTrue(fieldValues.start());

        assertNull(tableNode.getRefreshAttributes());

        NodeAttributeImpl attr = new NodeAttributeImpl("field3", String.class, null, null);
        attr.setValueHandlerType(RefreshAttributeValueHandlerFactory.TYPE);
        attr.setOwner(tableNode);
        attr.setParentAttribute(RecordsAsTableNode.DATA_SOURCE_ATTR);
        tableNode.addNodeAttribute(attr);
        attr.init();
        attr.save();

        Map<String, NodeAttribute> refreshAttrs = tableNode.getRefreshAttributes();
        assertNotNull(refreshAttrs);
        assertEquals(1, refreshAttrs.size());
        NodeAttribute refAttr = refreshAttrs.get("field3");
        assertNotNull(refAttr);
        List<ReferenceValue> refValues = refAttr.getReferenceValues();
        assertNotNull(refValues);
        assertEquals(1, refValues.size());
        assertEquals("1", refValues.get(0).getValue());
        assertEquals("one", refValues.get(0).getValueAsString());
    }

    @Test
    public void sessionAttributesTest() throws Exception
    {
        assertNull(tableNode.getRefreshAttributes());

        NodeAttributeImpl attr = new NodeAttributeImpl("filter1", String.class, null, null);
        attr.setValueHandlerType(RefreshAttributeValueHandlerFactory.TYPE);
        attr.setOwner(tableNode);
        attr.setParentAttribute(RecordsAsTableNode.DATA_SOURCE_ATTR);
        tableNode.addNodeAttribute(attr);
        attr.init();
        attr.save();

        attr = new NodeAttributeImpl("filter2", String.class, null, null);
        attr.setOwner(tableNode);
        attr.setParentAttribute(RecordsAsTableNode.DATA_SOURCE_ATTR);
        tableNode.addNodeAttribute(attr);
        attr.init();
        attr.save();

        Map<String, NodeAttribute> attrs = tableNode.getRefreshAttributes();
        assertNotNull(attrs);
        assertEquals(1, attrs.size());

        tableNode.getViewableObjects(attrs);

        attrs = ds.getLastSessionAttributes();
        assertNotNull(attrs);
        assertEquals(2, attrs.size());
        assertNotNull(attrs.get("filter1"));
        assertNotNull(attrs.get("filter2"));
    }

    @Test
    public void detailTableColumnTest() throws Exception
    {
        tableNode.setShowFieldsInDetailColumn(true);

        Record record = createMock(Record.class);

        expect(record.getSchema()).andReturn(schema);
        expect(record.getValue("field1")).andReturn(1).times(2);
        expect(record.getValue("field2")).andReturn("test1").times(2);

        replay(record);

        ds.addDataPortion(record);
        ds.addDataPortion(null);

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        MessagesRegistry messagesRegistry = registry.getService(MessagesRegistry.class);
        Messages messages = messagesRegistry.getMessages(RecordsAsTableNode.class);
        String detailColumnName =messages.get("detailColumnName");
        String detailLinkName = messages.get("detailValueViewLinkName");
        assertArrayEquals(
                new String[]{"field1 displayName", "field2", detailColumnName}
                , table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(1, rows.size());
        assertEquals("1", rows.get(0)[0]);
        assertEquals("test1", rows.get(0)[1]);

        Object detailObj = rows.get(0)[2];
        assertNotNull(detailObj);
        assertTrue(detailObj instanceof ViewableObject);
        ViewableObject detail = (ViewableObject) detailObj;
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, detail.getMimeType());
        assertEquals(detailLinkName, detail.toString());

        Object detailData = detail.getData();
        assertNotNull(detailData);
        assertTrue(detailData instanceof Table);
        Table detailTable = (Table) detailData;
        assertArrayEquals(
                new String[]{
                        messages.get("fieldNameColumnName")
                        , messages.get("fieldValueColumnName")}
                , detailTable.getColumnNames());
        List<Object[]> detailRows = RavenUtils.tableAsList(detailTable);
        assertEquals(2, detailRows.size());
        assertArrayEquals(new String[]{"field1", "1"}, detailRows.get(0));
        assertArrayEquals(new String[]{"field2", "test1"}, detailRows.get(1));

        verify(record);
    }

    @Test
    public void detailTableColumnTest2() throws Exception
    {
        tableNode.setShowFieldsInDetailColumn(true);

        Record record = createMock(Record.class);

        expect(record.getSchema()).andReturn(schema);
        expect(record.getValue("field1")).andReturn(1).times(2);
        expect(record.getValue("field2")).andReturn("test1").times(2);

        replay(record);

        ds.addDataPortion(record);
        ds.addDataPortion(null);

        tableNode.setDetailColumnNumber(1);
        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        MessagesRegistry messagesRegistry = registry.getService(MessagesRegistry.class);
        Messages messages = messagesRegistry.getMessages(RecordsAsTableNode.class);
        assertArrayEquals(
                new String[]{"field1 displayName", "field2"}
                , table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(1, rows.size());
        assertEquals("test1", rows.get(0)[1]);

        Object detailObj = rows.get(0)[0];
        assertNotNull(detailObj);
        assertTrue(detailObj instanceof ViewableObject);
        ViewableObject detail = (ViewableObject) detailObj;
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, detail.getMimeType());
        assertEquals("1", detailObj.toString());

        Object detailData = detail.getData();
        assertNotNull(detailData);
        assertTrue(detailData instanceof Table);
        Table detailTable = (Table) detailData;
        assertArrayEquals(
                new String[]{
                        messages.get("fieldNameColumnName")
                        , messages.get("fieldValueColumnName")}
                , detailTable.getColumnNames());
        List<Object[]> detailRows = RavenUtils.tableAsList(detailTable);
        assertEquals(2, detailRows.size());
        assertArrayEquals(new String[]{"field1", "1"}, detailRows.get(0));
        assertArrayEquals(new String[]{"field2", "test1"}, detailRows.get(1));

        verify(record);
    }

    @Test
    public void cellValueExpressionTest() throws Exception
    {
        tableNode.setUseCellValueExpression(Boolean.TRUE);
        tableNode.setCellValueExpression("value+columnNumber+record['field1']");

        Record rec = schema.createRecord();
        rec.setValue("field1", 1);
        rec.setValue("field2", 2);

        ds.addDataPortion(rec);
        ds.addDataPortion(null);

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        assertArrayEquals(new String[]{"field1 displayName", "field2"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(1, rows.size());
        assertArrayEquals(new String[]{"3", "5"}, rows.get(0));
    }

    @Test
    public void columnValueTest() throws Exception
    {
        RecordsAsTableColumnValueNode columnValue = new RecordsAsTableColumnValueNode();
        columnValue.setName("column 1 value");
        tableNode.addAndSaveChildren(columnValue);
        columnValue.setColumnNumber(1);
        columnValue.getNodeAttribute("columnValue").setValue("value+record['field1']");
        assertTrue(columnValue.start());

        Record record = createMock(Record.class);

        expect(record.getSchema()).andReturn(schema);
        expect(record.getValue("field1")).andReturn(1).times(1);
        expect(record.getAt("field1")).andReturn(1).times(1);
        expect(record.getValue("field2")).andReturn("test1").times(1);

        replay(record);

        ds.addDataPortion(record);
        ds.addDataPortion(null);

        tableNode.setFieldsOrder("field2, field1");

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        assertArrayEquals(new String[]{"field2", "field1 displayName"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(1, rows.size());
        assertArrayEquals(new String[]{"test11", "1"}, rows.get(0));

        verify(record);
    }

    @Test
    public void fieldReferenceValuesTest() throws Exception
    {
        CustomReferenceValuesSourceNode source = new CustomReferenceValuesSourceNode();
        source.setName("ref values source");
        tree.getRootNode().addAndSaveChildren(source);
        source.setReferenceValuesExpression("[1:100]");
        assertTrue(source.start());

        RecordFieldReferenceValuesNode valuesNode = new RecordFieldReferenceValuesNode();
        valuesNode.setName("ref values");
        tableNode.addAndSaveChildren(valuesNode);
        valuesNode.setFieldName("field1");
        valuesNode.setReferenceValuesSource(source);
        assertTrue(valuesNode.start());

        Record record = createMock(Record.class);

        expect(record.getSchema()).andReturn(schema);
        expect(record.getValue("field1")).andReturn(1).times(1);
        expect(record.getValue("field2")).andReturn(2).times(1);

        replay(record);
        
        ds.addDataPortion(record);
        ds.addDataPortion(null);

        tableNode.setFieldsOrder("field1, field2");

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        assertArrayEquals(new String[]{"field1 displayName", "field2"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(1, rows.size());
        assertArrayEquals(new String[]{"100", "2"}, rows.get(0));

        verify(record);
    }
        

    @Test
    public void schemaFieldReferenceValuesTest() throws Exception
    {
        CustomReferenceValuesSourceNode source = new CustomReferenceValuesSourceNode();
        source.setName("ref values source");
        schema.getChildren("field1").addAndSaveChildren(source);
        source.setReferenceValuesExpression("[1:'one']");
        assertTrue(source.start());

        Record record = createMock(Record.class);

        expect(record.getSchema()).andReturn(schema);
        expect(record.getValue("field1")).andReturn(1).times(1);
        expect(record.getValue("field2")).andReturn(2).times(1);

        replay(record);

        ds.addDataPortion(record);
        ds.addDataPortion(null);

        tableNode.setFieldsOrder("field1, field2");

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        assertArrayEquals(new String[]{"field1 displayName", "field2"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(1, rows.size());
        assertArrayEquals(new String[]{"one", "2"}, rows.get(0));

        verify(record);
    }

    @Test
    public void deleteRecordTest() throws Exception
    {
        tableNode.setEnableDeletes(Boolean.TRUE);
        Record record = createMock(Record.class);

        expect(record.getSchema()).andReturn(schema).times(1);
        expect(record.getValue("field1")).andReturn(1);
        expect(record.getValue("field2")).andReturn("test1");
        record.setTag(Record.DELETE_TAG, null);

        replay(record);

        ds.addDataPortion(record);
        ds.addDataPortion(null);

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        assertArrayEquals(new String[]{null, "field1 displayName", "field2"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(1, rows.size());
        assertEquals("1", rows.get(0)[1]);
        assertEquals("test1", rows.get(0)[2]);

        assertNotNull(rows.get(0)[0]);
        assertTrue(rows.get(0)[0] instanceof RecordsAsTableNode.DeleteRecordAction);
        RecordsAsTableNode.DeleteRecordAction action = (DeleteRecordAction) rows.get(0)[0];
        assertNotNull(action.toString());
        assertNotNull(action.getConfirmationMessage());
        assertEquals(0, collector.getDataList().size());
        Object res = action.getData();
        assertNotNull(res);
        assertTrue(res instanceof String);
        
        assertEquals(2, collector.getDataList().size());
        assertSame(record, collector.getDataList().get(0));
        assertNull(collector.getDataList().get(1));

        verify(record);
    }

    @Test
    public void recordActionTest() throws Exception
    {
        RecordsAsTableRecordActionNode recordAction = new RecordsAsTableRecordActionNode();
        recordAction.setName("action");
        tableNode.addAndSaveChildren(recordAction);
        recordAction.setEnabledActionText("action title");
        recordAction.setActionExpression("record['field1']=2");
        assertTrue(recordAction.start());
        
        collector.setDataSource(recordAction);

        Record record = createMock(Record.class);

        expect(record.getSchema()).andReturn(schema);
        expect(record.getValue("field1")).andReturn(1).times(1);
        record.putAt("field1", 2);

        replay(record);

        ds.addDataPortion(record);
        ds.addDataPortion(null);

        tableNode.setFieldsOrder("field1");

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        assertArrayEquals(new String[]{null, "field1 displayName"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(1, rows.size());
        assertEquals("1", rows.get(0)[1]);
        assertTrue(rows.get(0)[0] instanceof ActionViewableObject);
        ActionViewableObject action = (ActionViewableObject)rows.get(0)[0];

        assertEquals("action title", action.toString());
        assertTrue(collector.getDataList().isEmpty());
        assertEquals("2", action.getData());
        assertEquals(2, collector.getDataListSize());
        assertSame(record, collector.getDataList().get(0));
        assertNull(collector.getDataList().get(1));

        collector.getDataList().clear();
        recordAction.setActionExpression("dataList<<'test data';'test'");
        assertEquals("test", action.getData());
        assertEquals(1, collector.getDataListSize());
        assertEquals("test data", collector.getDataList().get(0));

        verify(record);
    }

    @Test
    public void addRecordActionTest() throws Exception
    {
        AddRecordActionNode addAction = new AddRecordActionNode();
        addAction.setName("action");
        tableNode.addAndSaveChildren(addAction);
        addAction.setEnabledActionText("action title");
        addAction.setPrepareRecord("record['field1']=1");
        addAction.setPrepareActionAttributes("actionAttributes.field1.value+='2'");
        addAction.setActionExpression("record['field1']++");
        assertTrue(addAction.start());

        NodeAttributeImpl field2Attr = new NodeAttributeImpl("field2", String.class, 10, null);
        field2Attr.setOwner(addAction);
        field2Attr.setValueHandlerType(ActionAttributeValueHandlerFactory.TYPE);
        field2Attr.init();
        addAction.addNodeAttribute(field2Attr);

        collector.setDataSource(addAction);

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(2, objects.size());
        ViewableObject object = objects.iterator().next();
        assertTrue(object instanceof ActionViewableObject);
        ActionViewableObject action = (ActionViewableObject) object;
        Collection<NodeAttribute> attrs = action.getActionAttributes();
        assertNotNull(attrs);
        assertEquals(2, attrs.size());
        Iterator<NodeAttribute> it = attrs.iterator();
        NodeAttribute f1 = it.next();
        NodeAttribute f2 = it.next();
        assertEquals("field1", f1.getName());
        assertEquals("field1 displayName", f1.getDisplayName());
        assertEquals("12", f1.getValue());
        assertEquals("field2", f2.getName());
        assertEquals("10", f2.getValue());
        f1.setValue("1");

        action.getData();
        
        assertEquals(2, collector.getDataListSize());
        assertNull(collector.getDataList().get(1));
        assertTrue(collector.getDataList().get(0) instanceof Record);
        Record rec = (Record) collector.getDataList().get(0);
        assertEquals(2, rec.getValue("field1"));
        assertEquals(10, rec.getValue("field2"));
    }

    @Test
    public void addRecordActionFieldsOrderTest() throws Exception
    {
        AddRecordActionNode addAction = new AddRecordActionNode();
        addAction.setName("action");
        tableNode.addAndSaveChildren(addAction);
        addAction.setEnabledActionText("action title");
        addAction.setActionExpression("record['field1']++");
        addAction.setFieldsOrder("field2, field1");
        assertTrue(addAction.start());

        NodeAttributeImpl field2Attr = new NodeAttributeImpl("field2", String.class, 10, null);
        field2Attr.setOwner(addAction);
        field2Attr.setValueHandlerType(ActionAttributeValueHandlerFactory.TYPE);
        field2Attr.init();
        addAction.addNodeAttribute(field2Attr);

        collector.setDataSource(addAction);

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(2, objects.size());
        ViewableObject object = objects.iterator().next();
        assertTrue(object instanceof ActionViewableObject);
        ActionViewableObject action = (ActionViewableObject) object;
        Collection<NodeAttribute> attrs = action.getActionAttributes();
        assertNotNull(attrs);
        assertEquals(2, attrs.size());
        Iterator<NodeAttribute> it = attrs.iterator();
        NodeAttribute f2 = it.next();
        NodeAttribute f1 = it.next();
        assertEquals("field1", f1.getName());
        assertEquals("field1 displayName", f1.getDisplayName());
        assertEquals("field2", f2.getName());
        assertEquals("10", f2.getValue());
        f1.setValue("1");

        action.getData();

        assertEquals(2, collector.getDataListSize());
        assertNull(collector.getDataList().get(1));
        assertTrue(collector.getDataList().get(0) instanceof Record);
        Record rec = (Record) collector.getDataList().get(0);
        assertEquals(2, rec.getValue("field1"));
        assertEquals(10, rec.getValue("field2"));
    }

    @Test
    public void addRecordActionSchemaReferenceValuesTest() throws Exception
    {
        CustomReferenceValuesSourceNode valuesSource = new CustomReferenceValuesSourceNode();
        valuesSource.setName("values source");
        schema.getChildren("field1").addAndSaveChildren(valuesSource);
        valuesSource.setReferenceValuesExpression("[1:'one']");
        assertTrue(valuesSource.start());

        AddRecordActionNode addAction = new AddRecordActionNode();
        addAction.setName("action");
        tableNode.addAndSaveChildren(addAction);
        addAction.setEnabledActionText("action title");
        addAction.setActionExpression("null");
        assertTrue(addAction.start());

        collector.setDataSource(addAction);

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(2, objects.size());
        ViewableObject object = objects.iterator().next();
        assertTrue(object instanceof ActionViewableObject);
        ActionViewableObject action = (ActionViewableObject) object;
        Collection<NodeAttribute> attrs = action.getActionAttributes();
        assertNotNull(attrs);
        assertEquals(2, attrs.size());
        Iterator<NodeAttribute> it = attrs.iterator();
        NodeAttribute f1 = it.next();
        NodeAttribute f2 = it.next();
        assertNull(f2.getReferenceValues());
        List<ReferenceValue> refValues = f1.getReferenceValues();
        assertNotNull(refValues);
        assertEquals(1, refValues.size());
        assertEquals("1", refValues.get(0).getValue());
        assertEquals("one", refValues.get(0).getValueAsString());
    }

    @Test
    public void addRecordActionFieldReferenceValuesTest() throws Exception
    {
        CustomReferenceValuesSourceNode valuesSource = new CustomReferenceValuesSourceNode();
        valuesSource.setName("values source");
        tree.getRootNode().addAndSaveChildren(valuesSource);
        valuesSource.setReferenceValuesExpression("[1:'one']");
        assertTrue(valuesSource.start());

        RecordFieldReferenceValuesNode fieldRefValues = new RecordFieldReferenceValuesNode();
        fieldRefValues.setName("refValues");
        tableNode.addAndSaveChildren(fieldRefValues);
        fieldRefValues.setFieldName("field1");
        fieldRefValues.setReferenceValuesSource(valuesSource);
        assertTrue(fieldRefValues.start());

        AddRecordActionNode addAction = new AddRecordActionNode();
        addAction.setName("action");
        tableNode.addAndSaveChildren(addAction);
        addAction.setEnabledActionText("action title");
        addAction.setActionExpression("null");
        assertTrue(addAction.start());

        collector.setDataSource(addAction);

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(2, objects.size());
        ViewableObject object = objects.iterator().next();
        assertTrue(object instanceof ActionViewableObject);
        ActionViewableObject action = (ActionViewableObject) object;
        Collection<NodeAttribute> attrs = action.getActionAttributes();
        assertNotNull(attrs);
        assertEquals(2, attrs.size());
        Iterator<NodeAttribute> it = attrs.iterator();
        NodeAttribute f1 = it.next();
        NodeAttribute f2 = it.next();
        assertNull(f2.getReferenceValues());
        List<ReferenceValue> refValues = f1.getReferenceValues();
        assertNotNull(refValues);
        assertEquals(1, refValues.size());
        assertEquals("1", refValues.get(0).getValue());
        assertEquals("one", refValues.get(0).getValueAsString());
    }

    @Test
    public void editRecordActionFieldsOrderTest() throws Exception
    {
        EditRecordActionNode edit = new EditRecordActionNode();
        edit.setName("action");
        tableNode.addAndSaveChildren(edit);
        edit.setEnabledActionText("action title");
        edit.setPrepareRecord("record['field1']+=1");
        edit.setPrepareActionAttributes("actionAttributes.field1.value+='2'");
        edit.setActionExpression("record['field1']++");
        edit.setFieldsOrder("field2, field1");
        assertTrue(edit.start());

        NodeAttributeImpl field2Attr = new NodeAttributeImpl("field2", String.class, 10, null);
        field2Attr.setOwner(edit);
        field2Attr.setValueHandlerType(ActionAttributeValueHandlerFactory.TYPE);
        field2Attr.init();
        edit.addNodeAttribute(field2Attr);

        collector.setDataSource(edit);

        Record rec = schema.createRecord();
        rec.setValue("field1", 1);
        ds.addDataPortion(rec);
        ds.addDataPortion(null);

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        Table tab = (Table) object.getData();

        Object actionObject = tab.getRowIterator().next()[0];
        assertTrue(actionObject instanceof ActionViewableObject);
        ActionViewableObject action = (ActionViewableObject) actionObject;
        Collection<NodeAttribute> attrs = action.getActionAttributes();
        assertNotNull(attrs);
        assertEquals(2, attrs.size());
        Iterator<NodeAttribute> it = attrs.iterator();
        NodeAttribute f2 = it.next();
        NodeAttribute f1 = it.next();
        assertEquals("field1", f1.getName());
        assertEquals("field1 displayName", f1.getDisplayName());
        assertEquals("22", f1.getValue());
        assertEquals("field2", f2.getName());
        assertEquals("10", f2.getValue());
        f1.setValue("2");

        action.getData();

        assertEquals(2, collector.getDataListSize());
        assertNull(collector.getDataList().get(1));
        assertTrue(collector.getDataList().get(0) instanceof Record);
        rec = (Record) collector.getDataList().get(0);
        assertEquals(3, rec.getValue("field1"));
        assertEquals(10, rec.getValue("field2"));
    }

    @Test
    public void editRecordActionSchemaReferenceValuesTest() throws Exception
    {
        CustomReferenceValuesSourceNode valuesSource = new CustomReferenceValuesSourceNode();
        valuesSource.setName("values source");
        schema.getChildren("field1").addAndSaveChildren(valuesSource);
        valuesSource.setReferenceValuesExpression("[1:'one']");
        assertTrue(valuesSource.start());

        EditRecordActionNode edit = new EditRecordActionNode();
        edit.setName("action");
        tableNode.addAndSaveChildren(edit);
        edit.setEnabledActionText("action title");
        edit.setActionExpression("null");
        assertTrue(edit.start());

        collector.setDataSource(edit);

        Record rec = schema.createRecord();
        rec.setValue("field1", 1);
        ds.addDataPortion(rec);
        ds.addDataPortion(null);

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        Table tab = (Table) object.getData();

        Object actionObject = tab.getRowIterator().next()[0];
        assertTrue(actionObject instanceof ActionViewableObject);
        ActionViewableObject action = (ActionViewableObject) actionObject;
        Collection<NodeAttribute> attrs = action.getActionAttributes();
        assertNotNull(attrs);
        assertEquals(2, attrs.size());
        Iterator<NodeAttribute> it = attrs.iterator();
        NodeAttribute f1 = it.next();
        NodeAttribute f2 = it.next();

        assertNull(f2.getReferenceValues());
        List<ReferenceValue> refValues = f1.getReferenceValues();
        assertNotNull(refValues);
        assertEquals(1, refValues.size());
        assertEquals("1", refValues.get(0).getValue());
        assertEquals("one", refValues.get(0).getValueAsString());
    }

    @Test
    public void editRecordActionFieldReferenceValuesTest() throws Exception
    {
        CustomReferenceValuesSourceNode valuesSource = new CustomReferenceValuesSourceNode();
        valuesSource.setName("values source");
        tree.getRootNode().addAndSaveChildren(valuesSource);
        valuesSource.setReferenceValuesExpression("[1:'one']");
        assertTrue(valuesSource.start());

        RecordFieldReferenceValuesNode fieldValues = new RecordFieldReferenceValuesNode();
        fieldValues.setName("fieldValues");
        tableNode.addAndSaveChildren(fieldValues);
        fieldValues.setFieldName("field1");
        fieldValues.setReferenceValuesSource(valuesSource);
        assertTrue(fieldValues.start());

        EditRecordActionNode edit = new EditRecordActionNode();
        edit.setName("action");
        tableNode.addAndSaveChildren(edit);
        edit.setEnabledActionText("action title");
        edit.setActionExpression("null");
        assertTrue(edit.start());

        collector.setDataSource(edit);

        Record rec = schema.createRecord();
        rec.setValue("field1", 1);
        ds.addDataPortion(rec);
        ds.addDataPortion(null);

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        Table tab = (Table) object.getData();

        Object actionObject = tab.getRowIterator().next()[0];
        assertTrue(actionObject instanceof ActionViewableObject);
        ActionViewableObject action = (ActionViewableObject) actionObject;
        Collection<NodeAttribute> attrs = action.getActionAttributes();
        assertNotNull(attrs);
        assertEquals(2, attrs.size());
        Iterator<NodeAttribute> it = attrs.iterator();
        NodeAttribute f1 = it.next();
        NodeAttribute f2 = it.next();

        assertNull(f2.getReferenceValues());
        List<ReferenceValue> refValues = f1.getReferenceValues();
        assertNotNull(refValues);
        assertEquals(1, refValues.size());
        assertEquals("1", refValues.get(0).getValue());
        assertEquals("one", refValues.get(0).getValueAsString());
    }

    @Test
    public void actionTest() throws Exception
    {
        RecordsAsTableActionNode recordAction = new RecordsAsTableActionNode();
        recordAction.setName("action");
        tableNode.addAndSaveChildren(recordAction);
        recordAction.setEnabledActionText("action title");
        recordAction.setActionExpression("records.size()");
        assertTrue(recordAction.start());

        collector.setDataSource(recordAction);

        Record record = createMock(Record.class);

        expect(record.getSchema()).andReturn(schema);
        expect(record.getValue("field1")).andReturn(1).times(1);

        replay(record);

        ds.addDataPortion(record);
        ds.addDataPortion(null);

        tableNode.setFieldsOrder("field1");

        Collection<ViewableObject> objects = tableNode.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(2, objects.size());

        List<ViewableObject> vos = new ArrayList<ViewableObject>(objects);
        ViewableObject object = vos.get(0);
        assertEquals(Viewable.RAVEN_ACTION_MIMETYPE, object.getMimeType());
        assertTrue(object instanceof ActionViewableObject);
        ActionViewableObject action = (ActionViewableObject)object;
        assertEquals("action title", action.toString());
        assertTrue(collector.getDataList().isEmpty());
        assertEquals("1", action.getData());
//        assertEquals(1, collector.getDataListSize());
//        assertSame(record, collector.getDataList().get(0));
        

        object = vos.get(1);
        assertNotNull(object);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        assertNotNull(object.getData());
        assertTrue(object.getData() instanceof Table);
        Table table = (Table) object.getData();
        assertArrayEquals(new String[]{"field1 displayName"}, table.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(1, rows.size());
        assertEquals("1", rows.get(0)[0]);

        verify(record);
    }

    @Test
    public void detailTest() throws Exception
    {
        BaseNode masterNode = new BaseNode("master node");
        tree.getRootNode().addAndSaveChildren(masterNode);
        assertTrue(masterNode.start());

        UserContext userContext = new TestUserContext();
        UserContextServiceModule.setUserContext(userContext);

        tableNode.setMasterNode(masterNode);
        tableNode.setDetailFields("field1, field2");
        
        NodeAttributeImpl attr = new NodeAttributeImpl("field1", String.class, null, null);
        attr.setValueHandlerType(RefreshAttributeValueHandlerFactory.TYPE);
        attr.setOwner(tableNode);
        attr.setParentAttribute(RecordsAsTableNode.DATA_SOURCE_ATTR);
        tableNode.addNodeAttribute(attr);
        attr.init();
        attr.save();
        attr = new NodeAttributeImpl("field2", String.class, null, null);
        attr.setOwner(tableNode);
        attr.setParentAttribute(RecordsAsTableNode.DATA_SOURCE_ATTR);
        tableNode.addNodeAttribute(attr);
        attr.init();
        attr.save();

        Map<String, NodeAttribute> attrs = tableNode.getRefreshAttributes();
        assertNotNull(attrs);
        assertEquals(1, attrs.size());
        
        assertNull(tableNode.getViewableObjects(attrs));
        assertNull(ds.getLastSessionAttributes());

        RavenUtils.setMasterFieldValues(masterNode, Arrays.asList("1", "2"));
        tableNode.getViewableObjects(attrs);

        attrs = ds.getLastSessionAttributes();
        assertNotNull(attrs);
        assertEquals(2, attrs.size());
        checkSessionAttribute(attrs, "field1", "1");
        checkSessionAttribute(attrs, "field2", "2");
    }

    @Test()
    public void addAction_detailTest() throws Exception
    {
        BaseNode masterNode = new BaseNode("master node");
        tree.getRootNode().addAndSaveChildren(masterNode);
        assertTrue(masterNode.start());

        UserContext userContext = new TestUserContext();
        UserContextServiceModule.setUserContext(userContext);

        tableNode.setMasterNode(masterNode);
        tableNode.setDetailFields("field1");

        NodeAttributeImpl attr = new NodeAttributeImpl("field1", String.class, null, null);
        attr.setValueHandlerType(RefreshAttributeValueHandlerFactory.TYPE);
        attr.setOwner(tableNode);
        attr.setParentAttribute(RecordsAsTableNode.DATA_SOURCE_ATTR);
        tableNode.addNodeAttribute(attr);
        attr.init();
        attr.save();

        AddRecordActionNode addActionNode = new AddRecordActionNode();
        addActionNode.setName("add");
        tableNode.addAndSaveChildren(addActionNode);
        addActionNode.setEnabledActionText("add");
        addActionNode.setConfirmationMessage("Adding");
        assertTrue(addActionNode.start());

        ds.addDataPortion(null);
        assertNull(tableNode.getViewableObjects(null));
    }

    @Test
    public void addAction_detailTest2() throws Exception
    {
        BaseNode masterNode = new BaseNode("master node");
        tree.getRootNode().addAndSaveChildren(masterNode);
        assertTrue(masterNode.start());

        UserContext userContext = new TestUserContext();
        UserContextServiceModule.setUserContext(userContext);

        tableNode.setMasterNode(masterNode);
        tableNode.setDetailFields("field1");

        NodeAttributeImpl attr = new NodeAttributeImpl("field1", String.class, null, null);
        attr.setValueHandlerType(RefreshAttributeValueHandlerFactory.TYPE);
        attr.setOwner(tableNode);
        attr.setParentAttribute(RecordsAsTableNode.DATA_SOURCE_ATTR);
        tableNode.addNodeAttribute(attr);
        attr.init();
        attr.save();

        AddRecordActionNode addActionNode = new AddRecordActionNode();
        addActionNode.setName("add");
        tableNode.addAndSaveChildren(addActionNode);
        addActionNode.setEnabledActionText("add");
        addActionNode.setConfirmationMessage("Adding");
        assertTrue(addActionNode.start());

        collector.setDataSource(addActionNode);

        ds.addDataPortion(null);
        RavenUtils.setMasterFieldValues(masterNode, Arrays.asList("1"));
        List<ViewableObject> vos = tableNode.getViewableObjects(null);
        assertNotNull(vos);
        assertEquals(2, vos.size());
        assertEquals(Viewable.RAVEN_ACTION_MIMETYPE, vos.get(0).getMimeType());
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vos.get(1).getMimeType());

        vos.get(0).getData();
        assertEquals(2, collector.getDataListSize());
        assertNull(collector.getDataList().get(1));
        Object obj = collector.getDataList().get(0);
        assertTrue(obj instanceof Record);
        assertEquals(1, ((Record)obj).getValue("field1"));
    }

    @Test
    public void masterTest() throws Exception
    {
        UserContext userContext = new TestUserContext();
        UserContextServiceModule.setUserContext(userContext);

        tableNode.setEnableSelect(Boolean.TRUE);
        tableNode.setIndexFields("field1");
        tableNode.setMasterFields("field1, field2");

        Record rec = schema.createRecord();
        rec.setValue("field1", 1);
        rec.setValue("field2", 2);
        ds.addDataPortion(rec);
        rec = schema.createRecord();
        rec.setValue("field1", 2);
        rec.setValue("field2", 3);
        ds.addDataPortion(rec);

        ds.addDataPortion(null);

        List<ViewableObject> vos = tableNode.getViewableObjects(null);
        assertNotNull(vos);
        assertEquals(1, vos.size());

        Table table = (Table) vos.get(0).getData();
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertTrue(rows.get(0)[0] instanceof RecordsAsTableNode.SelectRowAction);
        ViewableObject action = (ViewableObject) rows.get(0)[0];
        assertNull(action.getData());
        Object key = userContext.getParams().get(tableNode.getIndexFieldsValuesParamName());
        assertNotNull(key);
        assertArrayEquals(new Object[]{1}, (Object[])key);
        List<String> masterValues =  RavenUtils.getMasterFieldValues(tableNode);
        assertNotNull(masterValues);
        assertEquals(2, masterValues.size());
        assertArrayEquals(new Object[]{"1", "2"}, masterValues.toArray());
    }

    @Test
    public void selectActionTest() throws Exception
    {
        UserContext userContext = new TestUserContext();
        UserContextServiceModule.setUserContext(userContext);

        tableNode.setEnableSelect(Boolean.TRUE);
        tableNode.setIndexFields("field1");

        Record rec = schema.createRecord();
        rec.setValue("field1", 1);
        rec.setValue("field2", 2);
        ds.addDataPortion(rec);
        rec = schema.createRecord();
        rec.setValue("field1", 2);
        rec.setValue("field2", 3);
        ds.addDataPortion(rec);

        ds.addDataPortion(null);

        List<ViewableObject> vos = tableNode.getViewableObjects(null);
        assertNotNull(vos);
        assertEquals(1, vos.size());

        Table table = (Table) vos.get(0).getData();
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertTrue(rows.get(0)[0] instanceof RecordsAsTableNode.SelectRowAction);
        ViewableObject action = (ViewableObject) rows.get(0)[0];
        assertNull(action.getData());
        Object key = userContext.getParams().get(tableNode.getIndexFieldsValuesParamName());
        assertNotNull(key);
        assertArrayEquals(new Object[]{1}, (Object[])key);

        vos = tableNode.getViewableObjects(null);
        assertNotNull(vos);
        assertEquals(1, vos.size());
        table = (Table) vos.get(0).getData();
        rows = RavenUtils.tableAsList(table);
        assertEquals(2, rows.size());
        assertTrue(table.containsRowTag(0, Table.SELECTED_TAG));
        assertFalse(table.containsRowTag(1, Table.SELECTED_TAG));
    }

    private void checkSessionAttribute(Map<String, NodeAttribute> attrs, String name, String value)
    {
        NodeAttribute attr = attrs.get(name);
        assertNotNull(attr);
        assertEquals(value, attr.getValue());
    }
}
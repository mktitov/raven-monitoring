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

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.ds.RecordException;
import org.raven.ds.impl.RecordsAsTableNode.DeleteRecordAction;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.RavenUtils;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.table.Table;
import org.raven.test.DataCollector;
import org.raven.tree.ActionViewableObject;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.ActionAttributeValueHandlerFactory;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.RefreshAttributeValueHandlerFactory;
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
        assertNotNull(refreshAttrs.get("field1"));
        assertEquals("field1 displayName", refreshAttrs.get("field1").getDisplayName());
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
}
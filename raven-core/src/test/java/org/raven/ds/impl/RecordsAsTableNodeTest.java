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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.PushOnDemandDataSource;
import org.raven.RavenCoreTestCase;
import org.raven.RavenUtils;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.table.Table;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
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

}
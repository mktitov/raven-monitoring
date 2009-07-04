/*
 *  Copyright 2008 Mikhail Titov.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.table.objects.TestTableDataSource;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.RefreshAttributeValueHandlerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class TableViewNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        TestTableDataSource tableDs = new TestTableDataSource();
        tableDs.setName("table data source");
        tableDs.save();
        tree.getRootNode().addChildren(tableDs);
        tableDs.init();
        tableDs.start();
        assertEquals(Status.STARTED, tableDs.getStatus());

        TableViewNode tableView = new TableViewNode();
        tableView.setName("table view");
        tableView.save();
        tree.getRootNode().addChildren(tableView);
        tableView.init();
        tableView.setDataSource(tableDs);
        NodeAttribute refAttr = new NodeAttributeImpl("refAttr", String.class, "test", null);
        refAttr.setValueHandlerType(RefreshAttributeValueHandlerFactory.TYPE);
        refAttr.setOwner(tableView);
        refAttr.save();
        tableView.addNodeAttribute(refAttr);
        refAttr.init();
        tableView.start();
        assertEquals(Status.STARTED, tableView.getStatus());

        Map<String, NodeAttribute> refreshAttributes = tableView.getRefreshAttributes();
        assertNotNull(refreshAttributes);
        assertEquals(1, refreshAttributes.size());
        NodeAttribute sessAttr = refreshAttributes.get("refAttr");
        assertNotNull(sessAttr);
        assertNotSame(refAttr, sessAttr);
        assertEquals("test", sessAttr.getValue());

        List<ViewableObject> objects = tableView.getViewableObjects(refreshAttributes);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.get(0);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, object.getMimeType());
        Object data = object.getData();
        assertTrue(data instanceof Table);
        Table table = (Table) data;

        List<Object[]> rows = new ArrayList<Object[]>();
        for (Iterator<Object[]> it=table.getRowIterator(); it.hasNext();)
            rows.add(it.next());

        assertEquals(2, rows.size());
        assertArrayEquals(new String[]{"col1", "col2"}, table.getColumnNames());
        assertArrayEquals(new Object[]{"val_1_1", "val_2_1"}, rows.get(0));
        assertArrayEquals(new Object[]{"val_1_2", "val_2_2"}, rows.get(1));
    }
}

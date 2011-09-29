/*
 *  Copyright 2011 Mikhail Titov.
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

package org.raven.tree.impl;

import org.raven.RavenUtils;
import java.util.List;
import org.raven.table.Table;
import java.util.Arrays;
import java.util.Collections;
import org.raven.tree.Node;
import org.junit.Test;
import org.raven.test.ServiceTestCase;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.weda.internal.services.MessagesRegistry;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class ChildsAsTableViewableObjectTest extends ServiceTestCase
{
    @Test
    public void noChildTest() throws Exception
    {
        Node owner = createMock(Node.class);
        expect(owner.getSortedChildrens()).andReturn(Collections.EMPTY_LIST);
        replay(owner);
        ChildsAsTableViewableObject vo = new ChildsAsTableViewableObject(owner, null, null);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vo.getMimeType());
        assertNull(vo.getData());
        verify(owner);
    }

    @Test
    public void withChildsTest() throws Exception
    {
        MessagesRegistry messages = registry.getService(MessagesRegistry.class);
        Node owner = createMock("owner", Node.class);
        Node child = createMock("child", Node.class);
        NodeAttribute attr = createMock(NodeAttribute.class);

        expect(owner.getSortedChildrens()).andReturn(Arrays.asList(child));
        expect(child.getPath()).andReturn("/node/child");
        expect(child.getName()).andReturn("child");
        expect(child.getNodeAttribute("attr")).andReturn(attr);
        expect(attr.getValue()).andReturn("value");

        replay(owner, child, attr);

        ChildsAsTableViewableObject vo = new ChildsAsTableViewableObject(
                owner, new String[]{"attr"}, new String[]{"attr title"});
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vo.getMimeType());
        Object data = vo.getData();
        assertTrue(data instanceof Table);
        Table table = (Table) data;
        String firstColName = messages.getMessages(ChildsAsTableViewableObject.class).get("nodeNameColumn");
        assertArrayEquals(new String[]{firstColName, "attr title"}, table.getColumnNames());
        
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertEquals(1, rows.size());

        Object obj = rows.get(0)[0];
        assertTrue(obj instanceof ViewableObject);
        ViewableObject nodeRef = (ViewableObject) obj;
        assertEquals(Viewable.RAVEN_NODE_MIMETYPE, nodeRef.getMimeType());
        assertEquals("child", nodeRef.toString());
        assertEquals("/node/child", nodeRef.getData());

        assertEquals("value", rows.get(0)[1]);

        verify(owner, child, attr);
    }
}
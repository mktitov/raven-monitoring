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

package org.raven;

import org.raven.table.ColumnGroup;
import java.util.List;
import org.raven.table.Table;
import java.util.Collection;
import org.raven.tree.NodeAttribute;
import java.util.Map;
import java.util.Arrays;
import org.raven.tree.Viewable;
import org.raven.tree.Node;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import org.junit.Test;
import org.raven.table.ColumnGroupTag;
import org.raven.table.TableImpl;
import org.raven.test.ServiceTestCase;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.NodeAttributeImpl;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class RavenUtilsTest extends ServiceTestCase
{
    @Test
    public void splitTest()
    {
        assertArrayEquals(new String[]{"1", "2"}, RavenUtils.split("1, 2 "));
    }

    @Test
    public void getTableColumnGroups1()
    {
        TableImpl table = new TableImpl(new String[]{"col1", "col2"});
        List<ColumnGroup> groups = RavenUtils.getTableColumnGroups(table);
        assertNotNull(groups);
        assertEquals(2, groups.size());
        ColumnGroup grp = groups.get(0);
        assertEquals("col1", grp.getGroupName());
        assertEquals(0, grp.getFromColumn());
        assertEquals(0, grp.getToColumn());
        assertEquals(0, grp.getColumnNames().size());
        grp = groups.get(1);
        assertEquals("col2", grp.getGroupName());
        assertEquals(1, grp.getFromColumn());
        assertEquals(1, grp.getToColumn());
        assertEquals(0, grp.getColumnNames().size());
    }

    @Test
    public void getTableColumnGroups2()
    {
        TableImpl table = new TableImpl(new String[]{"col1", "col2", "col3", "col4"});
        table.addColumnTag(1, new ColumnGroupTag("grp1", 1, 2));
        List<ColumnGroup> groups = RavenUtils.getTableColumnGroups(table);
        assertNotNull(groups);
        assertEquals(3, groups.size());
        ColumnGroup grp = groups.get(0);
        assertEquals("col1", grp.getGroupName());
        assertEquals(0, grp.getFromColumn());
        assertEquals(0, grp.getToColumn());
        assertEquals(0, grp.getColumnNames().size());
        grp = groups.get(1);
        assertEquals("grp1", grp.getGroupName());
        assertEquals(1, grp.getFromColumn());
        assertEquals(2, grp.getToColumn());
        assertEquals(2, grp.getColumnNames().size());
        assertArrayEquals(new String[]{"col2", "col3"}, grp.getColumnNames().toArray());
        grp = groups.get(2);
        assertEquals("col4", grp.getGroupName());
        assertEquals(3, grp.getFromColumn());
        assertEquals(3, grp.getToColumn());
        assertEquals(0, grp.getColumnNames().size());
    }

    @Test
    public void tableToHtml_groupsTest() throws Exception
    {
        TableImpl table = new TableImpl(new String[]{"col1","col2","col3"});
        table.addColumnTag(1, new ColumnGroupTag("grp1", 1, 2));

        String html = RavenUtils.tableToHtml(table, null).toString();
        assertEquals("<table><tr><th rowspan=\"2\">col1</th><th colspan=\"2\">grp1</th></tr><tr><th>col2</th><th>col3</th></tr></table>", html);
    }

    @Test
    public void tableToHtml_nbspTest() throws Exception
    {
        TableImpl table = new TableImpl(new String[]{"nbsp1", "nbsp2"});
        table.addRow(new Object[]{null, ""});

        String html = RavenUtils.tableToHtml(table, null).toString();
        assertNotNull(html);
        assertEquals("<table><tr><th>nbsp1</th><th>nbsp2</th></tr><tr><td>&nbsp;</td><td>&nbsp;</td></tr></table>", html);
    }
    
    @Test
    public void tableToHtml_numberTest() throws Exception
    {
        TableImpl table = new TableImpl(new String[]{"float", "double", "integer"});
        table.addRow(new Object[]{new Float(12.345), new Double(12.343), new Integer(123)});

        String html = RavenUtils.tableToHtml(table, null).toString();
        assertNotNull(html);
        assertEquals("<table><tr><th>float</th><th>double</th><th>integer</th></tr><tr><td>12.35</td><td>12.34</td><td>123</td></tr></table>", html);
    }

    @Test
    public void tableToHtml_datesTest() throws Exception
    {
        TableImpl table = new TableImpl(new String[]{"java.sql.Date", "java.sql.Time", "java.util.Date"});
        Date date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").parse("01.01.2010 01:02:03");
        table.addRow(new Object[]{new java.sql.Date(date.getTime()), new java.sql.Time(date.getTime()), date});

        String html = RavenUtils.tableToHtml(table, null).toString();
        assertNotNull(html);
        assertEquals("<table><tr><th>java.sql.Date</th><th>java.sql.Time</th><"
                + "th>java.util.Date</th></tr><tr><td>01.01.2010</td>"
                + "<td>01:02:03</td><td>01.01.2010 01:02:03</td></tr></table>", html);
    }

    @Test
    public void getSelfAndChildsRefreshAttributes_emptyResult() throws Exception
    {
        Node node = createMock(Node.class);
        expect(node.getSortedChildrens()).andReturn(Collections.EMPTY_LIST);
        replay(node);
        assertSame(Collections.EMPTY_MAP, RavenUtils.getSelfAndChildsRefreshAttributes(node));
        verify(node);
    }

    @Test
    public void getSelfAndChildsRefreshAttributes_notEmptyResult() throws Exception
    {
        Node node = createMock("node", Node.class);
        Viewable child = createMock("child", Viewable.class);
        expect(node.getSortedChildrens()).andReturn(Arrays.asList((Node)child));
        Map<String, NodeAttribute> attrs = new HashMap<String, NodeAttribute>();
        NodeAttribute attr = new NodeAttributeImpl("attr1", String.class, "v1", "d1");
        attrs.put("attr1", attr);
        expect(child.getRefreshAttributes()).andReturn(attrs);
        replay(node, child);

        Map<String, NodeAttribute> res = RavenUtils.getSelfAndChildsRefreshAttributes(node);
        assertNotNull(res);
        assertEquals(1, res.size());
        assertSame(attr, res.get("attr1"));
        
        verify(node, child);
    }

    @Test
    public void getSelfAndChildsViewableObjects_emptyResult() throws Exception
    {
        Node node = createMock(Node.class);
        expect(node.getSortedChildrens()).andReturn(Collections.EMPTY_LIST);
        replay(node);
        assertSame(Collections.EMPTY_LIST, RavenUtils.getSelfAndChildsViewableObjects(node, null));
        verify(node);
    }

    @Test
    public void getSelfAndChildsViewableObjects_notEmptyResult() throws Exception
    {
        Node node = createMock(Node.class);
        Viewable child = createMock("child", Viewable.class);
        ViewableObject vo = createMock(ViewableObject.class);

        Map<String, NodeAttribute> refreshAttributes = new HashMap<String, NodeAttribute>();
        expect(node.getSortedChildrens()).andReturn(Arrays.asList((Node)child));
        expect(child.getViewableObjects(refreshAttributes)).andReturn(Arrays.asList(vo));

        replay(node, child, vo);

        Collection<ViewableObject> vos = RavenUtils.getSelfAndChildsViewableObjects(node, refreshAttributes);
        assertNotNull(vos);
        assertEquals(1, vos.size());
        assertSame(vo, vos.iterator().next());

        verify(node, child, vo);
    }
}
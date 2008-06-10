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

import org.junit.Before;
import org.raven.*;
import org.junit.Test;
import org.raven.table.objects.ColumnValueDataConsumer;
import org.raven.table.objects.TestDataSource;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.constraints.ConstraintException;

/**
 *
 * @author Mikhail Titov
 */
public class TableNodeTest extends RavenCoreTestCase
{
    private TableNode table;
    
    @Before
    public void createTableNode()
    {
        store.removeNodes();
        tree.reloadTree();
        
        table = new TableNode();
        table.setName("tableNode");
        tree.getRootNode().addChildren(table);
        store.saveNode(table);
        table.init();
    }
    
    @Test
    public void createTest()
    {
        TableNodeTemplate template = (TableNodeTemplate) table.getChildren(TableNodeTemplate.NAME);
        assertNotNull(template);
        
        tree.reloadTree();
        
        Node loadedTable = (TableNode) tree.getNode(table.getPath());
        assertNotNull(loadedTable);
        
        Node loadedTemplate = tree.getNode(template.getPath());
        assertNotNull(loadedTemplate);
        assertEquals(template, loadedTemplate);
    }
    
    @Test
    public void addTableColumnNameAttributeTest()
    {
        TableNodeTemplate template = (TableNodeTemplate) table.getChildren(TableNodeTemplate.NAME);
        ContainerNode node = new ContainerNode("node");
        template.addChildren(node);
        store.saveNode(node);
        node.init();
        
        NodeAttribute attr = checkColumnNameAttribute(node, null);
        tree.reloadTree();
        checkColumnNameAttribute(node, attr);
    }
    
    @Test
    public void configureTest()
    {
        TestDataSource ds = new TestDataSource("dataSource");
        tree.getRootNode().addChildren(ds);
        store.saveNode(ds);
        ds.init();
        ds.start();
        
        TableNodeTemplate template = (TableNodeTemplate) table.getChildren(TableNodeTemplate.NAME);        
        ContainerNode node = new ContainerNode("${column1}");
        template.addChildren(node);
        store.saveNode(node);
        node.init();
        
        ContainerNode child = new ContainerNode("child");
        node.addChildren(child);
        store.saveNode(child);
        NodeAttribute attr = 
                new NodeAttributeImpl("${column2}", String.class, "test ${column1}", "${column1}");
        attr.setOwner(child);
        child.addNodeAttribute(attr);
        store.saveNodeAttribute(attr);
        
        table.setDataSource(ds);
        table.start();
        table.configure();
        
        checkNodes(table, Status.INITIALIZED);
        
        tree.reloadTree();
        
        checkNodes(table, Status.STARTED);
    }
    
    @Test
    public void setDataTest() throws ConstraintException, Exception
    {
        TestDataSource ds = new TestDataSource("dataSource");
        tree.getRootNode().addChildren(ds);
        store.saveNode(ds);
        ds.init();
        ds.start();
        
        table.setDataSource(ds);
        store.saveNode(table);
        
        TableNodeTemplate template = (TableNodeTemplate) table.getChildren(TableNodeTemplate.NAME);
        ColumnValueDataConsumer c1 = new ColumnValueDataConsumer();
        c1.setName("c1");
        template.addChildren(c1);
        store.saveNode(c1);
        c1.init();
        c1.setDataSource(table);
        store.saveNodeAttribute(c1.getNodeAttribute("dataSource"));
        
        ColumnValueDataConsumer c2 = new ColumnValueDataConsumer();
        c2.setName("c2");
        template.addChildren(c2);
        store.saveNode(c2);
        c2.init();
        c2.setDataSource(table);
        store.saveNodeAttribute(c2.getNodeAttribute("dataSource"));
        NodeAttribute colAttr = c2.getNodeAttribute(TableNodeTemplate.TABLE_COLUMN_NAME);
        assertNotNull(colAttr);
        colAttr.setValue("column1");
        store.saveNodeAttribute(colAttr);
        
        table.configure();
        
        ds.pushData();
        
        assertEquals(table, c1.sourceDataSource);
        assertTrue(c1.value instanceof Table);
        assertEquals(table, c2.sourceDataSource);
        assertEquals("value1_1", c2.value);
    }
    
    private void checkNodes(Node node, Status status)
    {
        Node table = tree.getNode(node.getPath());
        assertNotNull(table);
        
        checkRow(table, "1", status);
        checkRow(table, "2", status);
//        Node node = table.getChildren(name)
    }
    
    private void checkRow(Node table, String suffix, Status status)
    {
        Node node = table.getChildren("value1_"+suffix);
        assertNotNull(node);
        assertEquals(status, node.getStatus());
        
        Node child = node.getChildren("child");
        assertNotNull(child);
        assertEquals(status, child.getStatus());
        NodeAttribute attr = child.getNodeAttribute("value2_"+suffix);
        assertNotNull(attr);
        assertEquals("test value1_"+suffix, attr.getValue());
        assertEquals("value1_"+suffix, attr.getDescription());
    }

    private NodeAttribute checkColumnNameAttribute(ContainerNode node, NodeAttribute attr)
    {
        Node templateNode = tree.getNode(node.getPath());
        NodeAttribute columnNameAttribute = 
                templateNode.getNodeAttribute(TableNodeTemplate.TABLE_COLUMN_NAME);
        assertNotNull(columnNameAttribute);
        if (attr!=null)
            assertEquals(attr, columnNameAttribute);
        assertEquals(String.class, columnNameAttribute.getType());
        assertNotNull(columnNameAttribute.getDescription());
        assertFalse(columnNameAttribute.isRequired());
        
        return columnNameAttribute;
    }
}

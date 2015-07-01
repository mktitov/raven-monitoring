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

package org.raven.tree.impl;

import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.table.Table;
import org.raven.template.GroupNode;
import org.raven.tree.impl.objects.ColumnValueDataConsumer;
import org.raven.tree.impl.objects.TestDataSource;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.weda.constraints.ConstraintException;

/**
 *
 * @author Mikhail Titov
 */
public class NodeGeneratorNodeTest extends RavenCoreTestCase
{
    private NodeGeneratorNode table;
    
    @Before
    public void createTableNode()
    {
        store.removeNodes();
        tree.reloadTree();
        
        table = new NodeGeneratorNode();
        table.setName("tableNode");
        tree.getRootNode().addChildren(table);
        tree.saveNode(table);
        table.init();
    }
    
    @Test
    public void createTest() throws Exception
    {
        NodeGeneratorNodeTemplate template =
                (NodeGeneratorNodeTemplate) table.getChildren(NodeGeneratorNodeTemplate.NAME);
        assertNotNull(template);
        
        tree.reloadTree();
        
        Node loadedTable = (NodeGeneratorNode) tree.getNode(table.getPath());
        assertNotNull(loadedTable);
        
        Node loadedTemplate = tree.getNode(template.getPath());
        assertNotNull(loadedTemplate);
        assertEquals(template, loadedTemplate);
    }
    
    @Test
    public void addTableColumnNameAttributeTest() throws InvalidPathException
    {
        NodeGeneratorNodeTemplate template =
                (NodeGeneratorNodeTemplate) table.getChildren(NodeGeneratorNodeTemplate.NAME);
        ContainerNode node = new ContainerNode("node");
        template.addChildren(node);
        tree.saveNode(node);
        node.init();
        
        NodeAttribute columnNameAttr = 
                checkColumnNameAttribute(node, NodeGeneratorNodeTemplate.TABLE_COLUMN_NAME, null);
//        NodeAttribute indexNameAttr = 
//                checkColumnNameAttribute(node, TableNodeTemplate.TABLE_INDEX_COLUMN_NAME, null);
        tree.reloadTree();
        checkColumnNameAttribute(node, NodeGeneratorNodeTemplate.TABLE_COLUMN_NAME, columnNameAttr);
//        checkColumnNameAttribute(node, TableNodeTemplate.TABLE_INDEX_COLUMN_NAME, indexNameAttr);
    }
    
    @Test
    public void configureTest() throws Exception
    {
        TestDataSource ds = new TestDataSource("dataSource");
        tree.getRootNode().addChildren(ds);
        tree.saveNode(ds);
        ds.init();
        ds.start();
        
        NodeGeneratorNodeTemplate template =
                (NodeGeneratorNodeTemplate) table.getChildren(NodeGeneratorNodeTemplate.NAME);
        GroupNode node = new GroupNode();
        node.setName("group");
        template.addChildren(node);
        tree.saveNode(node);
        node.init();
        node.setGroupingExpression("row['column1']");
        node.getNodeAttribute(NodeGeneratorNodeTemplate.TABLE_COLUMN_NAME).setValue("column1");
        
        ContainerNode child = new ContainerNode("child");
        node.addChildren(child);
        tree.saveNode(child);
        NodeAttribute attr = 
                new NodeAttributeImpl("child_attr", String.class, "'test '+row['column1']", null);
        attr.setOwner(child);
        child.addNodeAttribute(attr);
        attr.setTemplateExpression(true);
        attr.init();
        tree.saveNodeAttribute(attr);
        
        table.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE).setValue(ds.getPath());
        table.getNodeAttribute(NodeGeneratorNode.INDEXCOLUMNNAME_ATTRIBUTE).setValue("column1");
        table.start();
        assertEquals(Status.STARTED, table.getStatus());
        
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
        tree.saveNode(ds);
        ds.init();
        ds.start();
        
        table.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE).setValue(ds.getPath());
        table.getNodeAttribute(NodeGeneratorNode.INDEXCOLUMNNAME_ATTRIBUTE).setValue("column1");
        tree.saveNode(table);
        table.start();
        assertEquals(Status.STARTED, table.getStatus());
        
        NodeGeneratorNodeTemplate template =
                (NodeGeneratorNodeTemplate) table.getChildren(NodeGeneratorNodeTemplate.NAME);
        
        ContainerNode node = new ContainerNode("^t 'node-'+row.column1");
        template.addChildren(node);
        tree.saveNode(node);
        node.init();
        
        ColumnValueDataConsumer c1 = new ColumnValueDataConsumer();
        c1.setName("c1");
        node.addChildren(c1);
        tree.saveNode(c1);
        c1.init();
        c1.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE)
                .setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        c1.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE).setValue(table.getPath());
        tree.saveNodeAttribute(c1.getNodeAttribute("dataSource"));
        
        ColumnValueDataConsumer c2 = new ColumnValueDataConsumer();
        c2.setName("c2");
        node.addChildren(c2);
        tree.saveNode(c2);
        c2.init();
//        c2.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE).setValue(table.getPath());
//        tree.saveNodeAttribute(c2.getNodeAttribute("dataSource"));
        NodeAttribute colAttr = c2.getNodeAttribute(NodeGeneratorNodeTemplate.TABLE_COLUMN_NAME);
        assertNotNull(colAttr);
        colAttr.setValue("column2");
        
        tree.saveNodeAttribute(colAttr);
        
        table.configure();
        
        ds.pushData();
        
        assertEquals(0, c1.executionCount);
        assertEquals(0, c2.executionCount);
        
        checkDataConsumers(table, 1, 1, 1);
        checkDataConsumers(table, 2, 1, 1);
        
        table.getNodeAttribute(NodeGeneratorNode.ADDPOLICY_ATTRIBUTE)
                .setValue(NodeGeneratorNode.AddPolicy.DO_NOTHING.toString());
        ds.pushDataWithNewRow();
        
        checkDataConsumers(table, 1, 2, 2);
        checkDataConsumers(table, 2, 2, 2);
        assertEquals(3, table.getChildrens().size());
        
        table.getNodeAttribute(NodeGeneratorNode.ADDPOLICY_ATTRIBUTE)
                .setValue(NodeGeneratorNode.AddPolicy.AUTO_ADD.toString());
        ds.pushDataWithNewRow();
        
        checkDataConsumers(table, 1, 3, 3);
        checkDataConsumers(table, 2, 3, 3);
        assertEquals(4, table.getChildrens().size());
        node = (ContainerNode) table.getChildren("node-value1_3");
        assertNotNull(node);
        assertEquals(Status.INITIALIZED, node.getStatus());
        tree.remove(node);
        
        table.getNodeAttribute(NodeGeneratorNode.ADDPOLICY_ATTRIBUTE)
                .setValue(NodeGeneratorNode.AddPolicy.AUTO_ADD_AND_START.toString());
        ds.pushDataWithNewRow();
        checkDataConsumers(table, 1, 4, 4);
        checkDataConsumers(table, 2, 4, 4);
        assertEquals(4, table.getChildrens().size());
        node = (ContainerNode) table.getChildren("node-value1_3");
        assertNotNull(node);
        assertEquals(Status.STARTED, node.getStatus());
        
        ds.pushDataWithNewRow();
        checkDataConsumers(table, 1, 5, 5);
        checkDataConsumers(table, 2, 5, 5);
        checkDataConsumers(table, 3, 1, 1);
        
        table.getNodeAttribute(NodeGeneratorNode.REMOVEPOLICY_ATTRIBUTE)
                .setValue(NodeGeneratorNode.RemovePolicy.DO_NOTHING.toString());
        ds.pushData();
        checkDataConsumers(table, 1, 6, 6);
        checkDataConsumers(table, 2, 6, 6);
        checkDataConsumers(table, 3, 2, 1);
        
        table.getNodeAttribute(NodeGeneratorNode.REMOVEPOLICY_ATTRIBUTE)
                .setValue(NodeGeneratorNode.RemovePolicy.STOP_NODE.toString());
        ds.pushData();
        checkDataConsumers(table, 1, 7, 7);
        checkDataConsumers(table, 2, 7, 7);
        checkDataConsumers(table, 3, 3, 1);
        node = (ContainerNode) table.getChildren("node-value1_3");
        assertNotNull(node);
        assertEquals(Status.INITIALIZED, node.getStatus());
        tree.start(node, false);
        
        table.getNodeAttribute(NodeGeneratorNode.REMOVEPOLICY_ATTRIBUTE)
                .setValue(NodeGeneratorNode.RemovePolicy.AUTO_REMOVE.toString());
        ds.pushData();
        checkDataConsumers(table, 1, 8, 8);
        checkDataConsumers(table, 2, 8, 8);
        node = (ContainerNode) table.getChildren("node-value1_3");
        assertNull(node);
        
    }

    @Test
    public void removeBeforeProcessingTest() throws Exception
    {
        TestDataSource ds = new TestDataSource("dataSource");
        tree.getRootNode().addChildren(ds);
        tree.saveNode(ds);
        ds.init();
        ds.start();
        
        table.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE).setValue(ds.getPath());
        table.getNodeAttribute(NodeGeneratorNode.INDEXCOLUMNNAME_ATTRIBUTE).setValue("column1");
        table.setAddPolicy(NodeGeneratorNode.AddPolicy.AUTO_ADD_AND_START);
        table.setRemovePolicy(NodeGeneratorNode.RemovePolicy.REMOVE_BEFORE_PROCESSING);
        tree.saveNode(table);
        table.start();
        assertEquals(Status.STARTED, table.getStatus());
        
        NodeGeneratorNodeTemplate template =
                (NodeGeneratorNodeTemplate) table.getChildren(NodeGeneratorNodeTemplate.NAME);
        
        ContainerNode node = new ContainerNode("^t 'node-'+row.column1");
        template.addChildren(node);
        tree.saveNode(node);
        node.init();

        ds.pushData();
        assertEquals(3, table.getChildrenCount());
        ds.pushDataWithOneRow();
        assertEquals(2, table.getChildrenCount());
        assertNotNull(table.getChildren("node-value1_3"));
    }

    @Test
    public void indexExpressionTest() throws Exception
    {
        TestDataSource ds = new TestDataSource("dataSource");
        tree.getRootNode().addChildren(ds);
        tree.saveNode(ds);
        ds.init();
        ds.start();
        
        table.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE).setValue(ds.getPath());
        table.getNodeAttribute(NodeGeneratorNode.INDEXCOLUMNNAME_ATTRIBUTE).setValue("column1");
        table.setIndexExpression("''+rownum");
        table.setAddPolicy(NodeGeneratorNode.AddPolicy.AUTO_ADD_AND_START);
        tree.saveNode(table);
        table.start();
        assertEquals(Status.STARTED, table.getStatus());
        
        NodeGeneratorNodeTemplate template =
                (NodeGeneratorNodeTemplate) table.getChildren(NodeGeneratorNodeTemplate.NAME);
        
        ContainerNode node = new ContainerNode("^t \"node-${row.column1}\"");
        template.addChildren(node);
        tree.saveNode(node);
        node.init();

        ds.pushDataWithOneRow();
        assertEquals(2, table.getChildrenCount());
        Node child = table.getChildren("node-value1_3");
        assertNotNull(child);
        NodeAttribute indexAttr = child.getNodeAttribute(NodeGeneratorNode.INDEX_COLUMN_VALUE);
        assertNotNull(indexAttr);
        assertEquals("1", indexAttr.getRealValue());
    }

    private void checkDataConsumers(
            NodeGeneratorNode tableRef, int row, int executionCount1, int executionCount2)
        throws InvalidPathException
    {
        NodeGeneratorNode table = (NodeGeneratorNode) tree.getNode(tableRef.getPath());
        assertNotNull(table);
        
        Node node = table.getChildren("node-value1_"+row);
        assertNotNull(node);
        
        ColumnValueDataConsumer c = (ColumnValueDataConsumer) node.getChildren("c1");
        assertNotNull(c);
        assertEquals(table, c.sourceDataSource);
        assertTrue(c.value instanceof Table);
        assertEquals(executionCount1, c.executionCount);
        
        c = (ColumnValueDataConsumer) node.getChildren("c2");
        assertNotNull(c);
        assertEquals(table, c.sourceDataSource);
        assertEquals("value2_"+row, c.value);
        assertEquals(executionCount2, c.executionCount);
    }
    
    private void checkNodes(Node node, Status status) throws InvalidPathException
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
        NodeAttribute columnNameAttr =
				node.getNodeAttribute(NodeGeneratorNodeTemplate.TABLE_COLUMN_NAME);
        assertNotNull(columnNameAttr);
        assertEquals("column1", columnNameAttr.getValue());
        
        Node child = node.getChildren("child");
        assertNotNull(child);
        assertEquals(status, child.getStatus());
        NodeAttribute indexAttr = child.getNodeAttribute(NodeGeneratorNode.INDEX_COLUMN_VALUE);
        assertNotNull(indexAttr);
        assertEquals("value1_"+suffix, indexAttr.getValue());
        assertNull(child.getNodeAttribute(NodeGeneratorNodeTemplate.TABLE_COLUMN_NAME));
        NodeAttribute attr = child.getNodeAttribute("child_attr");
        assertNotNull(attr);
        assertEquals("test value1_"+suffix, attr.getValue());
    }

    private NodeAttribute checkColumnNameAttribute(
            ContainerNode node, String attrName, NodeAttribute columnNameAttr) 
        throws InvalidPathException
    {
        Node templateNode = tree.getNode(node.getPath());
        NodeAttribute columnNameAttribute = 
                templateNode.getNodeAttribute(attrName);
        assertNotNull(columnNameAttribute);
        if (columnNameAttr!=null)
            assertEquals(columnNameAttr, columnNameAttribute);
        assertEquals(String.class, columnNameAttribute.getType());
        assertNotNull(columnNameAttribute.getDescription());
        assertFalse(columnNameAttribute.isRequired());
        
        return columnNameAttribute;
    }
    
//    private NodeAttribute checkIndexColumnNameAttribute(ContainerNode node, NodeAttribute indexAttr)
//    {
//        Node templateNode = tree.getNode(node.getPath());
//        NodeAttribute columnNameAttribute = 
//                templateNode.getNodeAttribute(TableNodeTemplate.TABLE_COLUMN_NAME);
//        assertNotNull(columnNameAttribute);
//        if (columnNameAttr!=null)
//            assertEquals(columnNameAttr, columnNameAttribute);
//        assertEquals(String.class, columnNameAttribute.getType());
//        assertNotNull(columnNameAttribute.getDescription());
//        assertFalse(columnNameAttribute.isRequired());
//    }
}

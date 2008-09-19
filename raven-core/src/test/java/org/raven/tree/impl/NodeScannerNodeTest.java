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

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.impl.RavenUtils;
import org.raven.table.Table;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.objects.NodeScannerNodeConsumer;

/**
 *
 * @author Mikhail Titov
 */
public class NodeScannerNodeTest extends RavenCoreTestCase
{
    private NodeScannerNode scanner;
    private ContainerNode nodeForScan;
    private NodeScannerNodeConsumer consumer;
    private ContainerNode node1,  node1_1,  node2;

    @Before
    public void beforeTest()
    {
        nodeForScan = new ContainerNode("nodeForScan");
        tree.getRootNode().addChildren(nodeForScan);
        nodeForScan.save();
        nodeForScan.init();
        nodeForScan.start();
        assertEquals(Status.STARTED, nodeForScan.getStatus());

        scanner = new NodeScannerNode();
        scanner.setName("scanner");
        tree.getRootNode().addChildren(scanner);
        scanner.save();
        scanner.init();
        scanner.setStartingPoint(nodeForScan);
        scanner.setSortByNodeWeight(false);
        scanner.setNodeFilter(true);
        scanner.setSubtreeFilter(true);

        consumer = new NodeScannerNodeConsumer();
        consumer.setName("consumer");
        tree.getRootNode().addChildren(consumer);
        consumer.save();
        consumer.init();
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());
        scanner.addDependentNode(consumer);

        node1 = new ContainerNode("node1");
        nodeForScan.addChildren(node1);
        node1.save();
        node1.init();
        node1.start();
        assertEquals(Status.STARTED, node1.getStatus());

        node1_1 = new ContainerNode("node1_1");
        node1.addChildren(node1_1);
        node1_1.save();
        node1_1.init();
        node1_1.start();
        assertEquals(Status.STARTED, node1_1.getStatus());

        node2 = new ContainerNode("node2");
        nodeForScan.addChildren(node2);
        node2.save();
        node2.init();
        node2.start();
        assertEquals(Status.STARTED, node2.getStatus());
    }

    @Test
    public void subtreeFilterTest() throws Exception
    {
        scanner.start();
        assertEquals(Status.STARTED, scanner.getStatus());
        scanner.scannNodes();

        assertSame(scanner, consumer.getDataSource());
        Object data = consumer.getData();
        assertNotNull(data);
        assertTrue(data instanceof Table);

        List<Object[]> rows = RavenUtils.tableAsList((Table) data);
        assertEquals(3, rows.size());
        assertSame(node1, rows.get(0)[0]);
        assertEquals(new Integer(0), rows.get(0)[1]);
        assertSame(node1_1, rows.get(1)[0]);
        assertEquals(new Integer(0), rows.get(1)[1]);
        assertSame(node2, rows.get(2)[0]);
        assertEquals(new Integer(0), rows.get(2)[1]);

        scanner.getNodeAttribute("subtreeFilter").setValue("scanningNode.name=='node2'");
        consumer.reset();
        scanner.scannNodes();
        assertSame(scanner, consumer.getDataSource());
        data = consumer.getData();
        assertNotNull(data);
        assertTrue(data instanceof Table);
        rows = RavenUtils.tableAsList((Table) data);
        assertEquals(1, rows.size());
        assertSame(node2, rows.get(0)[0]);
        assertEquals(new Integer(0), rows.get(0)[1]);
    }

    @Test
    public void nodeFilter_test() throws Exception
    {
        scanner.getNodeAttribute("nodeFilter").setValue("scanningNode.name!='node1'");
        scanner.scannNodes();
        assertSame(scanner, consumer.getDataSource());
        Object data = consumer.getData();
        assertNotNull(data);
        assertTrue(data instanceof Table);
        List<Object[]> rows = RavenUtils.tableAsList((Table) data);
        assertEquals(2, rows.size());
        assertSame(node1_1, rows.get(0)[0]);
        assertEquals(new Integer(0), rows.get(0)[1]);
        assertSame(node2, rows.get(1)[0]);
        assertEquals(new Integer(0), rows.get(1)[1]);
    }

    @Test
    public void nodeWeight_test() throws Exception
    {
        scanner.getNodeAttribute("nodeWeight").setValue("scanningNode.id");
        scanner.scannNodes();
        assertSame(scanner, consumer.getDataSource());
        Object data = consumer.getData();
        assertNotNull(data);
        assertTrue(data instanceof Table);
        List<Object[]> rows = RavenUtils.tableAsList((Table) data);
        assertEquals(3, rows.size());
        assertSame(node1, rows.get(0)[0]);
        assertEquals(node1.getId(), rows.get(0)[1]);
        assertSame(node1_1, rows.get(1)[0]);
        assertEquals(node1_1.getId(), rows.get(1)[1]);
        assertSame(node2, rows.get(2)[0]);
        assertEquals(node2.getId(), rows.get(2)[1]);
    }

    @Test
    public void sortByNodeWeight_test() throws Exception
    {
        String nodeWeightExpression =
                "switch(scanningNode.name) " +
                "{" +
                "  case 'node1': return 3;" +
                "  case 'node1_1' : return 1;" +
                "  case 'node2' : return 2;" +
                "}";
        scanner.getNodeAttribute("nodeWeight").setValue(nodeWeightExpression);
        scanner.setSortByNodeWeight(true);
        scanner.scannNodes();
        assertSame(scanner, consumer.getDataSource());
        Object data = consumer.getData();
        assertNotNull(data);
        assertTrue(data instanceof Table);
        List<Object[]> rows = RavenUtils.tableAsList((Table) data);
        assertEquals(3, rows.size());
        assertSame(node1_1, rows.get(0)[0]);
        assertEquals(1, rows.get(0)[1]);
        assertSame(node2, rows.get(1)[0]);
        assertEquals(2, rows.get(1)[1]);
        assertSame(node1, rows.get(2)[0]);
        assertEquals(3, rows.get(2)[1]);

        scanner.setReverseOrder(true);
        scanner.scannNodes();
        assertSame(scanner, consumer.getDataSource());
        data = consumer.getData();
        assertNotNull(data);
        assertTrue(data instanceof Table);
        rows = RavenUtils.tableAsList((Table) data);
        assertEquals(3, rows.size());
        assertSame(node1, rows.get(0)[0]);
        assertEquals(3, rows.get(0)[1]);
        assertSame(node2, rows.get(1)[0]);
        assertEquals(2, rows.get(1)[1]);
        assertSame(node1_1, rows.get(2)[0]);
        assertEquals(1, rows.get(2)[1]);
    }

    @Test
    public void maxRowCountTest() throws Exception
    {
        scanner.setMaxRowCount(2);
        scanner.scannNodes();
        assertSame(scanner, consumer.getDataSource());
        Object data = consumer.getData();
        assertNotNull(data);
        assertTrue(data instanceof Table);
        List<Object[]> rows = RavenUtils.tableAsList((Table) data);
        assertEquals(2, rows.size());
        assertSame(node1, rows.get(0)[0]);
        assertSame(node1_1, rows.get(1)[0]);

        String nodeWeightExpression =
                "switch(scanningNode.name) " +
                "{" +
                "  case 'node1': return 3;" +
                "  case 'node1_1' : return 1;" +
                "  case 'node2' : return 2;" +
                "}";
        scanner.getNodeAttribute("nodeWeight").setValue(nodeWeightExpression);
        scanner.setSortByNodeWeight(true);

        scanner.scannNodes();
        assertSame(scanner, consumer.getDataSource());
        data = consumer.getData();
        assertNotNull(data);
        assertTrue(data instanceof Table);
        rows = RavenUtils.tableAsList((Table) data);
        assertEquals(2, rows.size());
        assertSame(node1_1, rows.get(0)[0]);
        assertSame(node2, rows.get(1)[0]);
    }

    @Test
    public void includeAdditionalNodesTest() throws Exception
    {
        scanner.getNodeAttribute("includeAdditionalNodes").setValue(
                "scanningNode.name=='node1'? scanningNode.parent : null");
        scanner.scannNodes();
        assertSame(scanner, consumer.getDataSource());
        Object data = consumer.getData();
        assertNotNull(data);
        assertTrue(data instanceof Table);
        List<Object[]> rows = RavenUtils.tableAsList((Table) data);
        assertEquals(4, rows.size());
        assertSame(node1, rows.get(0)[0]);
        assertSame(nodeForScan, rows.get(1)[0]);
        assertSame(node1_1, rows.get(2)[0]);
        assertSame(node2, rows.get(3)[0]);

        scanner.setMaxRowCount(3);
        scanner.scannNodes();
        assertSame(scanner, consumer.getDataSource());
        data = consumer.getData();
        assertNotNull(data);
        assertTrue(data instanceof Table);
        rows = RavenUtils.tableAsList((Table) data);
        assertEquals(4, rows.size());
        assertSame(node1, rows.get(0)[0]);
        assertSame(nodeForScan, rows.get(1)[0]);
        assertSame(node1_1, rows.get(2)[0]);
        assertSame(node2, rows.get(3)[0]);
   }
}
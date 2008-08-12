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

package org.raven.rrd;

import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.rrd.data.RRArchive;
import org.raven.rrd.data.RRDNode;
import org.raven.rrd.data.RRDataSource;
import org.raven.rrd.graph.DataDefinition;
import org.raven.rrd.graph.RRArea;
import org.raven.rrd.graph.RRComment;
import org.raven.rrd.graph.RRDef;
import org.raven.rrd.graph.RRGraphNode;
import org.raven.rrd.graph.RRLine;
import org.raven.rrd.objects.TestDataSource2;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.impl.AttributeReferenceValueHandlerFactory;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class RRGraphManagerTest extends RavenCoreTestCase
{
    private ContainerNode dataSourcesNode;
    
    @Test
    public void syncTest() throws Exception
    {
        createTestDataSources();
        createRoundRobinDatabase();
        RRGraphManager gmanager = createGraphManager();
        assertNotNull(gmanager);
        
        gmanager.start();
        checkGraphManager(gmanager);
        
        tree.reloadTree();
        gmanager = (RRGraphManager) tree.getNode(gmanager.getPath());
        dataSourcesNode = (ContainerNode) tree.getNode(dataSourcesNode.getPath());
        checkGraphManager(gmanager);
    }
    
    @Test
    public void removeDataSourceTest() throws Exception
    {
        createTestDataSources();
        createRoundRobinDatabase();
        RRGraphManager gmanager = createGraphManager();
        assertNotNull(gmanager);
        gmanager.start();
        
        Node ds = dataSourcesNode.getChildren("ds_0");
        
        Node graphNode = gmanager.getChildren("group1").getChildren("graph");
        assertNotNull(graphNode.getChildren("def_"+ds.getId()));
        assertNotNull(graphNode.getChildren("comment_"+ds.getId()));
        
        tree.remove(ds);
        assertNull(graphNode.getChildren("def_"+ds.getId()));
        assertNull(graphNode.getChildren("comment_"+ds.getId()));
        
        assertEquals(3, graphNode.getChildrenCount());
        
        tree.reloadTree();
        gmanager = (RRGraphManager) tree.getNode(gmanager.getPath());
        graphNode = gmanager.getChildren("group1").getChildren("graph");
        assertEquals(3, graphNode.getChildrenCount());
        
        gmanager.stop();
        dataSourcesNode = (ContainerNode) tree.getNode(dataSourcesNode.getPath());
        ds = dataSourcesNode.getChildren("ds_1");
        tree.remove(ds);
        gmanager.start();
        graphNode = gmanager.getChildren("group1").getChildren("graph");
        assertNotNull(graphNode);
        assertEquals(0, graphNode.getChildrenCount());
    }
    
    @Test
    public void addDataSourceTest() throws Exception
    {
        createTestDataSources();
        RRDNode rrd = createRoundRobinDatabase();
        RRGraphManager gmanager = createGraphManager();
        assertNotNull(gmanager);
        gmanager.start();
        
        addTestDataSource(3);
        addRRDataSource(3, rrd);
        
        Node graphNode = gmanager.getChildren("group2").getChildren("graph");
        assertNotNull(graphNode);
        assertEquals(6, graphNode.getChildrenCount());
    }

    private TestDataSource2 addTestDataSource(int i) throws NodeError 
    {
        TestDataSource2 ds = new TestDataSource2();
        ds.setName("ds_" + i);
        dataSourcesNode.addChildren(ds);
        ds.save();
        ds.init();
        ds.start();
        assertEquals(Status.STARTED, ds.getStatus());
        
        return ds;
    }
    
    private void checkDataSourceInGraph(RRGraphNode node, DataSource ds, RRColor lineColor)
    {
        Node comment = node.getChildren("comment_"+ds.getId());
        assertNotNull(comment);
        assertTrue(comment instanceof RRComment);
        assertEquals(Status.STARTED, comment.getStatus());

        
        Node def = node.getChildren("def_"+ds.getId());
        assertNotNull(def);
        assertTrue(def instanceof RRDef);
        assertEquals(Status.STARTED, def.getStatus());
        RRDataSource rrds = ((RRDef)def).getDataSource();
        assertNotNull(rrds);
        assertSame(ds, rrds.getDataSource());
        
        Node line = node.getChildren("line_"+ds.getId());
        assertNotNull(line);
        assertTrue(line instanceof RRLine);
        assertEquals(Status.STARTED, line.getStatus());
        DataDefinition dataDef = ((RRLine)line).getDataDefinition();
        assertNotNull(dataDef);
        assertSame(def, dataDef);
        NodeAttribute colorAttr = line.getNodeAttribute(RRArea.COLOR_ATTRIBUTE);
        assertNull(colorAttr.getValueHandlerType());
        assertEquals(lineColor, colorAttr.getRealValue());
//        assertEquals(tree, this);
    }

    private void checkGraphManager(RRGraphManager gmanager) {

        //group1, group2 and Template
        assertEquals(3, gmanager.getChildrenCount());
        Node group = gmanager.getChildren("group1");
        assertNotNull(group);
        assertTrue(group instanceof ContainerNode);
        assertEquals(1, group.getChildrenCount());
        assertEquals(Status.STARTED, group.getStatus());

        Node gNode = group.getChildren("graph");
        assertTrue(gNode instanceof RRGraphNode);
        assertEquals(Status.STARTED, gNode.getStatus());
        assertNull(gNode.getNodeAttribute(GroupNode.GROUPINGEXPRESSION_ATTRIBUTE));
        assertEquals(6, gNode.getChildrenCount());
        checkDataSourceInGraph(
                (RRGraphNode) gNode, (DataSource) dataSourcesNode.getChildren("ds_0")
                , RRColor.BLACK);
        checkDataSourceInGraph(
                (RRGraphNode) gNode, (DataSource) dataSourcesNode.getChildren("ds_1")
                , RRColor.BLUE);

        group = gmanager.getChildren("group2");
        assertNotNull(group);
        assertTrue(group instanceof ContainerNode);
        assertEquals(Status.STARTED, group.getStatus());
        assertEquals(1, group.getChildrenCount());

        gNode = group.getChildren("graph");
        assertTrue(gNode instanceof RRGraphNode);
        assertEquals(Status.STARTED, gNode.getStatus());
        assertEquals(3, gNode.getChildrenCount());
        assertNull(gNode.getNodeAttribute(GroupNode.GROUPINGEXPRESSION_ATTRIBUTE));
        checkDataSourceInGraph(
                (RRGraphNode) gNode, (DataSource) dataSourcesNode.getChildren("ds_2")
                , RRColor.BLACK);
    }

    private RRGraphManager createGraphManager() throws NodeError, Exception {

        RRGraphManager gmanager = new RRGraphManager();
        gmanager.setName("graph manager");
        tree.getRootNode().addChildren(gmanager);
        gmanager.save();
        gmanager.init();

        NodeAttribute attr = gmanager.getNodeAttribute(RRGraphManager.STARTINGPOINT_ATTRIBUTE);
        attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        attr.setValue(dataSourcesNode.getPath());
        attr.save();

        attr = gmanager.getNodeAttribute(RRGraphManager.FILTER_EXPRESSION_ATTRIBUTE);
        attr.setValue("true");
        attr.save();
        
        //creating template
        RRGraphManagerTemplate template = gmanager.getTemplate();
        assertNotNull(template);
        GroupNode groupNode = new GroupNode();
        groupNode.setName("grouping-by-name");
        template.addChildren(groupNode);
        groupNode.save();
        groupNode.init();
        
        attr = groupNode.getNodeAttribute(GroupNode.GROUPINGEXPRESSION_ATTRIBUTE);
        attr.setValue("dataSource.name ==~ /.*[01]/? \"group1\" : \"group2\"");
        attr.save();
        
        RRGraphNode graphNode = new RRGraphNode();
        graphNode.setName("graph");
        groupNode.addChildren(graphNode);
        graphNode.save();
        graphNode.init();
        
        attr = graphNode.getNodeAttribute(GroupNode.GROUPINGEXPRESSION_ATTRIBUTE);
        //The RRGraphManagerTemplate must create this attribute
        assertNotNull(attr);
        assertEquals(ExpressionAttributeValueHandlerFactory.TYPE, attr.getValueHandlerType());
        attr.setValue("\"graph\"");
        attr.save();
        
        attr = graphNode.getNodeAttribute(RRGraphManagerTemplate.AUTOCOLOR_ATTRIBUTE);
        assertNotNull(attr);
        assertEquals(RRColor.BLACK, attr.getRealValue());
        
        RRDef def = new RRDef();
        def.setName("def");
        graphNode.addChildren(def);
        def.save();
        def.init();
        assertNull(def.getNodeAttribute(GroupNode.GROUPINGEXPRESSION_ATTRIBUTE));
        assertNull(def.getNodeAttribute(RRGraphManagerTemplate.AUTOCOLOR_ATTRIBUTE));
        
        RRComment comment = new RRComment();
        comment.setName("comment");
        graphNode.addChildren(comment);
        comment.save();
        comment.init();
        assertNull(comment.getNodeAttribute(GroupNode.GROUPINGEXPRESSION_ATTRIBUTE));
        assertNull(comment.getNodeAttribute(RRGraphManagerTemplate.AUTOCOLOR_ATTRIBUTE));
        
        RRLine line = new RRLine();
        line.setName("line");
        graphNode.addChildren(line);
        line.save();
        line.init();
        attr = line.getNodeAttribute(RRArea.DATADEFINITION_ATTRIBUTE);
        assertNotNull(attr);
        attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        attr.setValue("../def");
        attr.save();
        attr = line.getNodeAttribute(RRArea.COLOR_ATTRIBUTE);
        attr.setValueHandlerType(AttributeReferenceValueHandlerFactory.TYPE);
        attr.setValue("../@"+RRGraphManagerTemplate.AUTOCOLOR_ATTRIBUTE);
        assertTrue(attr.isExpressionValid());
        
        return gmanager;
    }

    private void addRRDataSource(int i, RRDNode rrd) throws Exception, NodeError 
    {
        RRDataSource rrds = new RRDataSource();
        rrds.setName("rrds_" + i);
        rrd.addChildren(rrds);
        rrds.save();
        rrds.init();
        NodeAttribute attr = rrds.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE);
        attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        attr.setValue(dataSourcesNode.getChildren("ds_" + i).getPath());
        attr.save();
    }

    private RRDNode createRoundRobinDatabase() throws Exception 
    {
        RRDNode rrd = new RRDNode();
        rrd.setName("Round robin database");
        tree.getRootNode().addChildren(rrd);
        rrd.save();
        rrd.init();
        
        //creating rr dataSource and archives
        RRArchive rra = new RRArchive();
        rra.setName("archive");
        rrd.addChildren(rra);
        rra.save();
        rra.init();
        rra.getNodeAttribute(RRArchive.ROWS_ATTRIBUTE).setValue("100");

        for (int i=0; i<3; ++i)
        {
            addRRDataSource(i, rrd);
        }
        
        tree.start(rrd, false);
        
        return rrd;
    }

    private void createTestDataSources() 
    {
        dataSourcesNode = new ContainerNode("data-sources");
        tree.getRootNode().addChildren(dataSourcesNode);
        dataSourcesNode.save();
        dataSourcesNode.init();
        dataSourcesNode.start();
        
        for (int i=0; i<3; ++i)
        {
            addTestDataSource(i);
        }
    }
}

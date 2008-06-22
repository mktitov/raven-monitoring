/*
 *  Copyright 2008 Milhail Titov.
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

package org.raven.rrd.graph;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.tapestry.ioc.RegistryBuilder;
import org.jrobin.core.Util;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.ServiceTestCase;
import org.raven.conf.Configurator;
import org.raven.ds.DataSource;
import org.raven.rrd.ConsolidationFunction;
import org.raven.rrd.RRColor;
import org.raven.rrd.data.RRArchive;
import org.raven.rrd.data.RRDNode;
import org.raven.rrd.data.RRDataSource;
import org.raven.rrd.objects.TestDataSource;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.raven.tree.store.TreeStore;
import org.weda.constraints.ConstraintException;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class RRGraphNodeTest extends ServiceTestCase
{
    private Configurator configurator;
    private TreeStore treeStore;
    private Tree tree;
    private TypeConverter converter;
    
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Before
    public void initTest()
    {
        configurator = registry.getService(Configurator.class);
        assertNotNull(configurator);
        
        treeStore = configurator.getTreeStore();
        treeStore.removeNodes();
        tree = registry.getService(Tree.class);
        converter = registry.getService(TypeConverter.class);
        assertNotNull(tree);
    }

    @Test
    public void render() throws Exception 
    {
        TestDataSource ds = new TestDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addChildren(ds);
        treeStore.saveNode(ds);
        ds.init();
        
        RRDNode rrd = new RRDNode();
        rrd.setName("rrd");
        tree.getRootNode().addChildren(rrd);
        treeStore.saveNode(rrd);
        rrd.init();
        NodeAttribute attr = rrd.getNodeAttribute("step");
        attr.setValue("1");
        treeStore.saveNodeAttribute(attr);
        assertEquals(Status.INITIALIZED, rrd.getStatus());
        
        RRDataSource rrds = createRRDataSource("ds", rrd, ds);
        RRDataSource rrds2 = createRRDataSource("ds2", rrd, ds);
        
        RRArchive rra = new RRArchive();
        rra.setName("archive");
        rrd.addChildren(rra);
        treeStore.saveNode(rra);
        rra.init();
        attr = rra.getNodeAttribute("rows");
        attr.setValue("100");
        treeStore.saveNodeAttribute(attr);
        rra.start();
        assertEquals(Status.STARTED, rra.getStatus());
        
        RRGraphNode graph = createGraphNode(rrds, rrds2);
        long start = Util.getTime()-1;
        graph.setStartTime(""+start);
      
        rrd.start();
        assertEquals(Status.STARTED, rrd.getStatus());
        ds.start();
        assertEquals(Status.STARTED, ds.getStatus());
        
        
        TimeUnit.SECONDS.sleep(5);
        
        File file1 = new File("target/g1.png");
        File file2 = new File("target/g2.png");
                
        IOUtils.copy(graph.render(null, null), new FileOutputStream(file1));
        
        TimeUnit.SECONDS.sleep(5);
        
        IOUtils.copy(graph.render(null, null), new FileOutputStream(file2));
    }

    private RRGraphNode createGraphNode(RRDataSource rrds, RRDataSource rrds2)
    {
        RRGraphNode gnode = new RRGraphNode();
        gnode.setName("graph");
        tree.getRootNode().addChildren(gnode);
        treeStore.saveNode(gnode);
        gnode.init();
        
        RRDef def = new RRDef();
        def.setName("ds");
        gnode.addChildren(def);
        treeStore.saveNode(def);
        def.init();
        def.setConsolidationFunction(ConsolidationFunction.AVERAGE);
        def.setDataSource(rrds);
        def.start();
        assertEquals(Status.STARTED, def.getStatus());
        
        RRDef def2 = new RRDef();
        def2.setName("ds2");
        gnode.addChildren(def2);
        treeStore.saveNode(def2);
        def2.init();
        def2.setConsolidationFunction(ConsolidationFunction.AVERAGE);
        def2.setDataSource(rrds2);
        def2.start();
        assertEquals(Status.STARTED, def2.getStatus());
        
        RRCDef cdef = new RRCDef();
        cdef.setName("cdef");
        gnode.addChildren(cdef);
        treeStore.saveNode(cdef);
        cdef.init();
        cdef.setExpression("ds2,30,-");
        cdef.start();
        assertEquals(Status.STARTED, cdef.getStatus());
        
        RRCDef cdef2 = new RRCDef();
        cdef2.setName("cdef2");
        gnode.addChildren(cdef2);
        treeStore.saveNode(cdef2);
        cdef2.init();
        cdef2.setExpression("0,10,+");
        cdef2.start();
        assertEquals(Status.STARTED, cdef2.getStatus());
        
        assertEquals(Status.STARTED, cdef.getStatus());
        RRArea area = new RRArea();
        area.setName("area");
        gnode.addChildren(area);
        treeStore.saveNode(area);
        area.init();
        area.setColor(RRColor.GREEN);
        area.setDataDefinition(cdef);
        area.setLegend("cdef legend");
        area.start();
        assertEquals(Status.STARTED, area.getStatus());
        
        RRLine line = new RRLine();
        line.setName("line1");
        gnode.addChildren(line);
        treeStore.saveNode(line);
        line.init();
        line.setColor(RRColor.BLUE);
        line.setDataDefinition(def);
        line.setLegend("line1 legend");
        line.setWidth(1f);
        line.start();
        assertEquals(Status.STARTED, line.getStatus());
        
        RRStack stack = new RRStack();
        stack.setName("stack");
        gnode.addChildren(stack);
        treeStore.saveNode(stack);
        stack.init();
        stack.setColor(RRColor.ORANGE);
        stack.setDataDefinition(cdef2);
        stack.setLegend("Stack legend");
        stack.start();
        assertEquals(Status.STARTED, stack.getStatus());
        
        RRLine line2 = new RRLine();
        line2.setName("line2");
        gnode.addChildren(line2);
        treeStore.saveNode(line2);
        line2.init();
        line2.setColor(RRColor.RED);
        line2.setDataDefinition(def2);
        line2.setLegend("line2 legend\\r");
        line2.setWidth(2f);
        line2.start();
        assertEquals(Status.STARTED, line2.getStatus());
        
        RRHRule hrule = new RRHRule();
        hrule.setName("hrule");
        gnode.addChildren(hrule);
        treeStore.saveNode(hrule);
        hrule.init();
        hrule.setValue(50.);
        hrule.setColor(RRColor.DARK_GRAY);
        hrule.setLegend("hrule legend\\r");
        hrule.setWidth(3f);
        hrule.start();
        assertEquals(Status.STARTED, hrule.getStatus());
        
        RRGPrint gprint = new RRGPrint();
        gprint.setName("gprint");
        gnode.addChildren(gprint);
        treeStore.saveNode(gprint);
        gprint.init();
        gprint.setDataDefinition(def2);
        gprint.setConsolidationFunction(ConsolidationFunction.AVERAGE);
        gprint.setFormat("average = %3.2f%s\\r");
        gprint.start();
        assertEquals(Status.STARTED, gprint.getStatus());
        
        RRComment comment = new RRComment();
        comment.setName("comment1");
        gnode.addChildren(comment);
        treeStore.saveNode(comment);
        comment.init();
        comment.setComment("The first comment");
        comment.start();
        assertEquals(Status.STARTED, comment.getStatus());
        
        comment = new RRComment();
        comment.setName("comment2");
        gnode.addChildren(comment);
        treeStore.saveNode(comment);
        comment.init();
        comment.setComment("The second comment");
        comment.start();
        assertEquals(Status.STARTED, comment.getStatus());
        
        gnode.setHeight(600);
        gnode.setWidth(900);
        gnode.setTitle("The title of a graph");
        gnode.setVerticalLabel("vertical label");
        gnode.setMaxValue(150.);
        gnode.setMinValue(-10.);
        gnode.start();
        assertEquals(Status.STARTED, gnode.getStatus());
        
        return gnode;
    }
    
    private RRDataSource createRRDataSource(String name, RRDNode rrd, DataSource ds) 
            throws Exception
    {
        RRDataSource rrds = new RRDataSource();
        rrds.setName(name);
        rrd.addChildren(rrds);
        treeStore.saveNode(rrds);
        rrds.init();
        NodeAttribute attr = rrds.getNodeAttribute("dataSource");
        attr.setValue(ds.getPath());
        treeStore.saveNodeAttribute(attr);
        attr = rrds.getNodeAttribute("interval");
        attr.setValue("1");
        treeStore.saveNodeAttribute(attr);
        attr = rrds.getNodeAttribute("intervalUnit");
        attr.setValue(TimeUnit.SECONDS.toString());
        treeStore.saveNodeAttribute(attr);
        attr = rrds.getNodeAttribute("dataSourceType");
        attr.setValue("GAUGE");
        treeStore.saveNodeAttribute(attr);
                
        rrds.start();
        assertEquals(Status.STARTED, rrds.getStatus());
        
        return rrds;
    }
}

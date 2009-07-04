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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.jrobin.core.Util;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.DataSource;
import org.raven.rrd.ConsolidationFunction;
import org.raven.rrd.RRColor;
import org.raven.rrd.RRIoQueueNode;
import org.raven.rrd.data.RRArchive;
import org.raven.rrd.data.RRDNode;
import org.raven.rrd.data.RRDataSource;
import org.raven.rrd.objects.TestDataSource;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class RRGraphNodeTest extends RavenCoreTestCase
{
    @Test
    public void render() throws Exception 
    {
        TestDataSource ds = new TestDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addChildren(ds);
        tree.saveNode(ds);
        ds.init();

		RRIoQueueNode queueNode = new RRIoQueueNode();
		queueNode.setName("ioqueue");
		tree.getRootNode().addChildren(queueNode);
		queueNode.save();
		queueNode.init();
		queueNode.setCorePoolSize(1);
		queueNode.setMaximumPoolSize(1);
		queueNode.start();
		assertEquals(Status.STARTED, queueNode.getStatus());

        RRDNode rrd = new RRDNode();
        rrd.setName("rrd");
        tree.getRootNode().addChildren(rrd);
        tree.saveNode(rrd);
        rrd.init();
		rrd.setIoQueue(queueNode);
        NodeAttribute attr = rrd.getNodeAttribute("step");
        attr.setValue("1");
        tree.saveNodeAttribute(attr);
        assertEquals(Status.INITIALIZED, rrd.getStatus());
        
        RRDataSource rrds = createRRDataSource("ds", rrd, ds);
        RRDataSource rrds2 = createRRDataSource("ds2", rrd, ds);
        
        RRArchive rra = new RRArchive();
        rra.setName("archive");
        rrd.addChildren(rra);
        tree.saveNode(rra);
        rra.init();
        attr = rra.getNodeAttribute("rows");
        attr.setValue("100");
        tree.saveNodeAttribute(attr);
        rra.start();
        assertEquals(Status.STARTED, rra.getStatus());
        
        RRGraphNode graph = createGraphNode(rrds, rrds2);
        long start = Util.getTime()-1;
        graph.getNodeAttribute(RRGraphNode.STARTIME_ATTRIBUTE).setValue(""+start);
      
        rrd.start();
        assertEquals(Status.STARTED, rrd.getStatus());
        ds.start();
        assertEquals(Status.STARTED, ds.getStatus());
        
        
        TimeUnit.SECONDS.sleep(5);
        
        File file1 = new File("target/g1.png");
        File file2 = new File("target/g2.png");

        Map<String, NodeAttribute> attrs = graph.getRefreshAttributes();
        List<ViewableObject> objects = graph.getViewableObjects(attrs);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        IOUtils.copy(new ByteArrayInputStream((byte[])objects.get(0).getData()), new FileOutputStream(file1));
        
        TimeUnit.SECONDS.sleep(5);
        
        IOUtils.copy(new ByteArrayInputStream((byte[])objects.get(0).getData()), new FileOutputStream(file1));
    }

    private RRGraphNode createGraphNode(RRDataSource rrds, RRDataSource rrds2) throws Exception
    {
        RRGraphNode gnode = new RRGraphNode();
        gnode.setName("graph");
        tree.getRootNode().addChildren(gnode);
        tree.saveNode(gnode);
        gnode.init();
        
        RRDef def = new RRDef();
        def.setName("ds");
        gnode.addChildren(def);
        tree.saveNode(def);
        def.init();
        def.getNodeAttribute(RRDef.CONSOLIDATIONFUNCTION_ATTRIBUTE).setValue("AVERAGE");
        def.getNodeAttribute(RRDef.DATASOURCE_ATTRIBUTE).setValue(rrds.getPath());
        def.start();
        assertEquals(Status.STARTED, def.getStatus());
        
        RRDef def2 = new RRDef();
        def2.setName("ds2");
        gnode.addChildren(def2);
        tree.saveNode(def2);
        def2.init();
        def2.getNodeAttribute(RRDef.CONSOLIDATIONFUNCTION_ATTRIBUTE).setValue("AVERAGE");
        def2.getNodeAttribute(RRDef.DATASOURCE_ATTRIBUTE).setValue(rrds2.getPath());
        def2.start();
        assertEquals(Status.STARTED, def2.getStatus());
        
        RRCDef cdef = new RRCDef();
        cdef.setName("cdef");
        gnode.addChildren(cdef);
        tree.saveNode(cdef);
        cdef.init();
        cdef.getNodeAttribute(RRCDef.EXPRESSION_ATTRIBUTE).setValue("ds2,30,-");
        cdef.start();
        assertEquals(Status.STARTED, cdef.getStatus());
        
        RRCDef cdef2 = new RRCDef();
        cdef2.setName("cdef2");
        gnode.addChildren(cdef2);
        tree.saveNode(cdef2);
        cdef2.init();
        cdef2.getNodeAttribute(RRCDef.EXPRESSION_ATTRIBUTE).setValue("0,10,+");
        cdef2.start();
        assertEquals(Status.STARTED, cdef2.getStatus());
        
        assertEquals(Status.STARTED, cdef.getStatus());
        RRArea area = new RRArea();
        area.setName("area");
        gnode.addChildren(area);
        tree.saveNode(area);
        area.init();
        area.getNodeAttribute(RRArea.COLOR_ATTRIBUTE).setValue(RRColor.GREEN.toString());
        area.getNodeAttribute(RRArea.DATADEFINITION_ATTRIBUTE).setValue(cdef.getPath());
        area.getNodeAttribute(RRArea.LEGEND_ATTRIBUTE).setValue("cdef legend");
        area.start();
        assertEquals(Status.STARTED, area.getStatus());
        
        RRLine line = new RRLine();
        line.setName("line1");
        gnode.addChildren(line);
        tree.saveNode(line);
        line.init();
        line.getNodeAttribute(RRLine.COLOR_ATTRIBUTE).setValue(RRColor.BLUE.toString());
        line.getNodeAttribute(RRLine.DATADEFINITION_ATTRIBUTE).setValue(def.getPath());
        line.getNodeAttribute(RRLine.LEGEND_ATTRIBUTE).setValue("line1 legend");
        line.getNodeAttribute(RRLine.WIDTH_ATTRIBUTE).setValue("1f");
        line.start();
        assertEquals(Status.STARTED, line.getStatus());
        
        RRStack stack = new RRStack();
        stack.setName("stack");
        gnode.addChildren(stack);
        tree.saveNode(stack);
        stack.init();
        stack.getNodeAttribute(RRArea.COLOR_ATTRIBUTE).setValue(RRColor.ORANGE.toString());
        stack.getNodeAttribute(RRArea.DATADEFINITION_ATTRIBUTE).setValue(cdef2.getPath());
        stack.getNodeAttribute(RRArea.LEGEND_ATTRIBUTE).setValue("Stack legend");
        stack.start();
        assertEquals(Status.STARTED, stack.getStatus());
        
        RRLine line2 = new RRLine();
        line2.setName("line2");
        gnode.addChildren(line2);
        tree.saveNode(line2);
        line2.init();
        line2.getNodeAttribute(RRArea.COLOR_ATTRIBUTE).setValue(RRColor.RED.toString());
        line2.getNodeAttribute(RRArea.DATADEFINITION_ATTRIBUTE).setValue(def2.getPath());
        line2.getNodeAttribute(RRArea.LEGEND_ATTRIBUTE).setValue("line2 legend\\r");
        line2.getNodeAttribute(RRLine.WIDTH_ATTRIBUTE).setValue("2f");
        line2.start();
        assertEquals(Status.STARTED, line2.getStatus());
        
        RRHRule hrule = new RRHRule();
        hrule.setName("hrule");
        gnode.addChildren(hrule);
        tree.saveNode(hrule);
        hrule.init();
        hrule.getNodeAttribute(RRHRule.VALUE_ATTRIBUTE).setValue("50");
        hrule.getNodeAttribute(RRHRule.COLOR_ATTRIBUTE).setValue(RRColor.DARK_GRAY.toString());
        hrule.getNodeAttribute(RRHRule.LEGEND_ATTRIBUTE).setValue("hrule legend\\r");
        hrule.getNodeAttribute(RRHRule.WIDTH_ATTRIBUTE).setValue("3");
        hrule.start();
        assertEquals(Status.STARTED, hrule.getStatus());
        
        RRGPrint gprint = new RRGPrint();
        gprint.setName("gprint");
        gnode.addChildren(gprint);
        tree.saveNode(gprint);
        gprint.init();
        gprint.getNodeAttribute(RRGPrint.DATADEFINITION_ATTRIBUTE).setValue(def2.getPath());
        gprint.getNodeAttribute(RRGPrint.CONSOLIDATIONFUNCTION_ATTRIBUTE)
                .setValue(ConsolidationFunction.AVERAGE.toString());
        gprint.getNodeAttribute(RRGPrint.FORMAT_ATTRIBUTE).setValue("average = %3.2f%s\\r");
        gprint.start();
        assertEquals(Status.STARTED, gprint.getStatus());
        
        RRComment comment = new RRComment();
        comment.setName("comment1");
        gnode.addChildren(comment);
        tree.saveNode(comment);
        comment.init();
        comment.getNodeAttribute(RRComment.COMMENT_ATTRIBUTE).setValue("The first comment");
        comment.start();
        assertEquals(Status.STARTED, comment.getStatus());
        
        comment = new RRComment();
        comment.setName("comment2");
        gnode.addChildren(comment);
        tree.saveNode(comment);
        comment.init();
        comment.getNodeAttribute(RRComment.COMMENT_ATTRIBUTE).setValue("The second comment");
        comment.start();
        assertEquals(Status.STARTED, comment.getStatus());
        
        gnode.getNodeAttribute(RRGraphNode.HEIGHT_ATTRIBUTE).setValue("600");
        gnode.getNodeAttribute(RRGraphNode.WIDTH_ATTRIBUTE).setValue("900");
        gnode.getNodeAttribute(RRGraphNode.TITLE_ATTRIBUTE).setValue("The title of a graph");
        gnode.getNodeAttribute(RRGraphNode.VERTICALLABEL_ATTRIBUTE).setValue("vertical label");
        gnode.getNodeAttribute(RRGraphNode.MAXVALUE_ATTRIBUTE).setValue("150.");
        gnode.getNodeAttribute(RRGraphNode.MINVALUE_ATTRIBUTE).setValue("-10.");
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
        tree.saveNode(rrds);
        rrds.init();
        NodeAttribute attr = rrds.getNodeAttribute("dataSource");
        attr.setValue(ds.getPath());
        tree.saveNodeAttribute(attr);
        attr = rrds.getNodeAttribute("interval");
        attr.setValue("1");
        tree.saveNodeAttribute(attr);
        attr = rrds.getNodeAttribute("intervalUnit");
        attr.setValue(TimeUnit.SECONDS.toString());
        tree.saveNodeAttribute(attr);
        attr = rrds.getNodeAttribute("dataSourceType");
        attr.setValue("GAUGE");
        tree.saveNodeAttribute(attr);
                
        rrds.start();
        assertEquals(Status.STARTED, rrds.getStatus());
        
        return rrds;
    }
}

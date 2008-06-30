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

package org.raven;

import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.tapestry.ioc.RegistryBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.raven.conf.Configurator;
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.rrd.ConsolidationFunction;
import org.raven.rrd.RRColor;
import org.raven.rrd.data.RRArchive;
import org.raven.rrd.data.RRDNode;
import org.raven.rrd.data.RRDataSource;
import org.raven.rrd.graph.RRDef;
import org.raven.rrd.graph.RRGraphNode;
import org.raven.rrd.graph.RRLine;
import org.raven.snmp.SnmpNode;
import org.raven.tree.Node.Status;
import org.raven.tree.Tree;
import org.raven.tree.store.TreeStore;
import org.weda.constraints.ConstraintException;

/**
 *
 * @author Mikhail Titov
 */
public class RealTest extends ServiceTestCase
{
    private Tree tree;
    private TreeStore store;
    
    private SnmpNode snmp;
    private RRDNode rrd;
    private RRGraphNode cpuLoad;
    private RRGraphNode cpuUsage;
    private RRGraphNode ramUsage;
    
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Before
    public void initTest()
    {
        tree = registry.getService(Tree.class);
        store = registry.getService(Configurator.class).getTreeStore();
        
        store.removeNodes();
    }
    
    @Ignore
    @Test
    public void test() throws Exception
    {
        createSnmpDataSource();
        createRrdNode();
        creatGraphs();
        
        while(true)
        {
            TimeUnit.SECONDS.sleep(60);
            saveGraph("target/cpuLoad.png", cpuLoad);
            saveGraph("target/cpuUsage.png", cpuUsage);
            saveGraph("target/ramUsage.png", ramUsage);
        }
    }
    
    private void saveGraph(String filename, RRGraphNode gr) throws Exception
    {
        FileOutputStream file = new FileOutputStream(filename);
        IOUtils.copy(gr.render(null, null), file);
        file.close();
    }

    private void addRRDataSource(String name, String oid) throws Exception
    {
        RRDataSource rrs = new RRDataSource();
        rrs.setName(name);
        rrd.addChildren(rrs);
        store.saveNode(rrs);
        rrs.init();
        rrs.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE).setValue(snmp.getPath());
        rrs.getNodeAttribute(AbstractDataSource.INTERVAL_ATTRIBUTE).setValue("1");
        rrs.getNodeAttribute(SnmpNode.HOST_ATTR).setValue("localhost");
        rrs.getNodeAttribute(SnmpNode.OID_ATTR).setValue(oid);
        
        rrs.getNodeAttribute(RRDataSource.DATASOURCETYPE_ATTRIBUTE).setValue("GAUGE");
        rrs.start();
        assertEquals(Status.STARTED, rrs.getStatus());
    }

    private void creatGraphs() throws Exception
    {
        cpuLoad = createGraph("cpu-load-graph", 0, null, "cpu-load1", "cpu-load5", "cpu-load15");
        cpuUsage = createGraph("cpu-usage-graph", 0, null,  "cpu-user%", "cpu-system%", "cpu-idle%");
        ramUsage = createGraph(
                "memory-usage-graph", 3, "Mb", "ram-total", "ram-used", "ram-free"
                , "ram-total-swap", "ram-available-swap", "ram-shared", "ram-buffered"
                , "ram-cached");
    }
    
    private RRGraphNode createGraph(
            String name, int unitsExponent, String unit, String... dataSources) throws Exception
    {
        RRGraphNode gr = new RRGraphNode();
        gr.setName(name);
        tree.getRootNode().addChildren(gr);
        store.saveNode(gr);
        gr.init();
        
        gr.getNodeAttribute(RRGraphNode.TITLE_ATTRIBUTE).setValue(name);
        gr.getNodeAttribute(RRGraphNode.HEIGHT_ATTRIBUTE).setValue("600");
        gr.getNodeAttribute(RRGraphNode.WIDTH_ATTRIBUTE).setValue("900");
        gr.getNodeAttribute(RRGraphNode.UNIT_ATTRIBUTE).setValue(unit);
        gr.getNodeAttribute(RRGraphNode.UNITEXPONENT_ATTRIBUTE).setValue(""+unitsExponent);
        
        int cInd=0;
        for (String dsName: dataSources)
        {
            RRDef def = new RRDef();
            def.setName(dsName);
            gr.addChildren(def);
            store.saveNode(def);
            def.init();
            def.getNodeAttribute(RRDef.DATASOURCE_ATTRIBUTE)
                    .setValue(rrd.getChildren(dsName).getPath());
            def.start();
            assertEquals(Status.STARTED, def.getStatus());
            
            RRLine line = new RRLine();
            line.setName(dsName+"-line");
            gr.addChildren(line);
            store.saveNode(line);
            line.init();
            line.getNodeAttribute(RRLine.COLOR_ATTRIBUTE)
                    .setValue(RRColor.values()[cInd++].toString());
            line.getNodeAttribute(RRLine.LEGEND_ATTRIBUTE).setValue(def.getName()+"\\r");
            line.getNodeAttribute(RRLine.DATADEFINITION_ATTRIBUTE).setValue(def.getPath());
            line.start();
            assertEquals(Status.STARTED, line.getStatus());
        }
        
        gr.start();
        assertEquals(Status.STARTED, gr.getStatus());
        
        return gr;
    }

    private void createRrdNode() throws Exception
    {
        rrd = new RRDNode();
        rrd.setName("rrd");
        tree.getRootNode().addChildren(rrd);
        store.saveNode(rrd);
        rrd.init();
        rrd.setStep(60l);
        
        RRArchive rra = new RRArchive();
        rra.setName("archive");
        rrd.addChildren(rra);
        store.saveNode(rra);
        rra.init();
        rra.getNodeAttribute(RRArchive.CONSOLIDATIONFUNCTION_ATTRIBUTE).setValue(
                ConsolidationFunction.AVERAGE.toString());
        rra.getNodeAttribute(RRArchive.ROWS_ATTRIBUTE).setValue(""+(60*24));
        rra.getNodeAttribute(RRArchive.XFF_ATTRIBUTE).setValue(".99");
        rra.getNodeAttribute(RRArchive.STEPS_ATTRIBUTE).setValue("1");
        rra.start();
        assertEquals(Status.STARTED, rra.getStatus());
        
        addRRDataSource("cpu-load1", ".1.3.6.1.4.1.2021.10.1.3.1");
        addRRDataSource("cpu-load5", ".1.3.6.1.4.1.2021.10.1.3.2");
        addRRDataSource("cpu-load15", ".1.3.6.1.4.1.2021.10.1.3.3");
        
        addRRDataSource("cpu-user%", ".1.3.6.1.4.1.2021.11.9.0");
        addRRDataSource("cpu-system%", ".1.3.6.1.4.1.2021.11.10.0");
        addRRDataSource("cpu-idle%" ,".1.3.6.1.4.1.2021.11.11.0");
        
        addRRDataSource("ram-total", ".1.3.6.1.4.1.2021.4.5.0");
        addRRDataSource("ram-used", ".1.3.6.1.4.1.2021.4.6.0");
        addRRDataSource("ram-free", ".1.3.6.1.4.1.2021.4.11.0");
        addRRDataSource("ram-total-swap", ".1.3.6.1.4.1.2021.4.3.0");
        addRRDataSource("ram-available-swap", ".1.3.6.1.4.1.2021.4.4.0");
        addRRDataSource("ram-shared", ".1.3.6.1.4.1.2021.4.13.0");
        addRRDataSource("ram-buffered", ".1.3.6.1.4.1.2021.4.14.0");
        addRRDataSource("ram-cached", ".1.3.6.1.4.1.2021.4.15.0");
        
        rrd.start();
        assertEquals(Status.STARTED, rrd.getStatus());
    }

    private void createSnmpDataSource()
    {
        snmp = new SnmpNode();
        snmp.setName("snmp");
        tree.getRootNode().addChildren(snmp);
        store.saveNode(snmp);
        snmp.init();
        snmp.start();
        assertEquals(Status.STARTED, snmp.getStatus());
    }
}

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

import java.io.File;
import java.util.concurrent.TimeUnit;
import org.apache.tapestry.ioc.RegistryBuilder;
import org.jrobin.core.Archive;
import org.jrobin.core.Datasource;
import org.jrobin.core.FetchData;
import org.jrobin.core.FetchRequest;
import org.jrobin.core.RrdDb;
import org.jrobin.core.Util;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.ServiceTestCase;
import org.raven.conf.Configurator;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.raven.tree.store.TreeStore;
import org.weda.constraints.ConstraintException;

/**
 *
 * @author Mikhail Titov
 */
public class RRDNodeTest extends ServiceTestCase
{
    private Configurator configurator;
    private TreeStore treeStore;
    private Tree tree;
    
    @Before
    public void initTest()
    {
        configurator = registry.getService(Configurator.class);
        assertNotNull(configurator);
        
        treeStore = configurator.getTreeStore();
        
        treeStore.removeNodes();
        
        tree = registry.getService(Tree.class);
        assertNotNull(tree);
    }

    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Test
    public void test() throws ConstraintException, Exception
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
        attr.setValue("2");
        treeStore.saveNodeAttribute(attr);
        assertEquals(Status.INITIALIZED, rrd.getStatus());
        
        RRDataSource rrds = new RRDataSource();
        rrds.setName("ds");
        rrd.addChildren(rrds);
        treeStore.saveNode(rrds);
        rrds.init();
        attr = rrds.getNodeAttribute("dataSource");
        attr.setValue(ds.getPath());
        treeStore.saveNodeAttribute(attr);
        attr = rrds.getNodeAttribute("interval");
        attr.setValue("2");;
        treeStore.saveNodeAttribute(attr);
        attr = rrds.getNodeAttribute("intervalUnit");
        attr.setValue(TimeUnit.SECONDS.toString());
        treeStore.saveNodeAttribute(attr);
        attr = rrds.getNodeAttribute("dataSourceType");
        attr.setValue("GAUGE");
        treeStore.saveNodeAttribute(attr);
                
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
      
        long start = Util.getTime()-1;
        rrds.start();
        assertEquals(Status.STARTED, rrds.getStatus());
        rrd.start();
        assertEquals(Status.STARTED, rrd.getStatus());
        ds.start();
        assertEquals(Status.STARTED, ds.getStatus());
        
        TimeUnit.SECONDS.sleep(9);
        
        rrd.stop();
        long end = Util.getTime()+1;
        
        String rrdPath = configurator.getConfig().getStringProperty(
                        Configurator.RRD_DATABASES_PATH, null);
        File rrdFile = new File(rrdPath+File.separator+rrd.getId()+".jrrd");
        assertTrue(rrdFile.exists());
        
        RrdDb db = new RrdDb(rrdFile.getAbsolutePath());
        assertEquals(2, db.getRrdDef().getStep());
        assertTrue(db.containsDs("ds"));
        assertEquals(1, db.getArcCount());
        assertEquals(1, db.getArchive(0).getSteps());
        assertEquals(100, db.getArchive(0).getRows());
        FetchRequest fetchRequest = db.createFetchRequest(
                "AVERAGE", db.getArchive(0).getStartTime(), db.getArchive(0).getEndTime());
//        fetchRequest.setFilter("ds");
        FetchData data = fetchRequest.fetchData();
        double[] values = data.getValues("ds");
        assertNotNull(values);
//        assertEquals(5, values.length);
        db.dumpXml("target/rrd_dump.xml");
        db.close();
        //
        
//        assertTrue(rrdFile.delete());
        tree.reloadTree();
        rra = (RRArchive) tree.getNode(rra.getPath());
        assertTrue(rrdFile.exists());
        TimeUnit.SECONDS.sleep(9);
        db = new RrdDb(rrdFile.getAbsolutePath());
        db.dumpXml("target/rrd_dump.xml");
        db.close();
        
        //Test add new datasource and archive
        rrd = (RRDNode) tree.getNode(rrd.getPath());
        assertNotNull(rrd);
        RRDataSource rrds2 = new RRDataSource();
        rrds2.setName("ds2");
        rrd.addChildren(rrds2);
        treeStore.saveNode(rrds2);
        rrds2.init();
        attr = rrds2.getNodeAttribute("dataSource");
        attr.setValue(ds.getPath());
        treeStore.saveNodeAttribute(attr);
        attr = rrds2.getNodeAttribute("interval");
        attr.setValue("2");
        treeStore.saveNodeAttribute(attr);
        attr = rrds2.getNodeAttribute("intervalUnit");
        attr.setValue(TimeUnit.SECONDS.toString());
        treeStore.saveNodeAttribute(attr);
        rrds2.start();
        assertEquals(Status.STARTED, rrds2.getStatus());
        
        RRArchive rra2 = new RRArchive();
        rra2.setName("archive2");
        rrd.addChildren(rra2);
        treeStore.saveNode(rra2);
        rra2.init();
        rra2.setSteps(2);
        rra2.setRows(10);
        rra2.start();
        assertEquals(Status.STARTED, rra2.getStatus());
        
        db = new RrdDb(rrdFile.getAbsolutePath());
        assertNotNull(db);
        assertTrue(db.containsDs(rrds2.getName()));
        assertNotNull(db.getArchive(rra2.getConsolidationFunction(), rra2.getSteps()));
        db.close();
        
        rrds2.stop();
        rrds2.setName("ds3");
        rrds2.setHeartbeat(5l);
        rrds2.setMinValue(-1.);
        rrds2.setMaxValue(10.);
        rrds2.start();
        
        rra2.setRows(50);
        rra2.setXff(.1);
        
        db = new RrdDb(rrdFile.getAbsolutePath());
        assertNotNull(db);
        assertEquals(2, db.getDsCount());
        Datasource datasource = db.getDatasource(rrds2.getName());
        assertNotNull(datasource);
        assertEquals(5l, datasource.getHeartbeat());
        assertEquals(new Double(-1.), new Double(datasource.getMinValue()));
        assertEquals(new Double(10.), new Double(datasource.getMaxValue()));
        assertTrue(db.containsDs(rrds2.getName()));
        assertFalse(db.containsDs("ds2"));
        
        Archive archive = db.getArchive(rra2.getConsolidationFunction(), rra2.getSteps());
        assertNotNull(archive);
        assertEquals(50, archive.getRows());
        assertEquals(new Double(.1), new Double(archive.getXff()));
        
        rrd.removeChildren(rrds2);
        rrd.removeChildren(rra2);
        
        db = new RrdDb(rrdFile.getAbsolutePath());
        assertNotNull(db);
        assertFalse(db.containsDs(rrds2.getName()));
        assertNull(db.getArchive(rra2.getConsolidationFunction(), rra2.getSteps()));
        db.close();
        
    }

}

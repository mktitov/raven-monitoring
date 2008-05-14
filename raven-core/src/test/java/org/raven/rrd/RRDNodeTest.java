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
        rrd.getNodeAttribute("step").setValue("1");
        
        RRDataSource rrds = new RRDataSource();
        rrds.setName("ds");
        rrd.addChildren(rrds);
        treeStore.saveNode(rrds);
        rrds.init();
        rrds.getNodeAttribute("dataSource").setValue(ds.getPath());
        rrds.getNodeAttribute("interval").setValue("1");
        
        assertEquals(Status.INITIALIZED, rrd.getStatus());
        
        RRArchive rra = new RRArchive();
        rra.setName("archive");
        rrd.addChildren(rra);
        treeStore.saveNode(rra);
        rra.init();
        rra.getNodeAttribute("rows").setValue("100");
        rra.start();
        assertEquals(Status.STARTED, rra.getStatus());
      
        long start = Util.getTime()-1;
        rrds.start();
        assertEquals(Status.STARTED, rrds.getStatus());
        rrd.start();
        assertEquals(Status.STARTED, rrd.getStatus());
        ds.start();
        assertEquals(Status.STARTED, ds.getStatus());
        
        TimeUnit.SECONDS.sleep(5);
        
        rrd.stop();
        long end = Util.getTime();
        
        String rrdPath = configurator.getConfig().getStringProperty(
                        Configurator.RRD_DATABASES_PATH, null);
        File rrdFile = new File(rrdPath+File.separator+rrd.getId()+".jrrd");
        assertTrue(rrdFile.exists());
        
        RrdDb db = new RrdDb(rrdFile.getAbsolutePath());
        assertTrue(db.containsDs("ds"));
        assertEquals(1, db.getArcCount());
        FetchRequest fetchRequest = db.createFetchRequest("AVERAGE", start, end);
        fetchRequest.setFilter("ds");
        FetchData data = fetchRequest.fetchData();
        double[] values = data.getValues("ds");
        assertNotNull(values);
        assertEquals(5, values.length);
        
        db.close();
    }

}

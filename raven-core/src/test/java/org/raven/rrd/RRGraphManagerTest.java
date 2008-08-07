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
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.rrd.data.RRArchive;
import org.raven.rrd.data.RRDNode;
import org.raven.rrd.data.RRDataSource;
import org.raven.rrd.objects.TestDataSource2;
import org.raven.tree.NodeAttribute;
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
    public void addDataSourcesToGraphTest() throws Exception
    {
        createTestDataSources();
        createRoundRobinDatabase();
        
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
    }

    private void createRoundRobinDatabase() throws Exception 
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

        for (int i=0; i<3; ++i)
        {
            RRDataSource rrds = new RRDataSource();
            rrds.setName("rrds_"+i);
            rrd.addChildren(rrds);
            rrds.save();
            rrds.init();
            NodeAttribute attr = rrds.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE);
            attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
            attr.setValue(dataSourcesNode.getChildren("ds_"+i).getPath());
            attr.save();
        }
        
        tree.start(rrd, false);
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
            TestDataSource2 ds = new TestDataSource2();
            ds.setName("ds_"+i);
            dataSourcesNode.addChildren(ds);
            ds.save();
            ds.init();
            ds.start();
        }
    }
}

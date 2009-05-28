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

package org.raven.rrd.data;

import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.rrd.objects.TestDataSource2;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class RRDataSourceTest extends RavenCoreTestCase
{
    @Test
    public void dataSourceAttribute() throws Exception
    {
        TestDataSource2 ds = new TestDataSource2();
        ds.setName("dataSource");
        tree.getRootNode().addChildren(ds);
        tree.saveNode(ds);
        ds.init();
        
        RRDNode rrd = new RRDNode();
        rrd.setName("rrd");
        tree.getRootNode().addChildren(rrd);
        tree.saveNode(rrd);
        rrd.init();
        
        RRDataSource rrds = new RRDataSource();
        rrds.setName("ds");
        rrd.addChildren(rrds);
        tree.saveNode(rrds);
        rrds.init();
        NodeAttribute attr = rrds.getNodeAttribute("dataSource");
        attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        attr.setValue(ds.getPath());
        
        rrds.start();
        
        assertEquals(Status.STARTED, rrds.getStatus());
        
        attr.setValue(null);
        assertEquals(Status.INITIALIZED, rrds.getStatus());
        rrds.start();
        assertEquals(Status.INITIALIZED, rrds.getStatus());
        
        attr.setValue(ds.getPath());
        rrds.start();
        assertEquals(Status.STARTED, rrds.getStatus());
        
        tree.remove(ds);
        assertEquals(Status.INITIALIZED, rrds.getStatus());
    }
}

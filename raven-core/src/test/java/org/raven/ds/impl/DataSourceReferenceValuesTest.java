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

package org.raven.ds.impl;

import java.util.List;
import org.apache.tapestry.ioc.RegistryBuilder;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.ServiceTestCase;
import org.raven.conf.Configurator;
import org.raven.ds.DataSource;
import org.raven.ds.impl.objects.TestDataSourceInterface;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeListener;
import org.raven.tree.Tree;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.SystemNode;
import org.raven.tree.store.TreeStore;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class DataSourceReferenceValuesTest extends ServiceTestCase
{

    private Node ds1;
    private Node ds2;
    
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Test
    public void test() 
    {
        Tree tree = registry.getService(Tree.class);
        Configurator configurator= registry.getService(Configurator.class);
        TreeStore store = configurator.getTreeStore();
        
        store.removeNodes();
        tree.reloadTree();
        
        DataSourcesNode dataSources = 
                (DataSourcesNode) 
                tree.getRootNode().getChildren(SystemNode.NAME).getChildren(DataSourcesNode.NAME);
        
        DataSourceReferenceValues referenceValues = new DataSourceReferenceValues();
        NodeAttribute attr = new NodeAttributeImpl("attr", DataSource.class, null, null);
        attr.setOwner(new ContainerNode("owner"));
        trainMocks();
        
        assertNull(referenceValues.getReferenceValues(attr));
        
        dataSources.addChildren(ds1);
        dataSources.addChildren(ds2);
        
        List<String> values = referenceValues.getReferenceValues(attr);
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains("ds1Path"));
        assertTrue(values.contains("ds2Path"));
        
        attr.setType(ds1.getClass());
        values = referenceValues.getReferenceValues(attr);
        assertNotNull(values);
        assertEquals(1, values.size());
        assertTrue(values.contains("ds1Path"));
        
        attr.setType(ds2.getClass());
        values = referenceValues.getReferenceValues(attr);
        assertNotNull(values);
        assertEquals(1, values.size());
        assertTrue(values.contains("ds2Path"));
        
        attr.setType(DataSource.class);
        values = tree.getReferenceValuesForAttribute(attr);
        assertNotNull(values);
        assertEquals(2, values.size());
        
        verify(ds1, ds2);
    }
    
    private void trainMocks()
    {
        ds1 = createMock(DataSource.class);
        ds2 = createMock(TestDataSourceInterface.class);
        
        expect(ds1.getName()).andReturn("ds1");
        ds1.setParent((Node) anyObject());
        ds1.addListener((NodeListener) anyObject());
        expect(ds1.getIndex()).andReturn(1);
        expect(ds1.getPath()).andReturn("ds1Path").times(3);
        
        expect(ds2.getName()).andReturn("ds2");
        ds2.setParent((Node) anyObject());
        ds2.addListener((NodeListener) anyObject());
        expect(ds2.getIndex()).andReturn(2);
        expect(ds2.getPath()).andReturn("ds2Path").times(3);
        
        replay(ds1, ds2);
    }
}

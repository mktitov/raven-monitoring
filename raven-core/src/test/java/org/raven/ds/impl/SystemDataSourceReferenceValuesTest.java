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
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.ds.DataSource;
import org.raven.ds.impl.objects.TestDataSourceInterface;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeListener;
import org.raven.tree.NodePathResolver;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.SystemNode;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueCollectionImpl;
import org.weda.constraints.ReferenceValueCollection;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class SystemDataSourceReferenceValuesTest extends RavenCoreTestCase
{

    private Node ds1;
    private Node ds2;
    private DataSourcesNode dataSources;
    
    @Test
    public void instanceTest() throws TooManyReferenceValuesException, Exception 
    {
        NodePathResolver pathResolver = registry.getService(NodePathResolver.class);
        
        store.removeNodes();
        tree.reloadTree();
        
        dataSources = 
                (DataSourcesNode) 
                tree.getRootNode().getChildren(SystemNode.NAME).getChildren(DataSourcesNode.NAME);
        
        SystemDataSourceReferenceValues referenceValues = new SystemDataSourceReferenceValues();
        NodeAttribute attr = new NodeAttributeImpl("attr", DataSource.class, null, null);
        attr.setValueHandlerType(SystemDataSourceValueHandlerFactory.TYPE);
        attr.setOwner(new ContainerNode("owner"));
        attr.init();
        trainMocks();
        
        ReferenceValueCollection values = new ReferenceValueCollectionImpl(Integer.MAX_VALUE, null);
        referenceValues.getReferenceValues(attr, values);
        assertTrue(values.asList().isEmpty());
        
        dataSources.addChildren(ds1);
        dataSources.addChildren(ds2);
        
        values = new ReferenceValueCollectionImpl(Integer.MAX_VALUE, null);
        referenceValues.getReferenceValues(attr, values);
        checkReferenceValues(values.asList(), pathResolver);
        
//        attr.setType(ds1.getClass());
//        values = referenceValues.getReferenceValues(attr);
//        assertNotNull(values);
//        assertEquals(1, values.size());
//        assertTrue(values.contains("ds1Path"));
//        
//        attr.setType(ds2.getClass());
//        values = referenceValues.getReferenceValues(attr);
//        assertNotNull(values);
//        assertEquals(1, values.size());
//        assertTrue(values.contains("ds2Path"));
//        
        attr.setType(DataSource.class);
        checkReferenceValues(tree.getReferenceValuesForAttribute(attr), pathResolver);
        
        verify(ds1, ds2);
    }
    
    private void checkReferenceValues(List<ReferenceValue> values, NodePathResolver pathResolver)
    {
        assertNotNull(values);
        assertEquals(2, values.size());
        ReferenceValue value = values.get(0);
        assertEquals("ds1", value.getValueAsString());
        assertEquals(pathResolver.getAbsolutePath(ds1), value.getValue());
        value = values.get(1);
        assertEquals("ds2", value.getValueAsString());
        assertEquals(pathResolver.getAbsolutePath(ds2), value.getValue());

//        attr.setType(ds1.getClass());
//        values = referenceValues.getReferenceValues(attr);
//        assertNotNull(values);
//        assertEquals(1, values.size());
//        assertTrue(values.contains("ds1Path"));
//
//        attr.setType(ds2.getClass());
//        values = referenceValues.getReferenceValues(attr);
//        assertNotNull(values);
//        assertEquals(1, values.size());
//        assertTrue(values.contains("ds2Path"));
//
//        attr.setType(DataSource.class);
//        values = tree.getReferenceValuesForAttribute(attr);
//        assertNotNull(values);
//        assertEquals(2, values.size());
    }
    
    private void trainMocks()
    {
        ds1 = createMock("ds1", DataSource.class);
        ds2 = createMock("ds2", TestDataSourceInterface.class);
        
        expect(ds1.getName()).andReturn("ds1").anyTimes();
        ds1.setParent((Node) anyObject());
        ds1.addListener((NodeListener) anyObject());
        expect(ds1.getIndex()).andReturn(1);
        expect(ds1.getParent()).andReturn(dataSources).anyTimes();
        expect(ds1.compareTo((Node)anyObject())).andReturn(-1).anyTimes();
        
        expect(ds2.getName()).andReturn("ds2").anyTimes();
        ds2.setParent((Node) anyObject());
        ds2.addListener((NodeListener) anyObject());
        expect(ds2.getIndex()).andReturn(2);
        expect(ds2.getParent()).andReturn(dataSources).anyTimes();
        expect(ds2.compareTo((Node)anyObject())).andReturn(1).anyTimes();
//        expect(ds2.getPath()).andReturn("ds2Path").times(3);
        
        replay(ds1, ds2);
    }
}

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

package org.raven.tree.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.tapestry.ioc.RegistryBuilder;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.ServiceTestCase;
import org.raven.conf.Configurator;
import org.raven.impl.NodeClassTransformerWorker;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.store.TreeStore;
import org.weda.internal.services.ResourceProvider;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class TreeImplTest extends ServiceTestCase
{

    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Test
    public void getReferenceValuesForAttribute() throws IOException, Exception
    {
        NodeAttribute integerAttr = 
                new NodeAttributeImpl("integerAttribute", Integer.class, null, null);
        integerAttr.setId(1);
        NodeAttribute numberAttr = 
                new NodeAttributeImpl("numberAttribute", Number.class, null, null);
        numberAttr.setId(2);
        NodeAttribute stringAttr = 
                new NodeAttributeImpl("stringAttribute", String.class, null, null);
        stringAttr.setId(3);
        
        Configurator configurator = createMock("Configurator", Configurator.class);
        TreeStore store = createMock("TreeStore", TreeStore.class);
        ResourceProvider resourceProvider = createMock("ResourceProvider", ResourceProvider.class);
        AttributeReferenceValues refValues = 
                createMock("AttributeReferenceValues", AttributeReferenceValues.class);
        
        expect(configurator.getTreeStore()).andReturn(store).anyTimes();
        ContainerNode rootNode = new ContainerNode("");
        expect(store.getRootNode()).andReturn(rootNode);
        store.saveNode(isA(Node.class));
        expectLastCall().anyTimes();
        resourceProvider.getResourceStrings(NodeClassTransformerWorker.NODES_TYPES_RESOURCE);
        expectLastCall().andReturn(Collections.EMPTY_LIST);
        List<String> oneList = Arrays.asList("1");
        List<String> twoList = Arrays.asList("2");
        expect(refValues.getReferenceValues(numberAttr)).andReturn(oneList);
        expect(refValues.getReferenceValues(integerAttr)).andReturn(twoList);
                
        replay(configurator, store, resourceProvider, refValues);
        
        Map<Class, AttributeReferenceValues> providers = 
                new HashMap<Class, AttributeReferenceValues>();
        providers.put(Number.class, refValues);
        TreeImpl tree = new TreeImpl(providers, configurator, resourceProvider);
        assertSame(oneList, tree.getReferenceValuesForAttribute(numberAttr));
        assertSame(twoList, tree.getReferenceValuesForAttribute(integerAttr));
        assertNull(tree.getReferenceValuesForAttribute(stringAttr));
        
        verify(configurator, store, resourceProvider);
    }
}

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

import java.util.Set;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.test.ServiceTestCase;
import org.raven.conf.Configurator;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeListener;
import org.raven.tree.impl.objects.NodeWithParameter2;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class ParameterFieldTransformerWorkerTest extends ServiceTestCase 
{
    private Configurator configurator;
    
    @Override
    protected void configureRegistry(Set<Class> builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Before @After
    public void initTest()
    {
        configurator = registry.getService(Configurator.class);
        configurator.getTreeStore().removeNodes();
    }
    
    @Test
    public void test() throws Exception
    {
        NodeWithParameter2 node = new NodeWithParameter2();
        node.setName("node");
        NodeAttribute attribute = createAndTrainAttribute();
        node.addNodeAttribute(attribute);
        
        assertEquals("value", node.getParameter());
        
        verify(attribute);
    }

    private NodeAttribute createAndTrainAttribute()
    {
        NodeAttribute attribute = createMock(NodeAttribute.class);
        expect(attribute.getName()).andReturn("parameter");
        expect(attribute.getRealValue()).andReturn("value");
        replay(attribute);
        
        return attribute;
    }
}

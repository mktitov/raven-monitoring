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

package org.raven.conf.impl;

import org.apache.tapestry.ioc.RegistryBuilder;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.conf.Configurator;
import org.raven.ServiceTestCase;
import org.raven.conf.Config;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class ConfiguratorImplTest extends ServiceTestCase
{
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Test
    public void test() throws Exception
    {
        Configurator configurator = registry.getService(Configurator.class);
        assertNotNull(configurator);
        
        Config props = configurator.getConfig();
        assertNotNull(props);
        
        assertSame(props, configurator.getConfig());
    }
    
    @Test
    public void saveBaseNode()
    {
        Configurator configurator = registry.getService(Configurator.class);
        
        BaseNode node = new BaseNode(null);
        node.setName("root node");
        
        configurator.save(node);
        
        BaseNode childNode = new BaseNode(null);
        childNode.setName("child node");
        node.addChildren(childNode);
    }
}

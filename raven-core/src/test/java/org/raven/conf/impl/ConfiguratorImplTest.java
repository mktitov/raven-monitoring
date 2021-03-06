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

import java.util.Set;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.conf.Configurator;
import org.raven.test.ServiceTestCase;
import org.raven.conf.Config;
import org.raven.tree.store.TreeStore;

/**
 *
 * @author Mikhail Titov
 */
public class ConfiguratorImplTest extends ServiceTestCase
{
    @Override
    protected void configureRegistry(Set<Class> builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Test
    public void create() throws Exception
    {
        Configurator configurator = registry.getService(Configurator.class);
        assertNotNull(configurator);
        
        Config props = configurator.getConfig();
        assertNotNull(props);
        
        assertSame(props, configurator.getConfig());
        
        TreeStore store = configurator.getTreeStore();
        assertNotNull(store);
    }
    
    
//    @Test 
//    @Ignore
//    public void saveBaseNode()
//    {
//        Configurator configurator = registry.getService(Configurator.class);
//        
//        BaseNode node = new ContainerNode("root node");
//        
//        configurator.beginTransaction();
//        configurator.save(node);
//        configurator.commit();
//        
//        configurator.beginTransaction();
//        for (int i=0; i<2; ++i)
//        {
//            BaseNode childNode = new ContainerNode("child node "+i);
//            node.addChildren(childNode);
//            configurator.save(childNode);
//        }
//        configurator.commit();
//        
//        Collection<BaseNode> nodes = configurator.getObjects(BaseNode.class, "level ascending");
//        assertNotNull(nodes);
//    }
    
//    @Test
////    @Ignore
//    public void getObjects()
//    {
//        Configurator configurator = registry.getService(Configurator.class);
//        Collection<BaseNode> nodes = configurator.getObjects(BaseNode.class, "level descending");
//        
//        assertNotNull(nodes);
//        
//        BaseNode rootNode = null;
//        for (BaseNode node: nodes)
//            if (node.getParent()==null)
//            {
//                rootNode = node;
//                break;
//            }
//        
//        assertNotNull(rootNode);
//        assertNotNull(rootNode.getName());
//        configurator.beginTransaction();
//        Collection<Node> childrens = rootNode.getChildrens();
//        configurator.commit();
        
//        assertNotNull(childrens);
//    }
}

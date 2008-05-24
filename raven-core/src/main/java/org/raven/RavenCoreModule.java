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
package org.raven;

import java.util.Map;
import org.apache.tapestry.ioc.Configuration;
import org.apache.tapestry.ioc.MappedConfiguration;
import org.apache.tapestry.ioc.ServiceBinder;
import org.raven.conf.Configurator;
import org.raven.conf.impl.ConfiguratorImpl;
import org.raven.impl.NodeToStringConverter;
import org.raven.impl.SnmpVariableToNumberConverter;
import org.raven.impl.StringToNodeConverter;
import org.raven.tree.Tree;
import org.raven.tree.impl.TreeImpl;
import org.raven.tree.store.impl.H2TreeStore;
import org.weda.internal.services.ResourceProvider;

/**
 * Tapestry IOC module for raven-core module
 * @author Mikhail Titov
 */
public class RavenCoreModule
{
    public static void bind(ServiceBinder binder)
    {
        binder.bind(Configurator.class, ConfiguratorImpl.class);
    }
    
    public static Configurator buildConfigurator(Map<String, Class> treeStoreEngines)
    {
        return new ConfiguratorImpl(treeStoreEngines);
    }
    
//    @EagerLoad()
    public static Tree buildTree(Configurator configurator, ResourceProvider resourceProvider) 
            throws Exception
    {
        return new TreeImpl(configurator, resourceProvider);
    }
    
    public static void contributeConfigurator(MappedConfiguration<String, Class> conf)
    {
        conf.add(Configurator.H2_TREE_STORE_ENGINE, H2TreeStore.class);
    }
    
    public static void contributeTypeConverter(Configuration conf)
    {
        conf.add(new NodeToStringConverter());
        conf.add(new StringToNodeConverter());
        conf.add(new SnmpVariableToNumberConverter());
    }
}

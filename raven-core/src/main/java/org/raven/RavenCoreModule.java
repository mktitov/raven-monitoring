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

import java.util.List;
import java.util.Map;
import org.apache.tapestry.ioc.Configuration;
import org.apache.tapestry.ioc.MappedConfiguration;
import org.apache.tapestry.ioc.ServiceBinder;
import org.apache.tapestry.ioc.services.ChainBuilder;
import org.raven.conf.Configurator;
import org.raven.conf.impl.ConfiguratorImpl;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataSourceReferenceValues;
import org.raven.impl.AttributeReferenceToStringConverter;
import org.raven.impl.EnumReferenceValues;
import org.raven.impl.NodeToStringConverter;
import org.raven.impl.SnmpVariableToNumberConverter;
import org.raven.impl.StringToAttributeReferenceConverter;
import org.raven.impl.StringToNodeConverter;
import org.raven.impl.StringToTemplateVariableConverter;
import org.raven.template.TemplateVariable;
import org.raven.template.TemplateVariableReferenceValues;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.AttributeValueHandlerFactory;
import org.raven.tree.AttributeValueHandlerRegistry;
import org.raven.tree.NodePathResolver;
import org.raven.tree.Tree;
import org.raven.tree.impl.AttributeValueHandlerRegistryImpl;
import org.raven.tree.impl.NodePathResolverImpl;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
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
    
    public static NodePathResolver buildNodePathResolver(Tree tree)
    {
        return new NodePathResolverImpl(tree);
    }
    
    public static Tree buildTree(
            Map<Class, AttributeReferenceValues> referenceValuesProvider
            , Configurator configurator, ResourceProvider resourceProvider
            , NodePathResolver pathResolver) 
        throws Exception
    {
        return new TreeImpl(referenceValuesProvider, configurator, resourceProvider, pathResolver);
    }
    
    public static AttributeReferenceValues buildAttributeReferenceValues(
            ChainBuilder builder, List<AttributeReferenceValues> commands)
    {
        return builder.build(AttributeReferenceValues.class, commands);
    }
    
    public static AttributeValueHandlerRegistry buildAttributeValueHandlerRegistry(
            Map<String, AttributeValueHandlerFactory> factories)
    {
        return new AttributeValueHandlerRegistryImpl(factories);
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
        conf.add(new StringToAttributeReferenceConverter());
        conf.add(new AttributeReferenceToStringConverter());
        conf.add(new StringToTemplateVariableConverter());
    }
    
    public static void contributeTree(MappedConfiguration<Class, AttributeReferenceValues> conf)
    {
        conf.add(TemplateVariable.class, new TemplateVariableReferenceValues());
        conf.add(DataSource.class, new DataSourceReferenceValues());
        conf.add(Enum.class, new EnumReferenceValues());
    }
    
    public static void contributeAttributeValueHandlerRegistry(
            MappedConfiguration<String, AttributeValueHandlerFactory> conf)
    {
        conf.add(NodeReferenceValueHandlerFactory.TYPE, new NodeReferenceValueHandlerFactory());
    }
}

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
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.services.ChainBuilder;
import org.raven.cache.TemporaryCacheManager;
import org.raven.cache.TemporaryCacheManagerImpl;
import org.raven.conf.Configurator;
import org.raven.conf.impl.ConfiguratorImpl;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.ds.DataSource;
import org.raven.ds.impl.ConnectionPoolValueHandlerFactory;
import org.raven.ds.impl.DataPipeConvertToTypesReferenceValues;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.ds.impl.RecordSchemasNode;
import org.raven.ds.impl.SystemDataSourceReferenceValues;
import org.raven.ds.impl.SystemDataSourceValueHandlerFactory;
import org.raven.expr.ExpressionCache;
import org.raven.expr.ExpressionCompiler;
import org.raven.expr.impl.CacheExpressionCompiler;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.expr.impl.ExpressionCacheImpl;
import org.raven.expr.impl.ExpressionCompilerImpl;
import org.raven.expr.impl.GroovyExpressionCompiler;
import org.raven.impl.AttributeReferenceToStringConverter;
import org.raven.impl.BooleanReferenceValues;
import org.raven.impl.CharsetReferenceValues;
import org.raven.impl.ClassToStringConverter;
import org.raven.impl.DateToDateConverter;
import org.raven.impl.DateToLongConverter;
import org.raven.impl.EnumReferenceValues;
import org.raven.impl.InputStreamToStringConverter;
import org.raven.impl.IntegerToIpConverter;
import org.raven.impl.LocaleReferenceValues;
import org.raven.impl.LongToTimestampConverter;
import org.raven.impl.NodeAccessToNodeConverter;
import org.raven.impl.NodeAttributeToStringConverter;
import org.raven.impl.NodeToStringConverter;
import org.raven.impl.NumberToNumberConverter;
import org.raven.impl.OracleTimestampToTimestampConverter;
import org.raven.impl.SimpleCache;
import org.raven.impl.SnmpVariableToNumberConverter;
import org.raven.impl.StringToAttributeReferenceConverter;
import org.raven.impl.StringToCharsetConverter;
import org.raven.impl.StringToClassConverter;
import org.raven.impl.StringToIpConverter;
import org.raven.impl.StringToLocaleConverter;
import org.raven.impl.StringToNodeConverter;
import org.raven.impl.StringToTemplateVariableConverter;
import org.raven.log.NodeLogger;
import org.raven.log.impl.NodeLoggerImpl;
import org.raven.net.NetworkResponseService;
import org.raven.net.impl.NetworkResponseServiceImpl;
import org.raven.sched.impl.SystemSchedulerReferenceValues;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.sql.QueryParameterValueHandlerFactory;
import org.raven.template.GroupsOrganazier;
import org.raven.template.impl.GroupsOrganazierImpl;
import org.raven.template.impl.TemplateVariable;
import org.raven.template.impl.TemplateVariableReferenceValues;
import org.raven.template.impl.TemplateVariableValueHandlerFactory;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.AttributeValueHandlerFactory;
import org.raven.tree.AttributeValueHandlerRegistry;
import org.raven.tree.NodePathResolver;
import org.raven.tree.Tree;
import org.raven.tree.impl.AttributeReferenceHandlerFactory;
import org.raven.tree.impl.AttributeReferenceValueHandlerFactory;
import org.raven.tree.impl.AttributeValueHandlerRegistryImpl;
import org.raven.tree.impl.ChildrenNodesAsReferenceValues;
import org.raven.tree.impl.DataFileValueHandlerFactory;
import org.raven.tree.impl.NodePathResolverImpl;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.RefreshAttributeValueHandlerFactory;
import org.raven.tree.impl.SchemasNode;
import org.raven.tree.impl.SystemNode;
import org.raven.tree.impl.TreeImpl;
import org.raven.tree.store.impl.H2TreeStore;
import org.weda.internal.Cache;
import org.weda.internal.CacheScope;
import org.weda.internal.services.CacheManager;
import org.weda.internal.services.ResourceProvider;

/**
 * Tapestry IOC module for raven-core module
 * @author Mikhail Titov
 */
public class RavenCoreModule
{
    public static void bind(ServiceBinder binder)
    {
        binder.bind(NodeLogger.class, NodeLoggerImpl.class);
        binder.bind(GroupsOrganazier.class, GroupsOrganazierImpl.class);
        binder.bind(ExpressionCache.class, ExpressionCacheImpl.class);
        binder.bind(NetworkResponseService.class, NetworkResponseServiceImpl.class);
    }

    public static TemporaryCacheManager buildTemporaryCacheManager(CacheManager cacheManager)
    {
        return new TemporaryCacheManagerImpl(cacheManager);
    }
    
    @SuppressWarnings("unchecked")
	public static Configurator buildConfigurator(Map<String, Class> treeStoreEngines)
    {
        return new ConfiguratorImpl(treeStoreEngines);
    }
    
    public static NodePathResolver buildNodePathResolver()
    {
        return new NodePathResolverImpl();
    }
    
    public static Tree buildTree(
            AttributeReferenceValues attributeReferenceValues
            , Configurator configurator
            , ResourceProvider resourceProvider
            , NodePathResolver pathResolver
            , AttributeValueHandlerRegistry valueHandlerRegistry) 
        throws Exception
    {
        return new TreeImpl(
                attributeReferenceValues, configurator, resourceProvider, pathResolver
                , valueHandlerRegistry);
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

	public static ExpressionCompiler buildExpressionCompiler(
			ChainBuilder builder, List<ExpressionCompiler> commands)
	{
		return builder.build(ExpressionCompiler.class, commands);
	}
    
    @SuppressWarnings("unchecked")
	public static void contributeConfigurator(MappedConfiguration<String, Class> conf)
    {
        conf.add(Configurator.H2_TREE_STORE_ENGINE, H2TreeStore.class);
    }
    
    @SuppressWarnings("unchecked")
	public static void contributeTypeConverter(Configuration conf)
    {
        conf.add(new NodeToStringConverter());
        conf.add(new NodeAttributeToStringConverter());
        conf.add(new StringToNodeConverter());
        conf.add(new NumberToNumberConverter());
        conf.add(new SnmpVariableToNumberConverter());
        conf.add(new StringToAttributeReferenceConverter());
        conf.add(new AttributeReferenceToStringConverter());
        conf.add(new StringToTemplateVariableConverter());
        conf.add(new NodeAccessToNodeConverter());
        conf.add(new StringToClassConverter());
        conf.add(new ClassToStringConverter());
        conf.add(new InputStreamToStringConverter());
        conf.add(new StringToCharsetConverter());
		conf.add(new StringToLocaleConverter());
        conf.add(new StringToIpConverter());
        conf.add(new IntegerToIpConverter());
        conf.add(new LongToTimestampConverter());
        conf.add(new OracleTimestampToTimestampConverter());
        conf.add(new DateToLongConverter());
        conf.add(new DateToDateConverter());
    }
    
    @SuppressWarnings("unchecked")
	public static void contributeTree(MappedConfiguration<Class, AttributeReferenceValues> conf)
    {
        conf.add(TemplateVariable.class, new TemplateVariableReferenceValues());
        conf.add(DataSource.class, new SystemDataSourceReferenceValues());
        conf.add(Enum.class, new EnumReferenceValues());
    }
    
    public static void contributeAttributeValueHandlerRegistry(
            MappedConfiguration<String, AttributeValueHandlerFactory> conf
            , NodePathResolver pathResolver)
    {
        conf.add(
            NodeReferenceValueHandlerFactory.TYPE
            , new NodeReferenceValueHandlerFactory(pathResolver));
        conf.add(
            AttributeReferenceValueHandlerFactory.TYPE
            , new AttributeReferenceValueHandlerFactory(pathResolver));
        conf.add(
            AttributeReferenceHandlerFactory.TYPE
            , new AttributeReferenceHandlerFactory(pathResolver));
        conf.add(
            TemplateVariableValueHandlerFactory.TYPE
            , new TemplateVariableValueHandlerFactory(pathResolver));
        conf.add(
            SystemDataSourceValueHandlerFactory.TYPE
            , new SystemDataSourceValueHandlerFactory(pathResolver));
        conf.add(
            ExpressionAttributeValueHandlerFactory.TYPE
            , new ExpressionAttributeValueHandlerFactory());
        conf.add(
            QueryParameterValueHandlerFactory.TYPE
            , new QueryParameterValueHandlerFactory());
        conf.add(
            RefreshAttributeValueHandlerFactory.TYPE
            , new RefreshAttributeValueHandlerFactory());
        conf.add(
            SystemSchedulerValueHandlerFactory.TYPE
            , new SystemSchedulerValueHandlerFactory(pathResolver));
        conf.add(
            RecordSchemaValueTypeHandlerFactory.TYPE
            , new RecordSchemaValueTypeHandlerFactory(pathResolver));
        conf.add(
            ConnectionPoolValueHandlerFactory.TYPE
            , new ConnectionPoolValueHandlerFactory(pathResolver));
        conf.add(
            DataFileValueHandlerFactory.TYPE
            , new DataFileValueHandlerFactory(pathResolver));
    }
    
    public static void contributeAttributeReferenceValues(
            OrderedConfiguration<AttributeReferenceValues> conf
            , NodePathResolver pathResolver)
    {
        conf.add(EnumReferenceValues.class.getSimpleName(), new EnumReferenceValues(), "before:*");
        conf.add(
            SystemDataSourceReferenceValues.class.getSimpleName()
            , new SystemDataSourceReferenceValues()
            , "after:"+EnumReferenceValues.class.getSimpleName());
        conf.add(
            SystemSchedulerReferenceValues.class.getSimpleName()
            , new SystemSchedulerReferenceValues()
            , "after:"+SystemDataSourceReferenceValues.class.getSimpleName());
        conf.add(
            BooleanReferenceValues.class.getSimpleName()
            , new BooleanReferenceValues()
            , "after:"+SystemSchedulerReferenceValues.class.getSimpleName());
        conf.add(
            TemplateVariableReferenceValues.class.getSimpleName()
            , new TemplateVariableReferenceValues()
            , "after:"+BooleanReferenceValues.class.getSimpleName());
        conf.add(
            DataPipeConvertToTypesReferenceValues.class.getSimpleName()
            , new DataPipeConvertToTypesReferenceValues()
            , "after:"+TemplateVariableReferenceValues.class.getSimpleName());
        conf.add(
            CharsetReferenceValues.class.getSimpleName()
            , new CharsetReferenceValues()
            , "after:"+DataPipeConvertToTypesReferenceValues.class.getSimpleName());
		conf.add(
			LocaleReferenceValues.class.getSimpleName()
			, new LocaleReferenceValues()
			, "after:"+CharsetReferenceValues.class.getSimpleName());
        conf.add(
            RecordSchemasNode.class.getSimpleName()
            , new ChildrenNodesAsReferenceValues(
                RecordSchemaValueTypeHandlerFactory.TYPE
                , pathResolver.createPath(
                    true, SystemNode.NAME, SchemasNode.NAME, RecordSchemasNode.NAME))
            , "after:"+LocaleReferenceValues.class.getSimpleName());
        conf.add(
            ConnectionPoolsNode.class.getSimpleName()
            , new ChildrenNodesAsReferenceValues(
                ConnectionPoolValueHandlerFactory.TYPE
                , pathResolver.createPath(
                    true, SystemNode.NAME, ConnectionPoolsNode.NAME))
            , "after:"+RecordSchemasNode.class.getSimpleName());
    }

	public static void contributeExpressionCompiler(
			OrderedConfiguration<ExpressionCompiler> conf
			, ExpressionCache expressionCache)
	{
		conf.add(
				CacheExpressionCompiler.class.getName()
				, new CacheExpressionCompiler(expressionCache)
				, "before:*");
		conf.add(
				GroovyExpressionCompiler.class.getName()
				, new GroovyExpressionCompiler(expressionCache)
				, "after:"+CacheExpressionCompiler.class.getName());
		conf.add(
				ExpressionCompilerImpl.class.getName()
				, new ExpressionCompilerImpl(expressionCache)
				, "after:*");
	}

    public static void contributeCacheManager(MappedConfiguration<CacheScope, Cache> conf)
    {
        conf.add(CacheScope.GLOBAL, new SimpleCache());
    }
}

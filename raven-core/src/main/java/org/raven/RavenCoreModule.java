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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.services.ChainBuilder;
import org.raven.audit.Auditor;
import org.raven.audit.impl.AuditorImpl;
import org.raven.auth.LoginManager;
import org.raven.auth.AuthProvider;
import org.raven.auth.LoginService;
import org.raven.auth.Authenticator;
import org.raven.auth.NodeAccessService;
import org.raven.auth.impl.LoginManagerService;
import org.raven.auth.impl.AuthServiceImpl;
import org.raven.auth.impl.NodeAccessServiceImpl;
import org.raven.cache.TemporaryCacheManager;
import org.raven.cache.TemporaryCacheManagerImpl;
import org.raven.cache.TemporaryFileManagerValueHandlerFactory;
import org.raven.cache.TemporaryFileManagersNode;
import org.raven.conf.Configurator;
import org.raven.conf.impl.ConfiguratorImpl;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.ds.DataSource;
import org.raven.ds.impl.ConnectionPoolValueHandlerFactory;
import org.raven.ds.impl.DataConsumerAttributeValueHandlerFactory;
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
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.impl.*;
import org.raven.log.NodeLogger;
import org.raven.log.impl.NodeLoggerImpl;
import org.raven.net.NetworkResponseService;
import org.raven.net.impl.NetworkResponseServiceImpl;
import org.raven.sched.SystemExecutorService;
import org.raven.sched.impl.SystemExecutorServiceImpl;
import org.raven.sched.impl.SystemSchedulerReferenceValues;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.sql.QueryParameterValueHandlerFactory;
import org.raven.template.GroupsOrganazier;
import org.raven.template.impl.GroupsOrganazierImpl;
import org.raven.template.impl.TemplateVariable;
import org.raven.template.impl.TemplateVariableReferenceValues;
import org.raven.template.impl.TemplateVariableValueHandlerFactory;
import org.raven.tree.*;
import org.raven.tree.impl.*;
import org.raven.tree.store.impl.H2TreeStore;
import org.slf4j.Logger;
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
        binder.bind(Auditor.class, AuditorImpl.class);
        binder.bind(GroupsOrganazier.class, GroupsOrganazierImpl.class);
        binder.bind(ExpressionCache.class, ExpressionCacheImpl.class);
        binder.bind(NetworkResponseService.class, NetworkResponseServiceImpl.class);
    }
    
    public static SystemExecutorService buildSystemExecutorService(Logger logger) {
        return new SystemExecutorServiceImpl(logger);
    }
    
    public static MimeTypeService buildMimeTypeService() {
        return new MimeTypeServiceImpl();
    }
    
    public static VersionService buildVersionService(Collection<String> modulesVersion) throws IOException {
        return new VersionServiceImpl(modulesVersion);
    }

    public static TemporaryCacheManager buildTemporaryCacheManager(CacheManager cacheManager) {
        return new TemporaryCacheManagerImpl(cacheManager);
    }
    
    public static LoginManager buildAuthManager(Tree tree) {
        return new LoginManagerService(tree);
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

    public static TreeListeners buildTreeListeners(final Collection<TreeListener> listeners){
        return new TreeListenersImpl(listeners);
    }
    
    public static Tree buildTree(
            AttributeReferenceValues attributeReferenceValues
            , Configurator configurator
            , ResourceProvider resourceProvider
            , NodePathResolver pathResolver
            , AttributeValueHandlerRegistry valueHandlerRegistry
            , TreeListeners listeners)
        throws Exception
    {
        return new TreeImpl(
                attributeReferenceValues, configurator, resourceProvider, pathResolver
                , valueHandlerRegistry, listeners.getListeners());
    }
    
    public static ResourceManager buildResourceManager(final Collection<ResourceRegistrator> registrators) {
        return new ResourceManagerImpl(registrators);
    }

    public static LoginService buildAuthService(
            List<AuthProvider> providers, Configurator configurator, Logger logger)
    {
        return new AuthServiceImpl(providers, configurator, logger);
    }
    
    public static TreeNodesBuilder buildTreeNodesBuilder(final Collection<NodeBuildersProvider> providers,
            NodePathResolver pathResolver) 
    {
        return new TreeNodesBuilderImpl(providers, pathResolver);
    }
    
    public static TemplateNodeBuildersProvider buildTemplateNodeBuildersProvider(
        final Collection<ResourceDescriptor> resourceDescriptors) 
    {
        return new TemplateNodeBuildersProviderImpl(resourceDescriptors);
    }

    public static NodeAccessService buildNodeAccessService(Tree tree, Configurator configurator)
    {
        return new NodeAccessServiceImpl(tree, configurator);
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

    public static Authenticator buildAuthenticator(
            ChainBuilder builder, List<Authenticator> commands)
    {
        return builder.build(Authenticator.class, commands);
    }

//    public static UserContextConfiguratorService buildContextConfiguratorService(
//            Collection<UserContextConfigurator> configurators)
//    {
//        return new UserContextConfiguratorServiceImpl(configurators);
//    }
//
//    public static void contributeVersionService(Configuration<String> conf) {
//        RavenCoreModule.class.getClassLoader().
//        conf.add("raven-core: "+ResourceBundle.getBundle("org/raven/version").getString("version"));
//    }
    
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
        conf.add(new InputStreamToBinaryFieldConverter());
        conf.add(new StringToCharsetConverter());
		conf.add(new StringToLocaleConverter());
        conf.add(new StringToIpConverter());
        conf.add(new StringToInputStreamConverter());
        conf.add(new StringToByteArrayConverter());
        conf.add(new IntegerToIpConverter());
        conf.add(new LongToTimestampConverter());
        conf.add(new OracleTimestampToTimestampConverter());
        conf.add(new DateToLongConverter());
        conf.add(new DateToDateConverter());
        conf.add(new CollectionToTableConverter());
        conf.add(new ByteArrayDataSourceToByteArrayConverter());
        conf.add(new ByteArrayToBinaryFieldConverter());
        conf.add(new DataSourceToInputStreamConverter());
        conf.add(new DataSourceToBinaryFieldConverter());
        conf.add(new BinaryFieldToInputStreamConverter());
        conf.add(new FileToInputStreamConverter());
        conf.add(new ClobToStringConverter());
        conf.add(new StringToTimeZoneConverter());
        conf.add(new TimeZoneToStringConverter());
        conf.add(new RecordToMapConverter());
        conf.add(new DataFileToInputStreamConverter());
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
            ScriptAttributeValueHandlerFactory.TYPE
            , new ScriptAttributeValueHandlerFactory());
        conf.add(
            QueryParameterValueHandlerFactory.TYPE
            , new QueryParameterValueHandlerFactory());
        conf.add(
            RefreshAttributeValueHandlerFactory.TYPE
            , new RefreshAttributeValueHandlerFactory());
        conf.add(
            HiddenRefreshAttributeValueHandlerFactory.TYPE
            , new HiddenRefreshAttributeValueHandlerFactory());
        conf.add(
            ActionAttributeValueHandlerFactory.TYPE
            , new ActionAttributeValueHandlerFactory());
        conf.add(
            TextActionAttributeValueHandlerFactory.TYPE
            , new TextActionAttributeValueHandlerFactory());
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
            TemporaryFileManagerValueHandlerFactory.TYPE
            , new TemporaryFileManagerValueHandlerFactory(pathResolver));
        conf.add(DataFileValueHandlerFactory.TYPE, new DataFileValueHandlerFactory(pathResolver));
        conf.add(DataStreamValueHandlerFactory.TYPE, new DataStreamValueHandlerFactory(pathResolver));
        conf.add(DataConsumerAttributeValueHandlerFactory.TYPE, new DataConsumerAttributeValueHandlerFactory());
        conf.add(ResourceReferenceValueHandlerFactory.TYPE, new ResourceReferenceValueHandlerFactory());
        conf.add(ChildAttributesValueHandlerFactory.TYPE, new ChildAttributesValueHandlerFactory());
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
            TimeZoneReferenceValues.class.getSimpleName()
            , new TimeZoneReferenceValues()
            , "after:"+CharsetReferenceValues.class.getSimpleName());
        conf.add(
            LocaleReferenceValues.class.getSimpleName()
            , new LocaleReferenceValues()
            , "after:"+TimeZoneReferenceValues.class.getSimpleName());
        conf.add(
            RecordSchemasNode.class.getSimpleName()
            , new ChildrenNodesAsReferenceValues(
                RecordSchemaValueTypeHandlerFactory.TYPE
                , pathResolver.createPath(true, SystemNode.NAME, SchemasNode.NAME, RecordSchemasNode.NAME)
                , pathResolver.createPath(false, SchemasNode.NAME, RecordSchemasNode.NAME))
            , "after:"+LocaleReferenceValues.class.getSimpleName());
        conf.add(
            ConnectionPoolsNode.class.getSimpleName()
            , new ChildrenNodesAsReferenceValues(
                ConnectionPoolValueHandlerFactory.TYPE
                , pathResolver.createPath(true, SystemNode.NAME, ConnectionPoolsNode.NAME)
                , pathResolver.createPath(false, ConnectionPoolsNode.NAME)
            )
            , "after:"+RecordSchemasNode.class.getSimpleName());
        conf.add(
            TemporaryFileManagersNode.class.getSimpleName()
            , new ChildrenNodesAsReferenceValues(
                TemporaryFileManagerValueHandlerFactory.TYPE
                , pathResolver.createPath(true, SystemNode.NAME, TemporaryFileManagersNode.NAME)
                , pathResolver.createPath(false, TemporaryFileManagersNode.NAME))
            , "after:"+ConnectionPoolsNode.class.getSimpleName());
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

//    public static void contributeAuthenticator(
//            OrderedConfiguration<Authenticator> conf, Configurator configurator) throws Exception
//    {
//        conf.add(BasicAuthenticator.class.getName(), new BasicAuthenticator(configurator.getConfig()));
//    }
//
    public static void contributeCacheManager(MappedConfiguration<CacheScope, Cache> conf)
    {
        conf.add(CacheScope.GLOBAL, new SimpleCache());
    }

//    public static void contributeContextConfiguratorService(
//            Configuration<UserContextConfigurator> conf, Tree tree)
//    {
////        conf.add(new RavenUserContextConfigurator(tree));
//    }
    
    public static void contributeTreeListeners(Configuration<TreeListener> listeners
            , ResourceManager resourceManager, TreeNodesBuilder nodesBuilder) 
    {
        listeners.add(resourceManager);
        listeners.add(nodesBuilder);
    }
    
    public static void contributeTreeNodesBuilder(Configuration<NodeBuildersProvider> providers,
            TemplateNodeBuildersProvider templateNodeBuildersProvider) 
    {
        providers.add(templateNodeBuildersProvider);
    }
}

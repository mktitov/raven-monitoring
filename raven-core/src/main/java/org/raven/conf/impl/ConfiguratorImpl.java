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

import java.util.Map;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.conf.ConfiguratorError;
import org.raven.tree.store.TreeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class ConfiguratorImpl implements Configurator
{
	private final static Logger logger = LoggerFactory.getLogger(Configurator.class);
//    private PersistenceManagerFactory pmf;
//    private PersistenceManager pm;
    private TreeStore treeStore;
            
    public ConfiguratorImpl(Map<String, Class> treeStoreEngines)
    {
        try
        {
            initTreeStore(treeStoreEngines);
//        initPersistenceManager();
        } catch (Exception ex)
        {
            throw new ConfiguratorError("Configurator initialization error", ex);
        }
    }
    
    public TreeStore getTreeStore()
    {
        return treeStore;
    }

    public Config getConfig() throws Exception
    {
        return PropertiesConfig.getInstance();
    }
    
//    public void beginTransaction() 
//    {
//        pm.currentTransaction().begin();
//    }
//
//    public void commit() 
//    {
//        pm.currentTransaction().commit();
//    }
//
//    public void rollback() 
//    {
//        if (pm.currentTransaction().isActive())
//            pm.currentTransaction().rollback();
//    }
//
//    public void save(Object object)
//    {
//        pm.makePersistent(object);
//    }
//
//    public void saveInTransaction(Object object)
//    {
//        pm.currentTransaction().begin();
//        try
//        {
//            pm.makePersistent(object);
//            pm.currentTransaction().commit();
//        }finally
//        {
//            if (pm.currentTransaction().isActive())
//                pm.currentTransaction().rollback();
//        }
//    }
//
//    public void delete(Object object)
//    {
//        pm.currentTransaction().begin();
//        try
//        {
//            pm.deletePersistent(object);
//            pm.currentTransaction().commit();
//        }finally
//        {
//            if (pm.currentTransaction().isActive())
//                pm.currentTransaction().rollback();
//        }
//    }
//
//    public void deleteAll(Class objectType)
//    {
//        pm.currentTransaction().begin();
//        try
//        {
//            Query query = pm.newQuery(objectType);
//            query.deletePersistentAll();
//            pm.currentTransaction().commit();
//        }finally
//        {
//            if (pm.currentTransaction().isActive())
//                pm.currentTransaction().rollback();
//        }
//    }
//
//    public <T> T getObjectById(Object id) 
//    {
//        pm.currentTransaction().begin();
//        try
//        {
//            try
//            {
//                return (T)pm.getObjectById(id);
//                
//            } catch (JDOObjectNotFoundException e)
//            {
//                return null;
//            }
//        }finally
//        {
//            pm.currentTransaction().commit();
//        }
//    }
//
//    public Object getObjectId(Object obj) 
//    {
//        return pm.getObjectId(obj);
//    }
//    
//    public <T> Collection<T> getObjects(Class<T> objectType, String orderingExpression)
//    {
//        pm.currentTransaction().begin();
//        try
//        {
//            Query query = pm.newQuery(objectType);
//            if (orderingExpression!=null)
//                query.setOrdering(orderingExpression);
//            
//            return (Collection<T>) query.execute();
//            
//        }finally
//        {
//            pm.currentTransaction().commit();
//        }
//    }

//    private void initPersistenceManager() throws Exception 
//    {
//        pmf = JDOHelper.getPersistenceManagerFactory(getConfig().getProperties());
//        pm = pmf.getPersistenceManager();
//        pm.setMultithreaded(true);
//        pm.setDetachAllOnCommit(true);
//    }
//
    private void initTreeStore(Map<String, Class> treeStoreEngines) throws Exception
    {
        Config config = getConfig();
        
        String engineName = config.getStringProperty(TREE_STORE_ENGINE, H2_TREE_STORE_ENGINE);
        Class engineClass = treeStoreEngines.get(engineName);
        
        if (engineClass==null)
            throw new ConfiguratorError(String.format(
                    "Tree store engine class not found for engine name (%s)"
                    , engineName));
        
        treeStore = (TreeStore) engineClass.newInstance();
        
        String url = config.getStringProperty(TREE_STORE_URL, null);
        String user = config.getStringProperty(TREE_STORE_USER, null);
        String passwd = config.getStringProperty(TREE_STORE_PASSWORD, null);
        
        treeStore.init(url, user, passwd);
        if (logger.isInfoEnabled())
            logger.info(String.format("Tree store (%s) initialized", engineClass.getName()));
    }

    public void close() throws Exception
    {
        if (logger.isInfoEnabled())
            logger.info("Shutdowning clonfigurator");
        if (treeStore!=null)
            try{
                treeStore.close();
            }catch(Exception e)
            {
                logger.error("Configurator shutdown error", e);
            }
        if (logger.isInfoEnabled())
            logger.info("Configurator shutdowned");
    }

}

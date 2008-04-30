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

import java.util.Collection;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.tree.Tree;

/**
 *
 * @author Mikhail Titov
 */
public class ConfiguratorImpl implements Configurator
{
    private PersistenceManagerFactory pmf;
    private PersistenceManager pm;
    private Tree tree;
            
    public ConfiguratorImpl() throws Exception
    {
        initPersistenceManager();
//        pm.setDetachAllOnCommit(true);
    }
    
    public Tree getTree()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void beginTransaction() 
    {
        pm.currentTransaction().begin();
    }

    public void commit() 
    {
        pm.currentTransaction().commit();
    }

    public void rollback() 
    {
        if (pm.currentTransaction().isActive())
            pm.currentTransaction().rollback();
    }

    public void save(Object object)
    {
        pm.makePersistent(object);
    }

    public void saveInTransaction(Object object)
    {
        pm.currentTransaction().begin();
        try
        {
            pm.makePersistent(object);
            pm.currentTransaction().commit();
        }finally
        {
            if (pm.currentTransaction().isActive())
                pm.currentTransaction().rollback();
        }
    }

    public <T> T getObjectById(Object id) 
    {
        return (T)pm.getObjectById(id);
    }

    public Object getObjectId(Object obj) 
    {
        return pm.getObjectId(obj);
    }
    
    public Config getConfig() throws Exception
    {
        return PropertiesConfig.getInstance();
    }
    
    public <T> Collection<T> getObjects(Class<T> objectType, String orderingExpression)
    {
        pm.currentTransaction().begin();
        try
        {
            Query query = pm.newQuery(objectType);
            if (orderingExpression!=null)
                query.setOrdering(orderingExpression);
            
            return (Collection<T>) query.execute();
            
        }finally
        {
            pm.currentTransaction().commit();
        }
    }

    private void initPersistenceManager() throws Exception 
    {
        pmf = JDOHelper.getPersistenceManagerFactory(getConfig().getProperties());
        pm = pmf.getPersistenceManager();
        pm.setMultithreaded(true);
        pm.setDetachAllOnCommit(true);
    }

}

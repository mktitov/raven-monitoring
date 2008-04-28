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

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
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
            
    public ConfiguratorImpl() throws Exception
    {
        pmf = JDOHelper.getPersistenceManagerFactory(getConfig().getProperties());
    }
    
    public Tree getTree()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void save(Object object)
    {
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        tx.begin();
        try
        {
            pm.makePersistent(object);
            tx.commit();
        }finally
        {
            if (tx.isActive())
                tx.rollback();
        }
    }

    public Config getConfig() throws Exception
    {
        return PropertiesConfig.getInstance();
    }

}

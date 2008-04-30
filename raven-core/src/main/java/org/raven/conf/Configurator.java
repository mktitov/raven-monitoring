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

package org.raven.conf;

import java.util.Collection;
import org.raven.tree.Tree;

/**
 * The goal of the configurator is to store and restore the application state.
 * 
 * @author Mikhail Titov
 */
public interface Configurator 
{
    /**
     * Returns the tree of nodes.
     */
    public Tree getTree();
    /**
     * Starts new transaction.
     * @see #commit() 
     * @see #rollback() 
     */
    public void beginTransaction();
    /**
     * Commits started transaction.
     * @see #beginTransaction()
     * @see #rollback() 
     */
    public void commit();
    /**
     * Rollbacks started transaction.
     * @see #beginTransaction()
     * @see #commit() 
     */
    public void rollback();
    /**
     * Saves the state of the object in configuration database. The call of this method must be 
     * wrapped between method calls: {@link #beginTransaction()} and {@link #commit()} or 
     * {@link #beginTransaction()} and {@link #rollback()}
     * @param object object 
     */
    public void save(Object object);
    public void saveInTransaction(Object object);
    /**
     * Returns the object by it id.
     * @param id the object id
     */
    public <T> T getObjectById(Object id);
    /**
     * Returns the id of the object.
     */
    public Object getObjectId(Object obj);
    
    public <T> Collection<T> getObjects(Class<T> objectType, String orderingExpression);
    /**
     * Returns the storage of the configuration parameters.
     */
    public Config getConfig() throws Exception;
}

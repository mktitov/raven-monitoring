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

import org.raven.tree.store.TreeStore;

/**
 * The goal of the configurator is to store and restore the application state.
 * 
 * @author Mikhail Titov
 */
public interface Configurator 
{
	/**
	 * The name of parameter that stores URL of LDAP server.  
	 */
	public static final String PROVIDER_URL = "auth.providerURL";
	/**
	 * The name of parameter that stores context for search on LDAP server.  
	 */
	public static final String SEARCH_CONTEXT = "auth.searchContext";
	/**
	 * The name of parameter that stores login for bind to LDAP server.  
	 */
	public static final String BIND_NAME = "auth.bindName";
	/**
	 * The name of parameter that stores login for bind to domain.  
	 */
	public static final String ACCOUNT_NAME = "auth.accountName";
	/**
	 * The name of parameter that stores password for bind to LDAP server.  
	 */
	public static final String BIND_PASSWORD = "auth.bindPassword";
	/**
	 * The name of parameter that stores name of Windows domain.  
	 */
	public static final String WIN_DOMAIN = "auth.domain";
    /**
     * The name of the parameter that stores the list of WINS servers.
     */
    public static final String WINS_SERVERS = "auth.winsServers";
    /**
     * The name of the parameter that stores the domain controller.
     */
    public static final String DOMAIN_CONTROLLER = "auth.domainController";
    /**
     * The name of the parameter that stores the tree store engine.
     */
    public static final String TREE_STORE_ENGINE = "tree.store.engine";
    /**
     * The name of the parameter that holds the url to the tree database.
     */
    public static final String TREE_STORE_URL = "tree.store.url";
    /**
     * The name of the parameter that holds the user name for access to the tree database.
     */
    public static final String TREE_STORE_USER = "tree.store.user";
    /**
     * The name of the parameter that holds the password of the {@link #TREE_STORE_USER}.
     */
    public static final String TREE_STORE_PASSWORD = "tree.store.password";
    /**
     * The name of the h2database tree store engine.
     */
    public static final String H2_TREE_STORE_ENGINE = "h2";
    /**
     * 
     */
    public static final String RRD_DATABASES_PATH = "rrd.databasesPath";
	
    /**
     * Returns the tree store.
     */
    public TreeStore getTreeStore();
    /**
     * Returns the storage of the configuration parameters.
     */
    public Config getConfig() throws Exception;
//    /**
//     * Starts new transaction.
//     * @see #commit() 
//     * @see #rollback() 
//     */
//    public void beginTransaction();
//    /**
//     * Commits started transaction.
//     * @see #beginTransaction()
//     * @see #rollback() 
//     */
//    public void commit();
//    /**
//     * Rollbacks started transaction.
//     * @see #beginTransaction()
//     * @see #commit() 
//     */
//    public void rollback();
//    /**
//     * Saves the state of the object in configuration database. The call of this method must be 
//     * wrapped between method calls: {@link #beginTransaction()} and {@link #commit()} or 
//     * {@link #beginTransaction()} and {@link #rollback()}
//     * @param object object 
//     */
//    public void save(Object object);
//    /**
//     * Saves the state of the object in configuration database. Method itself calls methods
//     * {@link #beginTransaction()} and {@link #commit()} or {@link #rollback()} 
//     */
//    public void saveInTransaction(Object object);
//    /**
//     * Deletes object from configuration database. Method itself calls methods
//     * {@link #beginTransaction()} and {@link #commit()} or {@link #rollback()} 
//     */
//    public void delete(Object object);
//    /**
//     * Method deletes all objects of the type passed in parameter.
//     * @param objectType the object type.
//     */
//    public void deleteAll(Class objectType);
//    /**
//     * Returns the object by its id. Method returns <code>null</code> if no object found by id
//     * passed in parameter.
//     * @param id the object id
//     */
//    public <T> T getObjectById(Object id);
//    /**
//     * Returns the id of the object.
//     */
//    public Object getObjectId(Object obj);
//    
//    public <T> Collection<T> getObjects(Class<T> objectType, String orderingExpression);
}

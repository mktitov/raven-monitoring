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

package org.raven.tree.store;

import java.io.InputStream;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;


/**
 *
 * @author Mikhail Titov
 */
public interface TreeStore 
{
    /**
     * Initialize store
     * @throws TreeStoreError on any error.
     */
    public void init(String databaseUrl, String username, String password) throws TreeStoreError;
    /**
     * Closes the store
     * @throws org.raven.tree.store.TreeStoreError
     */
    public void close() throws TreeStoreError;
    /**
     * Saves node in the store.
     * @throws TreeStoreError on any error.
     */
    public void saveNode(Node node) throws TreeStoreError;
    /**
     * Returns node by id or <code>null</code> if no node with specefied id in the store.
     * @throws org.raven.tree.store.TreeStoreError
     */
    public Node getNode(int id) throws TreeStoreError;
    /**
     * Removes the node by its id from the store.
     * @throws org.raven.tree.store.TreeStoreError
     */
    public void removeNode(int id) throws TreeStoreError;
    /**
     * Removes all nodes from the tree store.
     * @throws org.raven.tree.store.TreeStoreError
     */
    public void removeNodes() throws TreeStoreError;
    /**
     * Saves node attribute in the store.
     * @throws TreeStoreError on any error.
     */
    public void saveNodeAttribute(NodeAttribute nodeAttribute) throws TreeStoreError;
    /**
     * Saves the attribute binary data
     * @param attr the attribute which binary data must be saved
     * @param data the input stream which contains binary data
     */
    public void saveNodeAttributeBinaryData(NodeAttribute attr, InputStream data);
    /**
     * Returns <b>true</b> if the attribute has binary data
     * @param attr node attribute
     */
    public boolean hasNodeAttributeBinaryData(NodeAttribute attr);
    /**
     * Returns the binary data of the attribute or null if the attribute has not binary data
     * @param attr Node attribute
     */
    public InputStream getNodeAttributeBinaryData(NodeAttribute attr);
    /**
     * Removes the node attribute by its id.
     * @throws org.raven.tree.store.TreeStoreError
     */
    public void removeNodeAttribute(int id) throws TreeStoreError;
    /**
     * Returns all nodes sorted by {@link org.raven.tree.Node#getLevel()}
     * @throws TreeStoreError on any error.
     */
    public Node getRootNode() throws TreeStoreError;
}

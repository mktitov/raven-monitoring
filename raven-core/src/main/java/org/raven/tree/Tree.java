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

package org.raven.tree;

import java.util.List;

/**
 * The tree of nodes. 
 * 
 * @author Mikhail Titov
 */
public interface Tree 
{
    public final static String SYSTEM_NODE_DISCRIMINATOR = "1";
    public final static String DATASOURCES_NODE_DESCRIMINATOR = "2";
    public final static String DATASOURCE_NODE_DESCRIMINATOR = "3";
    public final static String CONTAINER_NODE_DESCRIMINATOR = "4";
    /**
     * Returns the root node of the tree
     */
    public Node getRootNode();
    /**
     * Look up the node by it path.
     * @param path the path to the node.
     */
    public Node getNode(String path) throws NodeNotFoundError;
    /**
     * Remove node passed in parameter and all child nodes
     */
    public void remove(Node node);
    /**
     * Reloads tree from tree store.
     */
    public void reloadTree();
    /**
     * Returns all classes marked with {@link org.raven.annotations.NodeClass} annotation. 
     */
    public Class[] getAvailableNodesTypes();
    /**
     * Returns attributes types available for the node .
     */
    public Class[] getNodeAttributesTypes(Node node);
    /**
     * Returns reference values for attribute or <code>null</code> if reference values for
     * attribute was not defined.
     */
    public List<String> getReferenceValuesForAttribute(NodeAttribute attr);
}

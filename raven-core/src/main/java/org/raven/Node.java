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

/**
 * The base interface of the observable object tree.
 * 
 * @author Mikhail Titov
 */
public interface Node<T>
{
    /**
     * The separator char between nodes names in the path
     * @see #getPath() 
     */
    public static String NODE_SEPARATOR = "/";
    
    /**
     * Returns the parent node for this node. For root node method returns null.
     */
    public Node getParentNode();
    /**
     * Returns the name of the node.
     */
    public String getName();
    /**
     * Returns the path to this node from the root. The node name separator is "/".
     */
    public String getPath();
    /**
     * Returns nodes that belongs to this node.
     */
    public List<Node> getChildrens();
    /**
     * Returns the array of nodes types that can belong to this node type.
     */
    public Class[] getChildNodeTypes();
    /**
     * Returns node attributes
     */
    public List<NodeAttribute> getNodeAttributes();
    /**
     * Returns the type of the wrapped object
     */
    public Class<? extends T> getNodeLogicType();
    /**
     * Returns wrapped object
     */
    public T getNodeLogic();
    /**
     * Initializing the node
     */
    public void init() throws NodeInitializationError;
    /**
     * Returns the initialization priority of the node. 
     * The lower priority is stronger
     */
    public int getInitializationPriority();
    /**
     * Returns true if node was initialized (method {@link #init()} successfuly executed).
     */
    public boolean isInitialized();
}

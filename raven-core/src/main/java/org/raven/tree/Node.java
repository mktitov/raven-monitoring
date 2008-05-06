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

import java.util.Collection;
import java.util.Set;

/**
 * The base interface of the observable object tree.
 * 
 * @author Mikhail Titov
 */
public interface Node<T extends NodeLogic>
{
    /**
     * The separator char between nodes names in the path
     * @see #getPath() 
     */
    public static String NODE_SEPARATOR = "/";
    /**
     * Returns unique node id
     */
    public int getId();
    /**
     * Sets the node id.
     */
    public void setId(int id);
    /**
     * Returns the node level in the tree. Level 0 is a root node.
     */
    public byte getLevel();
    /**
     * Returns the parent node for this node. For root node method returns null.
     */
    public Node getParent();
    /**
     * Sets the current node parent.
     * @param parent
     */
    public void setParent(Node parent);
    /**
     * Returns the name of the node.
     */
    public String getName();
    /**
     * Sets the node name.
     */
    public void setName(String name);
    /**
     * Returns the path to this node from the root. The node name separator is "/".
     */
    public String getPath();
    /**
     * Adds children node to this node. 
     * @throws NodeInitializationError if this node is not a {@link #isContainer() container}
     *      or node type not one of the types returned by method {@link #getChildNodeTypes()}
     * @param node the children node.
     */
    public void addChildren(Node node);
    /**
     * Returns nodes that belongs to this node. Method returns <code>null</code> if this node
     * has not childrens.
     */
    public Collection<Node> getChildrens();
    /**
     * Returns the children node by its name or <code>null</code> if no children node with specified
     * name.
     * @param name the children node name
     */
    public Node getChildren(String name);
    /**
     * Returns the array of nodes types that can belong to this node type. If method returns null
     * then this node can hold any node type.
     * 
     * @see #addChildren(org.raven.tree.Node) 
     * @see #isContainer() 
     */
    public Class[] getChildNodeTypes();
    /**
     * Returns <code>true</code> if this node can hold children nodes. 
     * @see #getChildNodeTypes() 
     * @see #addChildren(org.raven.tree.Node) 
     */
    public boolean isContainer();
    /**
     * Adds new attribute to the node.
     */
    public void addNodeAttribute(NodeAttribute attr);
    /**
     * Returns node attributes
     */
    public Collection<NodeAttribute> getNodeAttributes();
    /**
     * Returns node attribute by its name or null if node does not contains the attribute with name
     * passed in parameter <code>name</code>.
     * @param name the attribute name
     */
    public NodeAttribute getNodeAttribute(String name);
    /**
     * Initializing the node.
     */
    public void init() throws NodeError;
    /**
     * Shutdowns only this node. 
     * @throws org.raven.tree.NodeShutdownError
     */
    public void shutdown() throws NodeShutdownError;
    /**
     * Returns true if node was initialized (method {@link #init()} successfuly executed).
     */
    public boolean isInitialized();
    /**
     * This node calls method {@link Node#init() dependentNode.init} after self initialization.
     * @param dependentNode the node that must be initialized after this node.
     */
    public void addDependentNode(Node dependentNode);
    /**
     * Returns the collection of dependent nodes or null if no nodes dependent on this.
     */
    public Set<Node> getDependentNodes();
    /**
     * If method returns <code>true</code> then node permits read only operations.
     */
    public boolean isReadOnly();
}

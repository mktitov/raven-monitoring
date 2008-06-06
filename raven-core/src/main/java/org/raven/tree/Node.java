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
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.weda.annotations.Description;

/**
 * The base interface of the observable object tree.
 * <p/>
 * Node lifecycle:
 * <ul>
 *  <li><i>creation/restoration</i>. At the end of this phase node created and its attributes linked
 *      with the node.</li>
 *  <li><i>initialization</i> ({@link #init()}). At the end of this phase 
 *      <ul>
 *          <li>the node status is {@link Status#INITIALIZED}</li>
 *          <li>all nodes from which this node is depend on were initialized.
 *          <li>node parameters extracted from the node class</li>
 *          <li>parameter values were seted from corresponding attribues values</li>
 *          <li>attributes where created and stored in the 
 *              {@link org.raven.tree.store.TreeStore tree database} for parameters that has not
 *              corresponding attributes.
 *      </ul>
 *  </li>
 *  <li><i>logic execution</i> ({@link #start()}). 
 *      <ul>
 *          <li>the node status is {@link Status#STARTED}</li>
 *          <li>Node going to this phase automaticaly if the 
 *          <li><font color="red">The node can not enter to this phase 
 *              until values of all {@link NodeAttribute#isRequired() required} attributes 
 *              will not be seted</font>. 
 *          </li>
 *          <li>At this point of the lifecycle node must realize its own logic.
 *          </li>
 *          <li>User can execute node actions.</li>
 *          
 *      </ul>
 *  </li>
 *  <li><i>logic stoping</i> ({@link #stop()}. Node status is {@link Status#INITIALIZED}</li>
 *  <li>node shutdown ({@link #shutdown()})</li>
 * </ul>
 * 
 * @author Mikhail Titov
 */
@Description("The node type")
public interface Node extends Cloneable
{
    public enum Status {CREATED, INITIALIZED, STARTED}
    /**
     * The separator char between nodes names in the path
     * @see #getPath() 
     */
    public static String NODE_SEPARATOR = "/";
    public static String ATTRIBUTE_SEPARATOR = "#";
    
    /**
     * Returns the node logger.
     */
    public Logger getLogger();
    /**
     * Returns unique node id.
     */
    public int getId();
    /**
     * Sets the node id.
     */
    public void setId(int id);
    /**
     * Returns the status of the current node. 
     */
    public Status getStatus();
    /**
     * Returns the node level in the tree. Level 0 is a root node.
     */
    public byte getLevel();
    /**
     * Returns the position of this node in the {@link #getChildrens() parent childrens list}.
     */
    public int getIndex();
    /**
     * Sets the position of this node in the {@link #getChildrens() parent childrens list}.
     */
    public void setIndex(int index);
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
     * Removes children node from this node.
     * @param node the node which must be removed from this node.
     */
    public void removeChildren(Node node);
    /**
     * Returns nodes that belongs to this node. Method returns <code>null</code> if this node
     * has not childrens.
     */
    public Collection<Node> getChildrens();
    /**
     * Returns children nodes sorted by {@link #getIndex() index}. Method returns <code>null</code>
     * if this node has not childrens.
     */
    public Collection<Node> getSortedChildrens();
    /**
     * Returns the children node by its name or <code>null</code> if no children node with specified
     * name.
     * @param name the children node name
     */
    public Node getChildren(String name);
    /**
     * Adds listener to the node.
     */
    public void addListener(NodeListener listener);
    /**
     * Removes listener from node.
     */
    public void removeListener(NodeListener listener);
    /**
     * Returns this node listeners or null if this node has not listeners.
     */
    public Collection<NodeListener> getListeners();
    /**
     * Returns the array of nodes types that can belong to this node type. 
     * 
     * @see #addChildren(org.raven.tree.Node) 
     * @see #isContainer() 
     */
    public List<Class> getChildNodeTypes();
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
     * Adds node attribute dependency.
     * @param attributeName attribute name.
     * @param listener the node attribute listener.
     */
    public void addNodeAttributeDependency(String attributeName, NodeAttributeListener listener);
    /**
     * Removes node attribute dependency.
     */
    public void removeNodeAttributeDependency(String attributeName, NodeAttributeListener listener);
    /**
     * Removes node attribute by its name.
     */
    public void removeNodeAttribute(String name);
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
     * If returns <code>true</code> then node will be initialized after child nodes else before.
     */
    public boolean isInitializeAfterChildrens();
    /**
     * If method returns <code>true</code> then the method {@link #start()} will executed 
     * automaticaly after {@link #init()} method.
     */
    public boolean isAutoStart();
    /**
     * Initializing the node.
     */
    public void init() throws NodeError;
    /**
     * This method is calling when node {@link Tree#remove(org.raven.tree.Node) is removing} 
     * from the tree. Method calls {@link #stop()} method if the node status is 
     * {@link Status#STARTED} and remove any dependencies from the nodes from which this node is
     * depend on. The node must not be used after this operation.
     * Shutdowns only this node. 
     * @throws org.raven.tree.NodeShutdownError
     */
    public void shutdown() throws NodeShutdownError;
    /**
     * Starts the node. This method automaticaly calling after {@link #init() node initialization}
     * if the {@link #isAutoStart autoStart} property is setted to <code>true</code>
     * @throws org.raven.tree.NodeError
     * @return <code>true</code> if node successfully started
     */
    public boolean start() throws NodeError;
    /**
     * Stops the node. The status after this operation switching to {@link Status#INITIALIZED}.
     * @throws org.raven.tree.NodeError
     */
    public void stop() throws NodeError;
    /**
     * This node calls method {@link Node#init() dependentNode.init} after self initialization.
     * @param dependentNode the node that must be initialized after this node.
     * @return <code>true</code> if node successfully added. <code>False</code> if node already
     *        in dependents list
     */
    public boolean addDependentNode(Node dependentNode);
    /**
     * Removes node from the dependency list of this node.
     * @return <code>true</code> if node successfully removed.
     */
    public boolean removeDependentNode(Node dependentNode);
    /**
     * Returns the collection of dependent nodes or null if no nodes dependent on this.
     */
    public Set<Node> getDependentNodes();
    /**
     * If method returns <code>true</code> then node permits read only operations.
     */
//    public boolean isReadOnly();
    /**
     * Method returns the first not null value of the attribute, with name passed in the 
     * <code>attributeName</code> parameter, of the nearest parent or null if parents does not
     * contains the attribute with name passed in the parameter.
     * @param attributeName the name of the attribute
     */
    public String getParentAttributeValue(String attributeName);
    /**
     * Method returns the first not null real value (the value of the 
     * {@link NodeAttribute#getType() attribute type}) 
     * of the attribute, with name passed in the 
     * <code>attributeName</code> parameter, of the nearest parent or null if parents does not
     * contains the attribute with name passed in the parameter.
     * @param attributeName the name of the attribute
     */
    public <T> T getParentAttributeRealValue(String attributeName);
    
    public List<Node> getChildrenList();
    /**
     * Returns <code>true</code> if this node is in the template.
     */
    public boolean isTemplate();
    
    /**
     * Clones the node.
     * @throws java.lang.CloneNotSupportedException
     */
    public Object clone() throws CloneNotSupportedException;
    
}

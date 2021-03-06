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

import groovy.lang.Closure;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import org.raven.log.LogLevel;
import org.raven.tree.store.TreeStore;
import org.slf4j.Logger;

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
//@Description("The node type")
public interface Node extends Cloneable, Comparable<Node>, PathObject
{
    public enum Status {CREATED, INITIALIZED, STARTED, REMOVING, REMOVED, SHUTDOWNED}
    /**
     * The separator char between nodes names in the path
     * @see #getPath() 
     */
    public static char NODE_SEPARATOR = '/';
    public static char ATTRIBUTE_SEPARATOR = '@';
    
    /**
     * Returns current log level for the node
     */
    public LogLevel getLogLevel();
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
     * Returns <b>true</b> if {@link #getStatus() } equals to {@link Status#STARTED}
     */
    public boolean isStarted();
    /**
     * Returns <b>true</b> if {@link #getStatus() } equals to {@link Status#INITIALIZED}
     */
    public boolean isInitialized();
    /**
     * Sets the new node status.
     */
    public void setStatus(Status status);
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
     * Returns <code>true</code> if this node is dynamic (must not be stored in the database)
     */
    public boolean isDynamic();
    /**
     * Returns <code>true</code> if childrens of this node are dynamic (must not be stored
     * in the database)
     */
    public boolean isChildrensDynamic();
    /**
     * Adds children node to this node. 
     * @throws NodeInitializationError if this node is not a {@link #isContainer() container}
     *      or node type not one of the types returned by method {@link #getChildNodeTypes()}
     * @param node the children node.
     */
    public void addChildren(Node node);
    /**
     * Adds children node to this node save it and makes initialization.
     * Returns the added
     * @param node the children node
     */
    public void addAndSaveChildren(Node node);
    /**
     * Removes children node from this node.
     * @param node the node which must be removed from this node.
     */
    public void removeChildren(Node node);
    /**
     * Detaches children node from this node.
     */
    public void detachChildren(Node node);
    /**
     * @deprecated use {@link #getNodes()}
     */
    public Collection<Node> getChildrens();
    /**
     * Returns child nodes sorted by {@link #getIndex() index}. Method returns an empty list if node does 
     * has not children
     */
    public List<Node> getNodes();
    /**
     * Returns <b>true</b> if this node has child node with name passed in the parameter
     * @param name node name
     */
    public boolean hasNode(String name);
    /**
     * recursion search for first node that satisfy the filter
     */
    public Node find(Closure<Boolean> filter);
    /**
     * recursion search for all nodes that satisfy the filter
     */
    public List<Node> findAll(Closure<Boolean> filter);
    /**
     * @deprecated use {@link #getNodesCount()}
     */
    public int getChildrenCount();
    /**
     * Returns children <b>count</b> in this node or <b>zero</b> if node has not children.
     */
    public int getNodesCount();
    /**
     * @deprecated use {@link #getNodes()}
     */
    public List<Node> getChildrenList();
    
    /**
     * @deprecated use {@link #getNodes()}
     */
    public List<Node> getSortedChildrens();
    /**
     * Return <code>true</code> if this node is conditional.
     * @see #getEffectiveChildrens()
     */
    public boolean isConditionalNode();
    /**
     * Returns the first {@link #isConditionalNode() non conditional} parent node
     * in the chain of the parent nodes.
     */
    public Node getEffectiveParent(); 
    /**
     * @deprecated use {@link #getEffectiveNodes()}
     */
    public Collection<Node> getEffectiveChildrens();
    /**
     * Returns the children of this node excluding {@link #isConditionalNode() conditional nodes}
     * and the {@link #getEffectiveChildrens() effective childrens} of the conditional nodes. 
     * Returned nodes are sorted by the {@link #getIndex() index property} and effective childrens 
     * from the {@link #isConditionalNode() conditional nodes} are inserted to the returned list at
     * the conditional node position.
     */
    public Collection<Node> getEffectiveNodes();
    /**
     * @deprecated use {@link #getNode(java.lang.String) }
     */
    public Node getChildren(String name);
    /**
     * Returns the child node by its name or <code>null</code> if no children node with specified
     * name.
     * @param name the children node name
     */
    public Node getNode(String name);
    /**
     * Returns the child node by its id or <code>null</code> if no child node with specified id.
     * @param id the child node id
     */
    public Node getNodeById(int id);
    /**
     * @deprecated use {@link #getNodeByPath(java.lang.String)} 
     */
    public Node getChildrenByPath(String path);
    /**
     * Returns child by path relative to this node
     */
    public Node getNodeByPath(String path);
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
     * Returns the array of nodes types that can belong to this node type. Method never returns 
     * <code>null</code>
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
     * @deprecated use {@link #addAttr(org.raven.tree.NodeAttribute)}
     */
    public void addNodeAttribute(NodeAttribute attr);
    /**
     * Adds new attribute to the node.
     */
    public void addAttr(NodeAttribute attr);
    /**
     * Return existing attribute or create new attribute based on attribute with name passed in the
     * parameter <b>protoAttrName</b>. Algorithm:
     * <ul>
     * <li>Search for prototype attribute with name <b>protoAttrName</b>. If not found throws exception
     * <li>Search for all attributes which name starts with <b>protoAttrName</b> and if find any with value
     *     that equals to <b>value</b> then returns this attribute
     * <li>Else creates new attribute (based on prototype attribute):
     *      <ul>
     *      <li>with name that starts with the name of prototype attribute name plus index (integer number).
     *      <li>attribute type and value handler type will be copied from prototype attribute
     *      <li>The parameter <b>value</b> will be converted to string and passed to attribute as it's value
     *      </ul>
     * </ul>
     * @param protoAttrName the name of the prototype attribute
     * @param value the value of the attribute
     * @throws Exception 
     */
    public NodeAttribute addUniqAttr(String protoAttrName, Object value) throws Exception;
    public NodeAttribute addUniqAttr(String protoAttrName, Object value, boolean reuseAttrWithNullValue) throws Exception;
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
     * @deprecated use {@link #removeAttr(java.lang.String)}
     */
    public void removeNodeAttribute(String name);
    /**
     * Removes node attribute by its name.
     */
    public void removeAttr(String name);
    /**
     * @deprecated use {@link #getAttrs()} instead
     */
    public Collection<NodeAttribute> getNodeAttributes();
    /**
     * Returns node attributes
     */
    public Collection<NodeAttribute> getAttrs();
    /**
     * Returns <b>true</b> if this node contains attribute with name passed in the parameter
     * @param name attribute name
     */
    public boolean hasAttr(String name);
    /**
     * @deprecated Use {@link #getAttr(java.lang.String)} instead
     */
    public NodeAttribute getNodeAttribute(String name);
    /**
     * Returns node attribute by its name or null if node does not contains the attribute with name
     * passed in parameter <code>name</code>.
     * @param name the attribute name
     */
    public NodeAttribute getAttr(String name);
    /**
     * If returns <code>true</code> then node will be initialized after child nodes else before.
     */
    public boolean isInitializeAfterChildrens();
    /**
     * If returns <code>true</code> then node will be started after child nodes else before.
     */
    public boolean isStartAfterChildrens();
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
     * Method is calling when node is removing from the parent node.
     */
    public void remove();
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
    public boolean addDependentNode(Node dependentNode, Object dependencyOwner);
    /**
     * Removes node from the dependency list of this node.
     * @return <code>true</code> if node successfully removed.
     */
    public boolean removeDependentNode(Node dependentNode, Object dependencyOwner);
    /**
     * Returns the collection of dependent nodes or null if no nodes dependent on this.
     */
    public Set<Node> getDependentNodes();
    /**
     * @deprecated use {@link #getParentAttr(java.lang.String)}
     */
    public NodeAttribute getParentAttribute(String attributeName);
    /**
     * Method returns the first attribute, with name passed in the 
     * <code>attributeName</code> parameter, of the nearest parent or null if parents does not
     * contains the attribute with name passed in the parameter.
     * @param attributeName the name of the attribute
     */
    public NodeAttribute getParentAttr(String attributeName);
    /**
     * @deprecated use {@link #getParentAttrValue(java.lang.String) 
     */
    public String getParentAttributeValue(String attributeName);
    /**
     * Method returns the first not null value of the attribute, with name passed in the 
     * <code>attributeName</code> parameter, of the nearest parent or null if parents does not
     * contains the attribute with name passed in the parameter.
     * @param attributeName the name of the attribute
     */
    public String getParentAttrValue(String attributeName);
    /**
     * @deprecated use {@link #getParentAttrRealValue(java.lang.String)}
     */
    public <T> T getParentAttributeRealValue(String attributeName);
    /**
     * Method returns the first not null real value (the value of the 
     * {@link NodeAttribute#getType() attribute type}) 
     * of the attribute, with name passed in the 
     * <code>attributeName</code> parameter, of the nearest parent or null if parents does not
     * contains the attribute with name passed in the parameter.
     * @param attributeName the name of the attribute
     */
    public <T> T getParentAttrRealValue(String attributeName);
    
    public boolean isLogLevelEnabled(LogLevel level);
    /**
     * Returns <code>true</code> if this node is in the template.
     */
    public boolean isTemplate();
    /**
     * Returns the template node to which this node belongs or <code>null</code> if this node 
     * does not belong to template.
     */
    public Node getTemplate();
    /**
     * Saves node in the {@link TreeStore tree store} database.
     */
    public void save();
    /**
     * Clones the node.
     * @throws java.lang.CloneNotSupportedException
     */
    public Object clone() throws CloneNotSupportedException;
    /**
     * Copy this node to the destination node passed in the parameter.
     * @param dest the destination node.
     * @param newNodeName if specified then the copied node will get the new name
     * @param useEffectiveChildrens if setted to <code>true</code> then method 
     *      {@link #getEffectiveChildrens()} will be used to clone children nodes of this node.
     * @throws java.lang.CloneNotSupportedException
     */
    public Node cloneTo(
            Node dest, String newNodeName, NodeTuner nodeTuner, boolean useEffectiveChildrens) 
        throws CloneNotSupportedException;
    /**
     * Method adds expression bindings to the bindings object passed in the parameter.
     */
    public void formExpressionBindings(Bindings bindings);
    /**
     * Returns the map that may be used to store data while node started.
     */
    public Map<String, Object> getVariables();
}

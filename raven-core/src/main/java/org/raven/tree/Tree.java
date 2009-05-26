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
import org.raven.expr.impl.BindingSupportImpl;
import org.weda.constraints.ReferenceValue;
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
    public Node getNode(String path) throws InvalidPathException;
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
    public List<Class> getChildNodesTypes(Node node);
    /**
     * Returns attributes types available for the node .
     */
    public Class[] getNodeAttributesTypes(Node node);
    /**
     * Returns reference values for attribute or <code>null</code> if reference values for
     * attribute was not defined.
     */
    public List<ReferenceValue> getReferenceValuesForAttribute(NodeAttribute attr);
    /**
     * Copies subtree of nodes starting from the <code>source</code> node 
     * to the <code>destination</code> node.
     * @param source the source node
     * @param destination the node to wich the source will be copied
     * @param newNodeName if not null then this name will be seted to the new node
     * @param nodeTuner allows to tune node parameters in copy process
     * @param store if seted to <code>true</code> then new node will be stored in the tree database
     * @param validateNodeType if seted to <code>true</code> and the type of the <code>source</code>
     *      node is not {@link Node#getChildNodeTypes() a valid child type} 
     *      for the <code>destination</code> node then {@link TreeError} exception will be throwed.
     * @param useEffectiveChildrens if seted to <code>true</code> then method 
     *      {@link Node#getEffectiveChildrens()} will be used to clone children nodes of the <code>
     *      source</code>.
     */
    public Node copy(
            Node source, Node destination, String newNodeName, NodeTuner nodeTuner
            , boolean store, boolean validateNodeType, boolean useEffectiveChildrens);
    /**
     * Moves source node to the destination node
     * @param source the node that must be moved
     * @param destination the node to which source node will be moved
     */
    public void move(Node source, Node destination) throws TreeException;
    /**
     * Search for nodes that satisfy the node filter
     * @param options search options
     * @param filter filter options
     * @return The list of found nodes. Method can return an empty list but never null.
     */
    public List<Node> search(Node searchFromNode, SearchOptions options, SearchFilter filter);
    /**
     * Starts all nodes in the subtree starting from the <code>node</code> passed in the parameter.
     * @param node the starting point.
     * @param autoStartOnly if sets to <code>true</code> then only nodes with 
     *      {@link Node#isAutoStart() autostart} attribute seted to <code>true</code> 
     *      will be started.
     */
    public void start(Node node, boolean autoStartOnly);
    /**
     * Stops all nodes in the subtree starting from the <code>node</code> passed in the parameter.
     */
    public void stop(Node node);
    /**
     * Shutdowns the tree
     */
    public void shutdown();
    /**
     * Returns the list of the template nodes or empty list if no template nodes in the tree.
     */
    public List<Node> getTempltateNodes();
    /**
     * Returns the list of the value handler types available for the attribute.
     * Method {@link ReferenceValue#getValue()} returns the value handler type
     * and the method {@link ReferenceValue#getValueAsString()} returns the localized name
     * of the value handler.
     */
    public List<ReferenceValue> getAttributeValueHandlerTypes(NodeAttribute attr);
    /**
     * Scans every node (all childrens and all childrens of childrens) begining 
     * from <b>startingPoint</b> (excluding <code>startingPoint</code> node) and pass
     * every scaned node to the <b>handler</b>. 
     * @param startingPoint the node from which scan must begin
     * @param handler the handler to which every scanned node will be passed.
     * @param nodeTypes only the nodes of types in this array will be passed to the node handler.
     *      if the parameter was not seted then any node will be passed to the node handler.
     */
    public boolean scanSubtree(
            Node startingPoint, ScannedNodeHandler handler, ScanOptions options);
    /**
     * Adds global binding support to the tree
     * @param bindingSupportId the unique binding support identificator
     * @param bindingSupport binding support to add
     */
    public void addGlobalBindings(String bindingSupportId, BindingSupportImpl bindingSupport);
    /**
     * Removes global binding support from the tree
     * @param bindingSupportId the id of the binding support to remove
     */
    public void removeGlobalBindings(String bindingSupportId);
}

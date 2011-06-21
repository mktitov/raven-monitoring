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

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.raven.conf.Configurator;
import org.raven.expr.BindingSupport;
import org.raven.tree.store.TreeStore;
import org.raven.tree.store.TreeStoreError;
import org.weda.constraints.ReferenceValue;

/**
 * The tree of nodes. 
 * 
 * @author Mikhail Titov
 */
public interface Tree 
{
    public static final String EXPRESSION_VARS_BINDINGS = "RAVEN_EXPRESSION_VARS";
    
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
     * Remove node passed in parameter and all child nodes from the tree and from the tree store
     */
    public void remove(Node node);
    /**
     * Removes node attribute from the node and from the store.
     */
    public void removeNodeAttribute(NodeAttribute attr);
    /**
     * Saves the node state using {@link Configurator#getTreeStore() tree store service}
     * if and only if the node is not {@link Node#isDynamic() dynamic}.
     * If node is dynamic then node will not saved in tree store
     * and node will get the unique negative id number.
     * @param node the node which state must be saved
     * 
     * @see Configurator#getTreeStore()
     * @see TreeStore
     * @throws TreeStoreError
     */
    public void saveNode(Node node);
    /**
     * Saves the node attribute state using {@link Configurator#getTreeStore() tree store service}
     * if and only if the node owned by the attribute is not {@link Node#isDynamic() dynamic}
     * @param attribute the attribute which state must be saved using tree store service
     * @throws TreeStoreError
     */
    public void saveNodeAttribute(NodeAttribute attribute);
    /**
     * Saves node attribute binary data.
     * @param attribute
     * @param data
     * @throws TreeError if the owner of the attribute is {@link Node#isDynamic() dynamic}
     * @throws TreeStoreError
     */
    public void saveNodeAttributeBinaryData(NodeAttribute attribute, InputStream data);
    /**
     * Reloads tree from tree store.
     */
    public void reloadTree();
    /**
     * Returns types of all nodes defined in the system. Method never return null.
     */
    public List<Class> getNodeTypes();
    /**
     * Returns all available child node types for the node class passed in the parameter. 
     * <p/>Because of child node types are choosing from the node type
     * ({@link #getChildNodesTypes(org.raven.tree.Node) not from instance of the node}), it is not
     * possible to get child node types that offering by the parent node. But it is possible
     * {@link #getThroughNodesTypes()  to get all node types that can imports child node types from
     * the parent node}.
     * <p/>Method never returns null.
     * @param nodeType the type of the node
     */
    public List<Class> getChildNodesTypes(Class nodeType);
    /**
     * Returns all available child node types for the node passed in the parameter.
     * <p/>Method never returns null.
     * @param node the node
     */
    public List<Class> getChildNodesTypes(Node node);
    /**
     * Returns classes of nodes that imports child node types from the parent node.
     */
    public Set<Class> getThroughNodesTypes();
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
     * @param destination the node to which the source will be copied
     * @param newNodeName if not null then this name will be set to the new node
     * @param nodeTuner allows to tune node parameters in copy process
     * @param store if set to <code>true</code> then new node will be stored in the tree database
     * @param validateNodeType if set to <code>true</code> and the type of the <code>source</code>
     *      node is not {@link Node#getChildNodeTypes() a valid child type} 
     *      for the <code>destination</code> node then {@link TreeError} exception will be throw.
     * @param useEffectiveChildrens if set to <code>true</code> then method 
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
     * @param newSourceName new name of the source node
     */
    public void move(Node source, Node destination, String newSourceName) throws TreeException;
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
     * Adds global binding support to the tree. If binding already exists then bindingSupport 
     * will not be added
     * @param bindingSupportId the unique binding support identificator
     * @param bindingSupport binding support to add
     */
    public void addGlobalBindings(String bindingSupportId, BindingSupport bindingSupport);
    /**
     * Adds global bindings support to the tree and returns the unique id that you can use in the
     * {@link #getGlobalBindings(String)} or in the {@link #removeGlobalBindings(String)} methods .
     */
    public String addGlobalBindings(BindingSupport bindingSupport);
    /**
     * Returns global binding support or null if binding support for given id not exists
     * @param bindingSupportId id of the binding support
     */
    public BindingSupport getGlobalBindings(String bindingSupportId);
    /**
     * Removes global binding support from the tree
     * @param bindingSupportId the id of the binding support to remove
     */
    public void removeGlobalBindings(String bindingSupportId);
}

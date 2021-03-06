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

/**
 * The {@link Node} listener.
 * 
 * @author Mikhail Titov
 */
public interface NodeListener extends NodeAttributeListener
{
    /**
     * Returns <code>true</code> if this listener is a subtree listener (listen all children nodes. 
     */
    public boolean isSubtreeListener();
    /**
     * Informs listener that status of the node changed.
     * @param node the node which status was changed.
     * @param oldStatus the old status of the node.
     * @param newStatus the new status of the node.
     */
    public void nodeStatusChanged(Node node, Node.Status oldStatus, Node.Status newStatus);
    /**
     * Informs listener that the name of the node were changed. This event will fired only when
     * the status of the node are {@link Node.Status#INITIALIZED} or {@link Node.Status#STARTED}.
     * @param node the node where name was changed
     * @param oldName the old name of the node
     * @param newName the new name of the node
     */
    public void nodeNameChanged(Node node, String oldName, String newName);
    /**
     * Informs listeners that node was shutdowned.
     */
    public void nodeShutdowned(Node node);
    /**
     * Informs listener that the new children was added to the node
     * @param owner the node to which children was added
     * @param children the node that added to the owner
     */
    public void childrenAdded(Node owner, Node children);
    /**
     * Informs listener that dependent node was added.
     * @param node the node to which dependent node was added.
     * @param dependentNode the dependent node.
     */
    public void dependendNodeAdded(Node node, Node dependentNode);
    /**
     * Informs listener that the children removed from the node.
     * 
     * @param removedNode the node which was removed.
     */
    public void nodeRemoved(Node removedNode);
    /**
     * Informs listener that node was moved from one node to another.
     * @param node the moved node
     */
    public void nodeMoved(Node node);
    /**
     * Informs listener that {@link Node#getIndex() node index} was changed
     * @param node the node where index was changed
     * @param oldIndex the old value
     * @param newIndex the new value
     */
    public void nodeIndexChanged(Node node, int oldIndex, int newIndex);
}

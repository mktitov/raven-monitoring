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
public interface NodeListener 
{
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
     * Informs the listener that the value of one of attributes were changed.
     * This event will fired only when
     * the status of the node are {@link Node.Status#INITIALIZED} or {@link Node.Status#STARTED}
     * @param node the node in which attribute value was changed.
     * @param attribute the attribute where value was changed.
     * @param oldValue the old value
     * @param newValue the new value
     */
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, String oldValue, String newValue);
}

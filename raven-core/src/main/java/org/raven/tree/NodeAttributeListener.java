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
 * The {@link NodeAttribute node attribute} listener.
 * @author Mikhail Titov
 */
public interface NodeAttributeListener 
{
    /**
     * Informs listener that attribute name changed.
     * @param attribute the attribute which name was changed.
     * @param oldName the old attribute name.
     * @param newName the new attribute name.
     */
    public void nodeAttributeNameChanged(NodeAttribute attribute, String oldName, String newName);
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
            Node node, NodeAttribute attribute, Object oldRealValue, Object newRealValue);
    /**
     * Informs the listener that the node attribute was removed.
     * @param node the owner of the attribute.
     * @param attribute the attribute that was removed.
     * @return If listener returns <b>true</b> then listener will be removed from the event source 
     *      (from <code>node</code>)
     */
    public boolean nodeAttributeRemoved(Node node, NodeAttribute attribute);
}

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

package org.raven.tree.impl;

import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeListener;

/**
 *
 * @author Mikhail Titov
 */
public class NodeListenerAdapter implements NodeListener
{
    private final boolean subtreeListener;

    public NodeListenerAdapter(boolean subtreeListener) 
    {
        this.subtreeListener = subtreeListener;
    }

    public boolean isSubtreeListener() 
    {
        return subtreeListener;
    }

    public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus) {
    }

    public void nodeNameChanged(Node node, String oldName, String newName) {
    }

    public void nodeShutdowned(Node node) {
    }

    public void childrenAdded(Node owner, Node children) {
    }

    public void nodeRemoved(Node removedNode) {
    }

    public void nodeAttributeNameChanged(NodeAttribute attribute, String oldName, String newName) {
    }

    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldRealValue, Object newRealValue) {
    }

    public boolean nodeAttributeRemoved(Node node, NodeAttribute attribute) {
        return false;
    }

    public void dependendNodeAdded(Node node, Node dependentNode) {
    }

    public void nodeMoved(Node node) {
    }

    public void nodeIndexChanged(Node node, int oldIndex, int newIndex) {
    }
    
}

/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import org.weda.constraints.ReferenceValue;

/**
 *
 * @author Mikhail Titov
 */
public interface RemoteTree extends Remote
{
    /**
     * Returns the root node of the tree
     */
    public RemoteNode getRootNode() throws RemoteException;
    /**
     * Copies subtree of nodes starting from the <code>source</code> node
     * to the <code>destination</code> node.
     * @param source the source node
     * @param destination the node to wich the source will be copied
     * @param newNodeName if not null then this name will be seted to the new node
     * @param validateNodeType if seted to <code>true</code> and the type of the <code>source</code>
     *      node is not {@link Node#getChildNodeTypes() a valid child type}
     *      for the <code>destination</code> node then {@link TreeError} exception will be throwed.
     */
    public RemoteNode copy(RemoteNode source, RemoteNode destination, String newNodeName
            , boolean validateNodeType)
        throws RemoteException;
    /**
     * Returns the list of the value handler types available for the attribute.
     * Method {@link ReferenceValue#getValue()} returns the value handler type
     * and the method {@link ReferenceValue#getValueAsString()} returns the localized name
     * of the value handler.
     */
    public List<ReferenceValue> getAttributeValueHandlerTypes(RemoteNodeAttribute attr)
        throws RemoteException;

}

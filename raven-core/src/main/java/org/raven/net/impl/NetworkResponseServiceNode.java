/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.net.impl;

import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseNode;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.tree.impl.BaseNode;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class NetworkResponseServiceNode extends BaseNode implements NetworkResponseNode
{
    @Service
    private static NetworkResponseService networkResponseService;


    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        networkResponseService.setNetworkResponseServiceNode(this);
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        networkResponseService.setNetworkResponseServiceNode(null);
    }

    public String getResponse(String context, String requesterIp, Map<String, Object> params)
            throws NetworkResponseServiceExeption
    {
        NetworkResponseContextNode contextNode = (NetworkResponseContextNode) getChildren(context);
        if (contextNode==null || !contextNode.getStatus().equals(Status.STARTED))
            throw new ContextUnavailableException(context);
        
        return contextNode.getResponse(requesterIp, params);
    }
}

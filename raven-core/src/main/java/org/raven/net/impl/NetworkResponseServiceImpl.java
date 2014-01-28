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

import org.raven.cache.TemporaryFileManager;
import org.raven.net.NetworkResponseNode;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.NetworkResponseServiceUnavailableException;
import org.raven.net.Request;
import org.raven.net.ResponseContext;

/**
 *
 * @author Mikhail Titov
 */
public class NetworkResponseServiceImpl implements NetworkResponseService
{
    private NetworkResponseNode networkResponseServiceNode;

    public synchronized NetworkResponseNode getNetworkResponseServiceNode()
    {
        return networkResponseServiceNode;
    }

    public synchronized void setNetworkResponseServiceNode(
            NetworkResponseNode networkResponseServiceNode)
    {
        this.networkResponseServiceNode = networkResponseServiceNode;
    }
    
//    public Response getResponse(String context, String requesterIp, Map<String, Object> params)
//            throws NetworkResponseServiceExeption
//    {
//        NetworkResponseNode serviceNode = checkNetworkResponseServiceNode();
//        return serviceNode.getResponse(context, requesterIp, params);
//    }
//
//    public Authentication getAuthentication(String context, String requesterIp) throws NetworkResponseServiceExeption
//    {
//        NetworkResponseNode serviceNode = checkNetworkResponseServiceNode();
//        return serviceNode.getAuthentication(context, requesterIp);
//    }

    public ResponseContext getResponseContext(Request request) 
            throws NetworkResponseServiceExeption 
    {
        NetworkResponseNode serviceNode = checkNetworkResponseServiceNode();
        return serviceNode.getResponseContext(request);
    }
    
    private NetworkResponseNode checkNetworkResponseServiceNode()
            throws NetworkResponseServiceUnavailableException
    {
        NetworkResponseNode serviceNode = getNetworkResponseServiceNode();
        if (serviceNode == null || !serviceNode.isStarted()) 
            throw new NetworkResponseServiceUnavailableException();
        return serviceNode;
    }

    public TemporaryFileManager getTemporaryFileManager() throws NetworkResponseServiceExeption {
        NetworkResponseNode serviceNode = checkNetworkResponseServiceNode();
        return ((NetworkResponseServiceNode)serviceNode).getTemporaryFileManager();
    }
}

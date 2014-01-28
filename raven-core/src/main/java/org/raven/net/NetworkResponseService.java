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

package org.raven.net;

import org.raven.cache.TemporaryFileManager;
import org.raven.net.impl.NetworkResponseServiceNode;

/**
 *
 * @author Mikhail Titov
 */
public interface NetworkResponseService extends ResponseContextProvider
{
    public final static String REQUEST_CONTENT_PARAMETER = "request-content";
    
    public NetworkResponseNode getNetworkResponseServiceNode();

    public void setNetworkResponseServiceNode(NetworkResponseNode networkResponseNode);
    /**
     * Returns the temporary file manager that will be used to store uploaded files or null if temporary file
     * manager not assigned to {@link NetworkResponseServiceNode}
     */
    public TemporaryFileManager getTemporaryFileManager() throws NetworkResponseServiceExeption;

}

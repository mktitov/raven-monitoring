/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.net.impl;

import org.raven.auth.UserContext;
import org.raven.auth.impl.AccessRight;
import org.raven.net.NetworkResponseContext;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.Response;
import org.raven.net.ResponseBuilder;
import org.raven.net.ResponseContext;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class NetRespContextRespBuilderWrapper implements ResponseBuilder {
    private final NetworkResponseContext responseContext;

    public NetRespContextRespBuilderWrapper(NetworkResponseContext responseContext) {
        this.responseContext = responseContext;
    }

    public AccessRight getAccessRight() {
        return AccessRight.READ;
    }

    public Response buildResponse(UserContext user, ResponseContext response) 
            throws NetworkResponseServiceExeption 
    {
        return responseContext.getResponse(response.getRequest().getRemoteAddr(), response.getRequest().getParams());
    }

    public Node getResponseBuilderNode() {
        return responseContext;
    }
}

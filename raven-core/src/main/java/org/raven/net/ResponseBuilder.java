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
package org.raven.net;

import org.raven.auth.UserContext;
import org.raven.auth.impl.AccessRight;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface ResponseBuilder {
    public Response buildResponse(UserContext user, ResponseContext responseContext) 
            throws NetworkResponseServiceExeption;
    public AccessRight getAccessRight();
    public Node getResponseBuilderNode();
    public Boolean isSessionAllowed();
    public boolean canHandleUnknownPath();
    public Long getBuildTimeout();
    /**
     * Returns <b>true</b> if response builder requires audit
     */
    public Boolean getRequireAudit();  
    /**
     * Returns <b>true</b> if response builder requires secure protocol (SSL)
     */
    public Boolean getRequireSSL();
}

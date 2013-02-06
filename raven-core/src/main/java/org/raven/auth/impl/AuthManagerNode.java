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
package org.raven.auth.impl;

import org.raven.auth.AuthException;
import org.raven.auth.AuthManager;
import org.raven.auth.AuthService;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class AuthManagerNode extends BaseNode implements AuthManager{
    public final static String NAME = "Services";

    public AuthManagerNode() {
        super(NAME);
    }

    public AuthService getAuthService(String name) throws AuthException {
        Node authServiceNode = getNode(name);
        if (authServiceNode==null)
            throw new AuthException(String.format("Auth service (%s) not found", name));
        if (!authServiceNode.isStarted())
            throw new AuthException(String.format("Auth service (%s) unavailable", name));
        return (AuthService) authServiceNode;
    }
}

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
import org.raven.tree.Tree;
import org.raven.tree.impl.SystemNode;

/**
 *
 * @author Mikhail Titov
 */
public class AuthManagerService implements AuthManager {
    private final Tree tree;

    public AuthManagerService(Tree tree) {
        this.tree = tree;
    }

    public AuthService getAuthService(String name) throws AuthException {
        try {
            AuthManagerNode managerNode = (AuthManagerNode) 
                    tree.getRootNode()
                    .getNode(SystemNode.NAME)
                    .getNode(AuthorizationNode.NODE_NAME)
                    .getNode(AuthManagerNode.NAME);
            return managerNode.getAuthService(name);
        } catch (Throwable e) {
            if (e instanceof AuthException)
                throw (AuthException)e;
            else
                throw new AuthException("Authentication service resolving error", e);
        }       
    }
}

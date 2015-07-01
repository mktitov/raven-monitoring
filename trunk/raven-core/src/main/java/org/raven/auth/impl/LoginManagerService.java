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

import org.raven.auth.LoginException;
import org.raven.auth.LoginManager;
import org.raven.auth.LoginService;
import org.raven.tree.Tree;
import org.raven.tree.impl.SystemNode;

/**
 *
 * @author Mikhail Titov
 */
public class LoginManagerService implements LoginManager {
    private final Tree tree;

    public LoginManagerService(Tree tree) {
        this.tree = tree;
    }

    public LoginService getLoginService(String name) throws LoginException {
        try {
            LoginManagerNode managerNode = (LoginManagerNode) 
                    tree.getRootNode()
                    .getNode(SystemNode.NAME)
                    .getNode(AuthorizationNode.NODE_NAME)
                    .getNode(LoginManagerNode.NAME);
            return managerNode.getLoginService(name);
        } catch (Throwable e) {
            if (e instanceof LoginException)
                throw (LoginException)e;
            else
                throw new LoginException("Login service resolving error", e);
        }       
    }
}

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
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class LoginManagerNode extends BaseNode implements LoginManager {
    public final static String NAME = "Login services";

    public LoginManagerNode() {
        super(NAME);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        createSystemLoginService();
    }
    
    private void createSystemLoginService() {
        if (!hasNode(SystemLoginService.NAME)) {
            SystemLoginService systemLoginService = new SystemLoginService();
            addAndSaveChildren(systemLoginService);
            systemLoginService.start();
        }
    }

    public LoginService getLoginService(String name) throws LoginException {
        Node authServiceNode = getNode(name);
        if (authServiceNode==null)
            throw new LoginException(String.format("Auth service (%s) not found", name));
        if (!authServiceNode.isStarted())
            throw new LoginException(String.format("Auth service (%s) unavailable", name));
        return (LoginService) authServiceNode;
    }
}

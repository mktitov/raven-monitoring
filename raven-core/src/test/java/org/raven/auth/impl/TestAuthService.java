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

import org.raven.auth.LoginService;
import org.raven.auth.UserContext;
import org.raven.net.ResponseContext;
import org.raven.net.http.server.SessionManager;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class TestAuthService extends BaseNode implements LoginService {

    public TestAuthService(String name) {
        super(name);
    }

    public UserContext login(String username, String password, String host, ResponseContext responseContext) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isLoginAllowedFromIp(String ip) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SessionManager getSessionManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

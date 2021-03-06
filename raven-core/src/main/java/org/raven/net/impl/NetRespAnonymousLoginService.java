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

import org.raven.auth.AnonymousLoginService;
import org.raven.auth.LoginException;
import org.raven.auth.UserContext;
import org.raven.auth.impl.AnonymousLoginServiceNode;
import org.raven.net.ResponseContext;
import org.raven.net.http.server.SessionManager;

/**
 *
 * @author Mikhail Titov
 */
public class NetRespAnonymousLoginService implements AnonymousLoginService {

    public UserContext login(String username, String password, String ip, ResponseContext responseContext) throws LoginException {
        return AnonymousLoginServiceNode.ANONYMOUS_USER;
    }

    public boolean isLoginAllowedFromIp(String ip) {
        return true;
    }

    public int getId() {
        return 0;
    }

    public String getName() {
        return "Anonymous login service";
    }

    @Override
    public boolean isStarted() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SessionManager getSessionManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

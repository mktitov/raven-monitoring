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

import org.raven.auth.AuthenticationFailedException;
import org.raven.auth.LoginException;
import org.raven.auth.LoginService;
import org.raven.auth.UserContext;
import org.raven.net.Authentication;
import org.raven.net.NetworkResponseContext;

/**
 *
 * @author Mikhail Titov
 */
public class NetRespContextLoginServiceWrapper implements LoginService {
    private final NetworkResponseContext respContext;

    public NetRespContextLoginServiceWrapper(NetworkResponseContext respContext) {
        this.respContext = respContext;
    }

    public UserContext login(String username, String password, String ip) throws LoginException {
        Authentication auth = respContext.getAuthentication();
        if (auth==null || (auth.getUser().equals(username) && auth.getPassword().equals(password)))
            return new NetRespContextUserContext(respContext, username, ip);
        throw new AuthenticationFailedException(username, respContext.getPath());
    }

    public boolean isLoginAllowedFromIp(String ip) {
        return true;
    }
}

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

import org.raven.auth.AnonymousLoginService;
import org.raven.auth.LoginException;
import org.raven.auth.UserContext;

/**
 *
 * @author Mikhail Titov
 */
//@NodeClass(parentNode = LoginManagerNode.class)
public class AnonymousLoginServiceNode extends LoginServiceNode implements AnonymousLoginService {
    public static final String NAME = "Anonymous";
    public final static UserContext ANONYMOUS_USER = new AnonymousLoginServiceUserContext();

    public AnonymousLoginServiceNode() {
        super(NAME);
    }
    
    @Override
    protected boolean createAuthenticatorsNode() {
        return false;
    }

    @Override
    public UserContext login(String login, String password, String ip) throws LoginException {
//        throw new AuthenticationFailedException(login, NAME);
        return ANONYMOUS_USER;
    }
}

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

import org.raven.auth.Authenticator;
import org.raven.auth.AuthenticatorException;
import org.raven.conf.Configurator;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class RootUserAuthenticator extends BaseNode implements Authenticator {
    public final static String ROOT_USER_NAME = "root";
    public final static String NAME = "Root user authenticator";

    public RootUserAuthenticator() {
        super(NAME);
    }

    public boolean checkAuth(String login, String password) throws AuthenticatorException {
        try {
            return ROOT_USER_NAME.equals(login) 
                && password.equals(configurator.getConfig().getStringProperty(
                    Configurator.AUTH_ROOT_PASSWORD, null));
        } catch (Exception e) {
            throw new AuthenticatorException(String.format("Error in (%s) authenticator", getPath()), e);
        }
    }
    
}

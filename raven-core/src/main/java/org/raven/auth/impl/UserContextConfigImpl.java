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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.raven.auth.UserContextConfig;

/**
 *
 * @author Mikhail Titov
 */
public class UserContextConfigImpl implements UserContextConfig {
    private final String host;
    private String login;
    private String authenticator;
    private String name;
    private boolean admin;
    private final Set<String> groups = new HashSet<String>();
    private final Map<String, Object> params = new HashMap<String, Object>();

    public UserContextConfigImpl(String login, String host) {
        this.login = login;
        this.host = host;
    }

    public String getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        this.authenticator = authenticator;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getHost() {
        return host;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

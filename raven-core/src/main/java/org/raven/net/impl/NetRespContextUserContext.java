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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.raven.auth.UserContext;
import org.raven.auth.impl.AccessControl;
import org.raven.net.NetworkResponseContext;
import org.raven.tree.Node;
import org.raven.tree.Tree;

/**
 *
 * @author Mikhail Titov
 */
public class NetRespContextUserContext implements UserContext {
    private final NetworkResponseContext respContext;
    private final String login;
    private final String host;

    public NetRespContextUserContext(NetworkResponseContext respContext, String login, String host) {
        this.respContext = respContext;
        this.login = login;
        this.host = host;
    }

    public String getAuthenticator() {
        return respContext.getName();
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return login;
    }

    public boolean isAdmin() {
        return false;
    }

    public String getHost() {
        return host;
    }

    public Set<String> getGroups() {
        return Collections.EMPTY_SET;
    }

    public Map<String, Object> getParams() {
        return Collections.EMPTY_MAP;
    }

    public int getAccessForNode(Node node) {
        return node==respContext? AccessControl.READ : AccessControl.NONE;
    }

    public boolean hasAccessToNode(Node node, String rights) {
        final int decodedRights = AccessControl.decodeRight(rights);
        return (getAccessForNode(node) & decodedRights) == decodedRights;
    }

    public Map<String, String> getResourcesList(Tree tree) {
        return Collections.EMPTY_MAP;
    }

    public void needRelogin() {
    }

    public boolean isNeedRelogin() {
        return false;
    }
}

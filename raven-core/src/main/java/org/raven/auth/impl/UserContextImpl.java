/*
 *  Copyright 2010 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.auth.impl;

import org.raven.auth.AccessPolicyManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.raven.auth.UserContext;
import org.raven.auth.UserContextConfig;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.Tree;

/**
 *
 * @author Mikhail Titov
 */
public class UserContextImpl implements UserContext
{
    private final String login;
    private final String authenticator;
    private final String name;
    private final String host;
    private final boolean admin;
    private final Map<String, Object> params;
    private final Set<String> groups;
    private final AccessPolicyManager policyManager;
    private volatile boolean needRelogin = false;

    public UserContextImpl(UserContextConfig config, AccessPolicyManager policyManager) {
        this.login = config.getLogin();
        this.authenticator = config.getAuthenticator();
        this.groups = Collections.unmodifiableSet(config.getGroups());
        this.params = new ConcurrentHashMap<String, Object>(config.getParams());
        this.admin = config.isAdmin();
        this.name = config.getName();
        this.host = config.getHost();
        this.policyManager = policyManager;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String getAuthenticator() {
        return authenticator;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public int getAccessForNode(Node node) {
        return admin? AccessControl.ALL_RIGHTS : policyManager.getAccessPoliciesForUser(this).getAccessForNode(node);
    }

    public boolean hasAccessToNode(Node node, String rights) {
        final int decodedRights = AccessControl.decodeRight(rights);
        return (getAccessForNode(node) & decodedRights) == decodedRights;
    }

    public boolean isNeedRelogin() {
        return needRelogin;
    }

    public void needRelogin() {
        this.needRelogin = true;
    }

    @Override
    public String toString() {
        return "login: "+login+"; host: "+host+"; admin: "+admin+"; authenticator: "+authenticator;
    }

    public HashMap<String, String> getResourcesList(Tree tree) {
        HashMap<String, String> rl = new HashMap<String, String>();
        for (AccessResource ar : policyManager.getAccessResourcesForUser(this).values()) {
            String title = ar.getTitle();
            if (title == null || title.length() == 0) {
                continue;
            }
            String path = null;
            if (ar.isPresent()) {
                path = ar.getShow();
                try {
                    if (getAccessForNode(tree.getNode(path)) < AccessControl.READ) {
                        continue;
                    }
                } catch (InvalidPathException ex) {
                    continue;
                }
            }
            rl.put(title, path);
        }
        return rl;
    }

}

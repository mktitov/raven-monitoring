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

package org.raven.auth;

import java.util.Map;
import java.util.Set;
import org.raven.auth.impl.AccessControl;
import org.raven.tree.Node;
import org.raven.tree.Tree;

/**
 *
 * @author Mikhail Titov
 */
public interface UserContext {
    /**
     * Return raven server session id for this user context
     */
//    public String getSessionId();
    /**
     * Return the name of the authenticator that authenticate the user
     */
    public String getAuthenticator();
    /**
     * Returns the user login.
     */
    public String getLogin();
    /**
     * Returns the user name
     */
    public String getName();
    /**
     * Returns true if this user is administrator
     */
    public boolean isAdmin();
    /**
     * Returns the ip address or host name from which user interact with system
     */
    public String getHost();
    /**
     * Returns the user groups.
     */
    public Set<String> getGroups();
    /**
     * Returns the user context parameters.
     */
    public Map<String, Object> getParams();
    /**
     * Returns access flags for given node.
     */
    public int getAccessForNode(Node node);
    /**
     * Returns <b>true</b> if user has rights passed in the parameter <b>rights</b>
     * @param node the node which access checked
     * @param rights the rights string (see {@link AccessControl})
     */
    public boolean hasAccessToNode(Node node, String rights);
    /**
     * Next request to sri will reset the session and reauthenticate the user (if need) after this function 
     * call
     */
    public void needRelogin();
    /**
     * Return the current status of the need relogin flag
     * @see UserContext#setNeedRelogin(boolean) 
     */
    public boolean isNeedRelogin();
    
    public Map<String,String>  getResourcesList(Tree tree);
}

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

import java.util.List;
import java.util.Map;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface UserContext
{
    /**
     * Return the name of the provider that authenticate the user
     */
    public String getAuthProvider();
    /**
     * Returns the user login.
     */
    public String getUsername();
    /**
     * Returns true if this user is administrator
     */
    public boolean isAdmin();
    /**
     * Sets administrator marker to this user.
     */
    public void setAdmin(boolean admin);
    /**
     * Returns the user groups.
     */
    public List<String> getGroups();
    /**
     * Returns the user context parameters.
     */
    public Map<String, Object> getParams();
    /**
     * Returns access flags for given node.
     */
    public int getAccessForNode(Node node);
    /**
     * Returns the user distinguished name.
     */
    public String getDN();
    /**
     * Sets the user distinguished name.
     */
    public void setDN(String dn);
    /**
     * Returns the user context attributes.
     */
    public Map<String, List<Object>> getAttrs();
}

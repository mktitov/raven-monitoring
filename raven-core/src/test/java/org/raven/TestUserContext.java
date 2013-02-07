/*
 *  Copyright 2011 Mikhail Titov.
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

package org.raven;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.raven.auth.UserContext;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class TestUserContext implements UserContext
{
    private Map<String, Object> params = new HashMap<String, Object>();

    public String getAuthenticator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getLogin() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isAdmin() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Set<String> getGroups() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public int getAccessForNode(Node node) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getHost() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

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

import org.raven.auth.AuthException;
import org.raven.auth.AuthService;
import org.raven.auth.UserContext;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class AuthServiceNode extends BaseNode implements AuthService {

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        initChildren();
    }
    
    private void initChildren() {
        if (!hasNode(AuthenticatorsNode.NAME))
            addAndStart(new AuthenticatorsNode());
        if (!hasNode(UserContextConfiguratorsNode.NAME))
            addAndStart(new UserContextConfiguratorsNode());
		if (!hasNode(ResourcesListNode.NODE_NAME))
			addAndStart(new ResourcesListNode());
		if (!hasNode(GroupsListNode.NODE_NAME))
			addAndStart(new GroupsListNode());
    }
    
    private void addAndStart(Node node) {
        addAndSaveChildren(node);
        node.start();
    }
    
    public UserContext authenticate(String username, String password) throws AuthException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}

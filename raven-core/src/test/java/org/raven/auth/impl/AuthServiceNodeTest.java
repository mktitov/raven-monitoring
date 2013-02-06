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

import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class AuthServiceNodeTest extends RavenCoreTestCase {
    
    @Test
    public void structureTest() {
        AuthServiceNode auth = new AuthServiceNode();
        auth.setName("service1");
        tree.getRootNode().addAndSaveChildren(auth);
        assertTrue(auth.start());
        checkNode(auth, AuthenticatorsNode.NAME);
        checkNode(auth, UserContextConfiguratorsNode.NAME);
        checkNode(auth, ResourcesListNode.NODE_NAME);
        checkNode(auth, GroupsListNode.NODE_NAME);
    }
    
    private void checkNode(Node owner, String child) {
        Node node = owner.getNode(child);
        assertNotNull(node);
        assertTrue(node.isStarted());
    }
}

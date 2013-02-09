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

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class AccessGroupNodeTest extends RavenCoreTestCase {
    private AccessGroupNode groupNode;
    
    @Before
    public void prepare() {
        groupNode = new AccessGroupNode();
        groupNode.setName("group node");
        testsNode.addAndSaveChildren(groupNode);
        groupNode.setLdapGroup("group");
        assertTrue(groupNode.start());
    }
    
    @Test
    public void allowAllUsersTest() {
        AccessGroup group = groupNode.getAccessGroup();
        assertNotNull(group);
        assertTrue(group.allowedUser("test"));
    }
    
    @Test
    public void allowOneUserTest() {
        addUser("test");
        AccessGroup group = groupNode.getAccessGroup();
        assertNotNull(group);
        assertTrue(group.allowedUser("test"));
        assertFalse(group.allowedUser("test1"));
    }
    
    @Test
    public void getAccessForNodeTest() {
        ResourceNode resNode = createResource();
        addResourceLink(resNode);
        AccessGroup group = groupNode.getAccessGroup();
        assertNotNull(group);
        assertEquals(AccessControl.NONE, group.getAccessForNode(resNode));
        assertEquals(AccessControl.READ, group.getAccessForNode(testsNode));
    }
    
    private ResourceNode createResource() {
        ResourceNode res = new ResourceNode();
        res.setName("res1");
        testsNode.addAndSaveChildren(res);
        assertTrue(res.start());
        addAccessControl(res, "tests node read", testsNode, NodePathModifier.NODE_ONLY, AccessRight.READ);
        return res;
    }
    
    private void addAccessControl(Node owner, String name, Node node, NodePathModifier modifier, AccessRight right) {
        AccessControlNode control = new AccessControlNode();
        control.setName(name);
        owner.addAndSaveChildren(control);
        control.setNode(node);
        control.setModifier(modifier);
        control.setRight(right);
        assertTrue(control.start());
    }
    
    private void addUser(String name) {
        AccessUserNode user = new AccessUserNode();
        user.setName(name);
        groupNode.addAndSaveChildren(user);
        assertTrue(user.start());
    }
    
    private void addResourceLink(ResourceNode resource) {
        ResourceLinkNode resLink = new ResourceLinkNode();
        resLink.setName("link to res1");
        groupNode.addAndSaveChildren(resLink);
        resLink.setNode(resource);
        assertTrue(resLink.start());
    }
    
}

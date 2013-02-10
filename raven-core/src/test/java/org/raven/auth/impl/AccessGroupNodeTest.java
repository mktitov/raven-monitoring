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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import static org.easymock.EasyMock.*;
import org.raven.auth.UserContext;

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
    
    @Test
    public void addPolicyIfNeed_InvalidGroupTest() {
        ResourceNode resNode = createResource();
        addResourceLink(resNode);
        UserContext context = createMock(UserContext.class);
        expect(context.getGroups()).andReturn(Collections.EMPTY_SET);
        replay(context);
        
        AccessGroup policy = new AccessGroup();
        groupNode.addPoliciesIfNeed(context, policy);
        assertEquals(AccessControl.NONE, policy.getAccessForNode(testsNode));
        verify(context);
    }
    
    @Test
    public void addPolicyIfNeed_AllUsersTest() {
        ResourceNode resNode = createResource();
        addResourceLink(resNode);
        UserContext context = createMock(UserContext.class);
        HashSet<String> groupNames = new HashSet<String>(Arrays.asList("group"));
        expect(context.getGroups()).andReturn(groupNames);
        expect(context.getLogin()).andReturn("test");
        replay(context);
        
        AccessGroup policy = new AccessGroup();
        groupNode.addPoliciesIfNeed(context, policy);
        assertEquals(AccessControl.READ, policy.getAccessForNode(testsNode));
        verify(context);
    }
    
    @Test
    public void addPolicyIfNeed_UserNotInListTest() {
        ResourceNode resNode = createResource();
        addResourceLink(resNode);
        addUser("test1");
        UserContext context = createMock(UserContext.class);
        HashSet<String> groupNames = new HashSet<String>(Arrays.asList("group"));
        expect(context.getGroups()).andReturn(groupNames);
        expect(context.getLogin()).andReturn("test");
        replay(context);
        
        AccessGroup policy = new AccessGroup();
        groupNode.addPoliciesIfNeed(context, policy);
        assertEquals(AccessControl.NONE, policy.getAccessForNode(testsNode));
        verify(context);
    }
    
    @Test
    public void addPolicyIfNeed_UserInListTest() {
        ResourceNode resNode = createResource();
        addResourceLink(resNode);
        addUser("test");
        UserContext context = createMock(UserContext.class);
        HashSet<String> groupNames = new HashSet<String>(Arrays.asList("group"));
        expect(context.getGroups()).andReturn(groupNames);
        expect(context.getLogin()).andReturn("test");
        replay(context);
        
        AccessGroup policy = new AccessGroup();
        groupNode.addPoliciesIfNeed(context, policy);
        assertEquals(AccessControl.READ, policy.getAccessForNode(testsNode));
        verify(context);
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

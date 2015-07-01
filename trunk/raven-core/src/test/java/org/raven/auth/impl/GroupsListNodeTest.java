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
import java.util.HashSet;
import org.junit.Before;
import static org.junit.Assert.*;
import org.raven.auth.UserContext;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import static org.easymock.EasyMock.*;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class GroupsListNodeTest extends RavenCoreTestCase {
    private GroupsListNode groupsNode;
    private BaseNode node1, node2, node3;
    private GroupsContainerNode container;
    private ResourceNode res1, res2, res3;
    private UserContext user;
    
    @Before
    public void prepare() {
        node1 = new BaseNode("node1");
        testsNode.addAndSaveChildren(node1);
        assertTrue(node1.start());
        
        node2 = new BaseNode("node2");
        testsNode.addAndSaveChildren(node2);
        assertTrue(node2.start());
        
        node3 = new BaseNode("node3");
        testsNode.addAndSaveChildren(node3);
        assertTrue(node3.start());
        
        res1 = addResourceNode("res1", testsNode);
        addAccessControl(res1, "read node1", node1, NodePathModifier.NODE_ONLY, AccessRight.READ);
        res2 = addResourceNode("res2", testsNode);
        addAccessControl(res2, "read node2", node2, NodePathModifier.NODE_ONLY, AccessRight.READ);
        res3 = addResourceNode("res3", testsNode);
        addAccessControl(res3, "read node3", node3, NodePathModifier.NODE_ONLY, AccessRight.READ);
        
        groupsNode = new GroupsListNode();
        testsNode.addAndSaveChildren(groupsNode);
        assertTrue(groupsNode.start());
        
        container = new GroupsContainerNode();
        container.setName("container");
        groupsNode.addAndSaveChildren(container);
        assertTrue(container.start());
    }
    
    @Test
    public void test() {
        addAccessGroup(container, "grp1", "group1", res1);
        addAccessGroup(groupsNode, "grp2", "group1", res2);
        addAccessGroup(groupsNode, "grp3", "group3", res3);
        UserContext user = createMock(UserContext.class);
        expect(user.isAdmin()).andReturn(false).atLeastOnce();
        HashSet<String> groupNames = new HashSet<String>(Arrays.asList("group1"));
        expect(user.getGroups()).andReturn(groupNames).atLeastOnce();
        expect(user.getLogin()).andReturn("test").atLeastOnce();
        replay(user);
        AccessControlList policy = groupsNode.getAccessPoliciesForUser(user);
        assertEquals(AccessControl.READ, policy.getAccessForNode(node1));
        assertEquals(AccessControl.READ, policy.getAccessForNode(node2));
        assertEquals(AccessControl.NONE, policy.getAccessForNode(node3));
        verify(user);
    }
    
    private AccessGroupNode addAccessGroup(Node owner, String name, String group, ResourceNode res) {
        AccessGroupNode groupNode = new AccessGroupNode();
        groupNode.setName(name);
        owner.addAndSaveChildren(groupNode);
        groupNode.setLdapGroup(group);
        assertTrue(groupNode.start());
        ResourceLinkNode resLink = new ResourceLinkNode();
        resLink.setName("res");
        groupNode.addAndSaveChildren(resLink);
        resLink.setNode(res);
        assertTrue(resLink.start());
        return groupNode;
    }
    
    private ResourceNode addResourceNode(String name, Node owner) {
        ResourceNode res = new ResourceNode();
        res.setName(name);
        owner.addAndSaveChildren(res);
        res.setDsc(name+" dsc");
        res.setShow(owner);
        assertTrue(res.start());
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
    
    private void addResourceLink(AccessGroupNode groupNode, ResourceNode resource) {
        ResourceLinkNode resLink = new ResourceLinkNode();
        resLink.setName("link to res1");
        groupNode.addAndSaveChildren(resLink);
        resLink.setNode(resource);
        assertTrue(resLink.start());
    }
}

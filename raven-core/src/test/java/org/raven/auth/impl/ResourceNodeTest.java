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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ContainerNode;

/**
 *
 * @author Mikhail Titov
 */
public class ResourceNodeTest extends RavenCoreTestCase {
    private ResourceNode resNode;
    private ContainerNode parent;
    private BaseNode child;
    
    @Before
    public void prepare() {
        parent = new ContainerNode("parent");
        testsNode.addAndSaveChildren(parent);
        assertTrue(parent.start());
        child = new BaseNode("child");
        parent.addAndSaveChildren(child);
        assertTrue(child.start());
        
        resNode = new ResourceNode();
        resNode.setName("resNode");
        testsNode.addAndSaveChildren(resNode);
        resNode.setDsc("dsc");
        resNode.setShow(resNode);
    }
    
    @Test
    public void noneAccessTest() {
        assertTrue(resNode.start());
        AccessResource res = resNode.getAccessResource();
        assertNotNull(res);
        assertEquals(AccessControl.NONE, res.getAccessForNode(parent));
        assertEquals(AccessControl.NONE, res.getAccessForNode(resNode));
    }
    
    @Test
    public void noneAccessTest2() {
        addAccessControl("control", parent, NodePathModifier.NODE_ONLY, AccessRight.NONE);
        assertTrue(resNode.start());
        AccessResource res = resNode.getAccessResource();
        assertNotNull(res);
        assertEquals(AccessControl.NONE, res.getAccessForNode(parent));
        assertEquals(AccessControl.NONE, res.getAccessForNode(resNode));
    }
    
    @Test
    public void readAccessForNodeOnly() {
        addAccessControl("control", parent, NodePathModifier.NODE_ONLY, AccessRight.READ);
        assertTrue(resNode.start());
        AccessResource res = resNode.getAccessResource();
        assertNotNull(res);
        assertEquals(AccessControl.READ, res.getAccessForNode(parent));
        assertEquals(AccessControl.NONE, res.getAccessForNode(child));
        assertEquals(AccessControl.NONE, res.getAccessForNode(resNode));
    }
    
    @Test
    public void readAccessForChildrenOnly() {
        addAccessControl("control", parent, NodePathModifier.CHILDREN_ONLY, AccessRight.READ);
        assertTrue(resNode.start());
        AccessResource res = resNode.getAccessResource();
        assertNotNull(res);
        assertEquals(AccessControl.TRANSIT, res.getAccessForNode(parent));
        assertEquals(AccessControl.READ, res.getAccessForNode(child));
        assertEquals(AccessControl.NONE, res.getAccessForNode(resNode));
    }
    
    @Test
    public void readAccessForNodeAndChildren() {
        addAccessControl("control", parent, NodePathModifier.NODE_and_CHILDREN, AccessRight.READ);
        assertTrue(resNode.start());
        AccessResource res = resNode.getAccessResource();
        assertNotNull(res);
        assertEquals(AccessControl.READ, res.getAccessForNode(parent));
        assertEquals(AccessControl.READ, res.getAccessForNode(child));
        assertEquals(AccessControl.NONE, res.getAccessForNode(resNode));
    }
    
    @Test
    public void accessPriorityTest() {
        addAccessControl("control", parent, NodePathModifier.NODE_and_CHILDREN, AccessRight.WRITE);
        addAccessControl("control2", child, NodePathModifier.NODE_ONLY, AccessRight.READ);
        assertTrue(resNode.start());
        AccessResource res = resNode.getAccessResource();
        assertNotNull(res);
        assertEquals(AccessControl.WRITE+AccessControl.READ, res.getAccessForNode(parent));
        assertEquals(AccessControl.READ, res.getAccessForNode(child));
        assertEquals(AccessControl.NONE, res.getAccessForNode(resNode));
    }
    
    private void addAccessControl(String name, Node node, NodePathModifier modifier, AccessRight right) {
        AccessControlNode control = new AccessControlNode();
        control.setName(name);
        resNode.addAndSaveChildren(control);
        control.setNode(node);
        control.setModifier(modifier);
        control.setRight(right);
        assertTrue(control.start());
    }
}

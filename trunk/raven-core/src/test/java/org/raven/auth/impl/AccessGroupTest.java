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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class AccessGroupTest extends RavenCoreTestCase {
    private Node node;
    
    @Before
    public void prepare() {
        node = new BaseNode("test node");
        testsNode.addAndSaveChildren(node);
        assertTrue(node.start());
    }
    
    @Test
    public void allowAllUsersTest() {
        AccessGroup grp = new AccessGroup("grp name", "grp", null, null);
        assertTrue(grp.allowedUser("test"));
        assertEquals(AccessControl.NONE, grp.getAccessForNode(node));
    }
    
    @Test
    public void allowAllUsersTest2() {
        AccessGroup grp = new AccessGroup("grp name", "grp", createResources(), null);
        assertTrue(grp.allowedUser("test"));
        assertEquals(AccessControl.READ, grp.getAccessForNode(node));
    }
    
    @Test
    public void allowToOneUserTest2() {
        AccessGroup grp = new AccessGroup("grp name", "grp", createResources(), Arrays.asList("test"));
        assertTrue(grp.allowedUser("test"));
        assertFalse(grp.allowedUser("test1"));
        assertEquals(AccessControl.READ, grp.getAccessForNode(node));
    }
    
    private List<AccessResource> createResources() {
        AccessControl control = new AccessControl(node.getPath(), AccessRight.READ.getRights());
        AccessResource res = new AccessResource("res1", "res title", null, Arrays.asList(control));
        List<AccessResource> resources = Arrays.asList(res);
        return resources;
    }
}

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

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail TItov
 */
public class ResourcesListNodeTest extends RavenCoreTestCase {
    private ResourcesListNode resourcesNode;
    
    @Before
    public void prepare() {
        resourcesNode = new ResourcesListNode();
        testsNode.addAndSaveChildren(resourcesNode);
        assertTrue(resourcesNode.start());
    }
    
    @Test
    public void test() {
        ResourcesContainerNode container = new ResourcesContainerNode();
        container.setName("group");
        resourcesNode.addAndSaveChildren(container);
        assertTrue(container.start());
        
        addResourceNode("res1", container);
        addResourceNode("res2", resourcesNode);
        List<AccessResource> resources = resourcesNode.getResources();
        assertNotNull(resources);
        assertEquals(2, resources.size());
        assertEquals("res1", resources.get(0).getName());
        assertEquals("res2", resources.get(1).getName());
    }
    
    private void addResourceNode(String name, Node owner) {
        ResourceNode res = new ResourceNode();
        res.setName(name);
        owner.addAndSaveChildren(res);
        res.setDsc(name+" dsc");
        res.setShow(owner);
        assertTrue(res.start());
    }
}

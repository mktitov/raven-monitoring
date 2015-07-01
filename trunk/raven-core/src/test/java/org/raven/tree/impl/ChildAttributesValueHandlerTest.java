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
package org.raven.tree.impl;

import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class ChildAttributesValueHandlerTest extends RavenCoreTestCase {

    @Test
    public void test() throws Exception {
        BaseNode node = new BaseNode("test");
        testsNode.addAndSaveChildren(node);
        assertTrue(node.start());
        
        addAttr(node, "attr1", "attr1 value", false, null);
        addAttr(node, "group", null, true, null);
        assertEquals("attr1 value", node.getAttr("attr1").getValue());
        assertNull(node.getAttr("group").getValue());
        
        addAttr(node, "child1", "child1 value", false, "group");
        assertEquals("child1=child1 value", node.getAttr("group").getValue());
        addAttr(node, "child2", "child2 value", false, "group");
        assertEquals("child1=child1 value; child2=child2 value", node.getAttr("group").getValue());
    }
    
    private NodeAttribute addAttr(Node node, String name, String value, boolean isGroup, String parent) throws Exception {
        NodeAttributeImpl attr = new NodeAttributeImpl(name, String.class, value, null);
        attr.setOwner(node);
        attr.setParentAttribute(parent);
        if (isGroup) 
            attr.setValueHandlerType(ChildAttributesValueHandlerFactory.TYPE);
        attr.init();
        node.addAttr(attr);
        return attr;
    }
}
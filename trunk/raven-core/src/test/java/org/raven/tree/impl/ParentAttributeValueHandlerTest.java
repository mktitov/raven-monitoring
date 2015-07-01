/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven.tree.impl;

import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class ParentAttributeValueHandlerTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        Node n1 = new ContainerNode("n1");
        tree.getRootNode().addAndSaveChildren(n1);
        assertTrue(n1.start());
        addAttr(n1, "test");

        Node n2 = new ContainerNode("n2");
        n1.addAndSaveChildren(n2);
        assertTrue(n2.start());

        Node n3 = new ContainerNode("n3");
        n2.addAndSaveChildren(n3);
        assertTrue(n3.start());
        addAttr(n3, null);

        assertEquals("test", n3.getNodeAttribute("attr").getValue());

        addAttr(n2, "test2");
        assertEquals("test2", n3.getNodeAttribute("attr").getValue());
    }

    private void addAttr(Node owner, Object val) throws Exception
    {
        NodeAttributeImpl attr = new NodeAttributeImpl("attr", String.class, val, null);
        attr.setOwner(owner);
        attr.init();
        owner.addNodeAttribute(attr);
    }
}

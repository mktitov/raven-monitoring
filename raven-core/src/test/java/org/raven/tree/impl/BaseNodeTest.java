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

import org.junit.Assert;
import org.junit.Test;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class BaseNodeTest extends Assert
{
    @Test
    public void cloneTest() throws CloneNotSupportedException
    {
        BaseNode parent = new BaseNode();
        parent.setName("parent");
        
//        Class[] childTypes = new Class[]{BaseNode.class};
        BaseNode node = new BaseNode();
        node.setName("node");
        node.setId(1);
        node.setParent(parent);
        
        NodeAttribute attr = new NodeAttributeImpl();
        attr.setName("attr");
        attr.setType(String.class);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        
//        BaseNode child = new BaseNode();
//        child.setName("child");
//        node.addChildren(child);
//        child.setId(2);
        
        //
        Node nodeClone = (Node) node.clone();
        //
        
        assertNotNull(nodeClone);
        assertNotSame(node, nodeClone);
        assertEquals(0, nodeClone.getId());
        assertNull(nodeClone.getParent());
        assertEquals("node", nodeClone.getName());
        
        NodeAttribute attrClone = nodeClone.getNodeAttribute("attr");
        assertNotNull(attrClone);
        assertNotSame(attr, attrClone);
        assertEquals(0, attrClone.getId());
        assertEquals(String.class, attrClone.getType());
        
//        Node childClone = nodeClone.getChildren("child");
//        assertNotNull(childClone);
//        assertNotSame(child, childClone);
//        assertSame(nodeClone, childClone.getParent());
    }
}

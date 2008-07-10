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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import org.junit.Assert;
import org.junit.Test;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import static org.easymock.EasyMock.*;

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
        
        BaseNode node = new BaseNode();
        node.setName("node");
        node.setId(1);
        node.setParent(parent);
        
        NodeAttribute attr = new NodeAttributeImpl();
        attr.setName("attr");
        attr.setType(String.class);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        
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
    }
    
    @Test
    public void getEffectiveChildrens()
    {
        BaseNode node = new BaseNode();
        assertNull(node.getEffectiveChildrens());
        
        Node child1 = createMock("child1", Node.class);
        Node child3 = createMock("child3", Node.class);
        Node condChild = createMock("condChild", Node.class);
        
        child1.setParent(node);
        child1.addListener(node);
        expect(child1.getName()).andReturn("child1");
        expect(child1.compareTo(child3)).andReturn(-1);
        expect(child1.compareTo(condChild)).andReturn(-1);
        expect(child1.getIndex()).andReturn(1);
        expect(child1.isConditionalNode()).andReturn(false);
        
        Node condChildChild = createMock("condChildChild", Node.class);
        
        condChild.setParent(node);
        condChild.addListener(node);
        expect(condChild.getName()).andReturn("condChild");
        expect(condChild.isConditionalNode()).andReturn(true);
        expect(condChild.getIndex()).andReturn(2);
        expect(condChild.getEffectiveChildrens()).andReturn(Arrays.asList(condChildChild));
        
        child3.setParent(node);
        child3.addListener(node);
        expect(child3.getName()).andReturn("child3");
        expect(child3.getIndex()).andReturn(3);
        expect(child3.compareTo(condChild)).andReturn(1);
        expect(child3.isConditionalNode()).andReturn(false);
        
        replay(child1, condChild, condChildChild, child3);
        
        node.addChildren(child3);
        node.addChildren(child1);
        node.addChildren(condChild);
        
        Collection childs = node.getEffectiveChildrens();
        assertNotNull(childs);
        assertEquals(3, childs.size());
        
        Iterator<Node> it = childs.iterator();
        assertSame(child1, it.next());
        assertSame(condChildChild, it.next());
        assertSame(child3, it.next());
        
        verify(child1, condChild, condChildChild, child3);
    }
}

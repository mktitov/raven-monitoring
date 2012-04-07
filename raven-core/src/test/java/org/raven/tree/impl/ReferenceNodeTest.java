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

import java.util.Collection;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.net.impl.TestViewable;
import org.raven.template.impl.TemplateEntry;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class ReferenceNodeTest extends RavenCoreTestCase {
    private ReferenceNode ref;
    
    @Before
    public void prepare() {
        ref = new ReferenceNode();
        ref.setName("reference");
        tree.getRootNode().addAndSaveChildren(ref); 
    }
    
    @Test
    public void testNotInTemplate() throws Exception
    {
        ContainerNode nodes = new ContainerNode("nodes");
        tree.getRootNode().addChildren(nodes);
        nodes.save();
        nodes.init();
        nodes.start();
        assertEquals(Status.STARTED, nodes.getStatus());

        ReferenceNode referenceNode = new ReferenceNode();
        referenceNode.setName("referenceToSystem");
        nodes.addChildren(referenceNode);
        referenceNode.save();
        referenceNode.init();

        Node systemNode = tree.getRootNode().getChildren(SystemNode.NAME);
        referenceNode.setReference(systemNode);
        referenceNode.start();
        assertEquals(Status.STARTED, referenceNode.getStatus());

        Collection<Node> depNodes = nodes.getEffectiveChildrens();
        assertNotNull(depNodes);
        assertEquals(1, depNodes.size());
        assertSame(systemNode, depNodes.iterator().next());

        referenceNode.setUseInTemplate(true);
        depNodes = nodes.getEffectiveChildrens();
        assertEquals(1, depNodes.size());
        assertSame(referenceNode, depNodes.iterator().next());
    }

    @Test
    public void testInTemplate() throws Exception
    {
        TemplateEntry nodes = new TemplateEntry();
        nodes.setName("template");
        tree.getRootNode().addChildren(nodes);
        nodes.save();
        nodes.init();
        nodes.start();
        assertEquals(Status.STARTED, nodes.getStatus());

        ReferenceNode referenceNode = new ReferenceNode();
        referenceNode.setName("referenceToSystem");
        nodes.addChildren(referenceNode);
        referenceNode.save();
        referenceNode.init();
        referenceNode.setUseInTemplate(true);

        Node systemNode = tree.getRootNode().getChildren(SystemNode.NAME);
        referenceNode.setReference(systemNode);

        Collection<Node> depNodes = nodes.getEffectiveChildrens();
        assertNotNull(depNodes);
        assertEquals(1, depNodes.size());
        assertSame(systemNode, depNodes.iterator().next());

        referenceNode.setUseInTemplate(false);
        depNodes = nodes.getEffectiveChildrens();
        assertEquals(1, depNodes.size());
        assertSame(referenceNode, depNodes.iterator().next());
    }
    
    @Test
    public void getSelfRefernceAttributesTest() throws Exception {
        assertTrue(ref.start());
        
        assertNull(ref.getRefreshAttributes());
        addAttrToRef();
        
        Map<String, NodeAttribute> attrs = ref.getRefreshAttributes();
        assertNotNull(attrs);
        assertTrue(attrs.containsKey("test"));
        assertEquals(1, attrs.size());
    }

    @Test
    public void getReferenceRefAtttributesTest() throws Exception {
        TestViewable node = new TestViewable();
        node.setName("viewable object");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());
        ref.setReference(node);
        assertTrue(ref.start());
        
        assertNull(ref.getRefreshAttributes());
        
        node.addRefreshAttribute(new NodeAttributeImpl("ref_test", String.class, null, null));
        Map<String, NodeAttribute> attrs = ref.getRefreshAttributes();
        assertNotNull(attrs);
        assertEquals(1, attrs.size());
        assertTrue(attrs.containsKey("ref_test"));
        
        addAttrToRef();
        attrs = ref.getRefreshAttributes();
        assertNotNull(attrs);
        assertEquals(2, attrs.size());
        assertTrue(attrs.containsKey("ref_test"));
        assertTrue(attrs.containsKey("test"));
    }
    
    @Test
    public void hideRefreshAttributesTest() throws Exception {
        TestViewable node = new TestViewable();
        node.setName("viewable object");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());
        ref.setReference(node);
        ref.setHideRefreshAttributes("ref_test");
        assertTrue(ref.start());
        
        assertNull(ref.getRefreshAttributes());
        
        node.addRefreshAttribute(new NodeAttributeImpl("ref_test", String.class, null, null));
        Map<String, NodeAttribute> attrs = ref.getRefreshAttributes();
        assertNull(attrs);
    }
    
    private void addAttrToRef() throws Exception {
        NodeAttributeImpl attr = new NodeAttributeImpl("test", String.class, null, null);
        attr.setValueHandlerType(RefreshAttributeValueHandlerFactory.TYPE);
        attr.init();
        ref.addNodeAttribute(attr);
    }
}
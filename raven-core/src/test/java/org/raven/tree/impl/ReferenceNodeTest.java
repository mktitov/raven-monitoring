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
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.template.impl.TemplateEntry;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;

/**
 *
 * @author Mikhail Titov
 */
public class ReferenceNodeTest extends RavenCoreTestCase
{
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
}
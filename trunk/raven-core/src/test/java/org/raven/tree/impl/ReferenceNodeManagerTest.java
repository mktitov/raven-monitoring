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

import java.util.List;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.objects.NodesDataSource;

/**
 *
 * @author Mikhail Titov
 */
public class ReferenceNodeManagerTest  extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        ContainerNode node1 = new ContainerNode("node1");
        tree.getRootNode().addChildren(node1);
        node1.save();
        node1.init();

        ContainerNode node2 = new ContainerNode("node2");
        tree.getRootNode().addChildren(node2);
        node2.save();
        node2.init();

        NodesDataSource ds = new NodesDataSource();
        ds.setName("ds");
        tree.getRootNode().addChildren(ds);
        ds.save();
        ds.init();
        ds.setNodes(node1, node2);
        ds.start();
        assertEquals(Status.STARTED, ds.getStatus());

        ReferenceNodeManager manager = new ReferenceNodeManager();
        manager.setName("manager");
        tree.getRootNode().addChildren(manager);
        manager.save();
        manager.init();
        manager.setDataSource(ds);
        manager.setTableColumn(2);
        manager.start();
        assertEquals(Status.STARTED, manager.getStatus());
        manager.refereshData(null);

        List<Node> refs = manager.getSortedChildrens();
        assertNotNull(refs);
        assertEquals(2, refs.size());
        assertSame(node1, ((ReferenceNode)refs.get(0)).getReference());
        assertSame(node2, ((ReferenceNode)refs.get(1)).getReference());

        Node ref1 = refs.get(0);
        ds.setNodes(node1);
        manager.refereshData(null);
        refs = manager.getSortedChildrens();
        assertNotNull(refs);
        assertEquals(1, refs.size());
        assertSame(node1, ((ReferenceNode)refs.get(0)).getReference());
        assertNotSame(ref1, refs.get(0));
    }
}
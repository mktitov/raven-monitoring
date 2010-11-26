/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.server.app.rest;

import java.util.Collection;
import org.junit.Test;
import org.raven.server.app.RavenServerAppTestCase;
import org.raven.server.app.bean.NodeBean;
import org.raven.tree.InvalidPathException;
import org.raven.tree.impl.SystemNode;

/**
 *
 * @author Mikhail Titov
 */
public class NodeResourceTest extends RavenServerAppTestCase
{
    @Test
    public void getChildNodeTest1() throws InvalidPathException
    {
        NodeResource res = new NodeResource();
        Collection<NodeBean> nodes = res.getChildNodes(null);

        assertNotNull(nodes);
        assertTrue(nodes.size()>=2);
    }

    @Test
    public void getChildNodeTest2() throws InvalidPathException
    {
        NodeResource res = new NodeResource();
        Collection<NodeBean> nodes = res.getChildNodes(
                tree.getRootNode().getChildren(SystemNode.NAME).getPath());

        assertNotNull(nodes);
        assertTrue(nodes.size()>2);
    }

}
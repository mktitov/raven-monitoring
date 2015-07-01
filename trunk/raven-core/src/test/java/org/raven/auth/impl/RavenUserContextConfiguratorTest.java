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

package org.raven.auth.impl;

import org.raven.auth.UserContextConfiguratorService;
import org.raven.test.RavenCoreTestCase;
import java.util.Arrays;
import java.util.Map;
import org.raven.auth.UserContextConfigurator;
import org.raven.tree.Node;
import org.raven.tree.impl.SystemNode;
import org.junit.Test;
import org.raven.auth.UserContext;
import org.raven.tree.Tree;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
@Deprecated
public class RavenUserContextConfiguratorTest extends RavenCoreTestCase
{
//    @Test
//    public void objectTest()
//    {
//        Tree treeService = createMock(Tree.class);
//        UserContextAndNode node = createStrictMock(UserContextAndNode.class);
//        UserContext context = createMock(UserContext.class);
//
//        expect(treeService.getRootNode()).andReturn(node);
//        expect(node.getChildren(SystemNode.NAME)).andReturn(node);
//        expect(node.getChildren(AuthorizationNode.NODE_NAME)).andReturn(node);
//        expect(node.getChildren(ContextsNode.NODE_NAME)).andReturn(node);
//        expect(node.getSortedChildrens()).andReturn(Arrays.asList((Node)node));
//        expect(node.getStatus()).andReturn(Node.Status.STARTED);
//        node.configure(context);
//
//        replay(treeService, node, context);
//
//        RavenUserContextConfigurator configuratorService = new RavenUserContextConfigurator(treeService);
//        configuratorService.configure(context);
//
//        verify(treeService, node, context);
//    }
//
//    @Test
//    public void serviceTest()
//    {
//        ContextsNode contexts = (ContextsNode) tree.getRootNode()
//                .getChildren(SystemNode.NAME)
//                .getChildren(AuthorizationNode.NODE_NAME)
//                .getChildren(ContextsNode.NODE_NAME);
//        UserContextConfiguratorNode configuratorNode = new UserContextConfiguratorNode();
//        configuratorNode.setName("configurator");
//        contexts.addAndSaveChildren(configuratorNode);
//        configuratorNode.setExpression("userContext.params.test='value'");
//        assertTrue(configuratorNode.start());
//
//        UserContextConfigurator configuratorService = registry.getService(
//                UserContextConfiguratorService.class);
//        assertNotNull(configuratorService);
//
//        UserContext context = createMock(UserContext.class);
//        Map params = createMock(Map.class);
//
//        expect(context.getParams()).andReturn(params);
//        expect(params.put("test", "value")).andReturn(null);
//
//        replay(context, params);
//
//        configuratorService.configure(context);
//
//        verify(context, params);
//    }
}
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

import java.util.Arrays;
import org.junit.Test;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.raven.tree.Node;
import org.raven.tree.NodeBuilder;
import org.raven.tree.NodeBuildersProvider;
import org.raven.tree.NodePathResolver;
import org.raven.tree.Tree;

/**
 *
 * @author Mikhail Titov
 */
public class TreeNodesBuilderImplTest extends Assert {

    @Test
    public void test() throws Exception {
        String basePath = "/tests/";
        IMocksControl mocks = createControl();
        Node baseNode = mocks.createMock("baseNode", Node.class);
        Node containerNode = mocks.createMock("containerNode", Node.class);
        NodeBuildersProvider provider = mocks.createMock(NodeBuildersProvider.class);
        NodeBuilder builder1 = mocks.createMock("builder1", NodeBuilder.class);
        NodeBuilder builder2 = mocks.createMock("builder2", NodeBuilder.class);
        Node node1 = mocks.createMock("node1", Node.class);
        Node node2 = mocks.createMock("node2", Node.class);
        Tree tree = mocks.createMock(Tree.class);
        NodePathResolver pathResolver = mocks.createMock(NodePathResolver.class);
        
        expect(provider.getBasePath()).andReturn(basePath);
        expect(tree.getNode(basePath)).andReturn(baseNode);
        expect(provider.getNodeBuilders()).andReturn(Arrays.asList(builder1, builder2));
        
        //sequence for builder1
        expect(builder1.getPath()).andReturn("container/node1");
        expect(baseNode.getNodeByPath("container/node1")).andReturn(null);       
        expect(builder1.getPath()).andReturn("container/node1");
        expect(pathResolver.splitToPathElements("container/node1")).andReturn(Arrays.asList("container", "node1"));
        expect(baseNode.getNode("container")).andReturn(null);
        expect(provider.createPathNode()).andReturn(containerNode);
        containerNode.setName("container");
        baseNode.addAndSaveChildren(containerNode);
        expect(builder1.build(containerNode)).andReturn(node1);
        node1.setName("node1");
        node1.save();
        
        //sequence for builder2
        expect(builder2.getPath()).andReturn("node2");
        expect(baseNode.getNodeByPath("node2")).andReturn(node2);       
        
        mocks.replay();
        
        TreeNodesBuilderImpl nodesBuilder = new TreeNodesBuilderImpl(Arrays.asList(provider), pathResolver);
        nodesBuilder.treeInitialized(tree);
        
        mocks.verify();
    }
}

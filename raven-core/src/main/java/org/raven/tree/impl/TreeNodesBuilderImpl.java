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

import java.util.Collection;
import java.util.List;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.NodeBuilder;
import org.raven.tree.NodeBuildersProvider;
import org.raven.tree.NodePathResolver;
import org.raven.tree.Tree;
import org.raven.tree.TreeListener;
import org.raven.tree.TreeNodesBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class TreeNodesBuilderImpl implements TreeNodesBuilder {
    private final Logger logger = LoggerFactory.getLogger(Tree.class);
    private final Collection<NodeBuildersProvider> providers;
    private final NodePathResolver pathResolver;

    public TreeNodesBuilderImpl(Collection<NodeBuildersProvider> providers, NodePathResolver pathResolver) {
        this.providers = providers;
        this.pathResolver = pathResolver;
    }

    public void treeInitialized(Tree tree) {
        for (NodeBuildersProvider provider: providers) {
            try {
                Node baseNode = tree.getNode(provider.getBasePath());
                for (NodeBuilder builder: provider.getNodeBuilders()) {
                    if (baseNode.getNodeByPath(builder.getPath())!=null)
                        continue;
                    processBuilder(builder, baseNode, provider);
                }
            } catch (InvalidPathException e) {
                if (logger.isErrorEnabled())
                    logger.error("Not found base node for node builders provider", e);
            }
        }
    }

    public void treeReloaded(Tree tree) {
    }

    private void processBuilder(NodeBuilder builder, Node baseNode, NodeBuildersProvider provider) {
        List<String> pathElems = pathResolver.splitToPathElements(builder.getPath());
        Node parent = baseNode;
        for (int i=0; i<pathElems.size()-1; ++i) {
            Node node = parent.getNode(pathElems.get(i));
            if (node==null) {
                node = provider.createPathNode();
                node.setName(pathElems.get(i));
                parent.addAndSaveChildren(node);
            }
            parent = node;
        }
        try {
            Node node = builder.build(parent);
            node.setName(pathElems.get(pathElems.size()-1));
            node.save();
        } catch (Exception e) {
            if (logger.isErrorEnabled())
                logger.error(String.format(
                    "Error creating node by node builders provider (%s) using node builder (%s)", 
                    provider.getBasePath(), builder.getPath()), e);
        }
    }
}

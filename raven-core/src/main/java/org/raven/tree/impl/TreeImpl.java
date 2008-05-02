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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.raven.conf.Configurator;
import org.raven.tree.Node;
import org.raven.tree.NodeNotFoundError;
import org.raven.tree.Tree;
import org.weda.internal.exception.NullParameterError;

/**
 *
 * @author Mikhail Titov
 */
public class TreeImpl implements Tree
{
    private final Configurator configurator;
    private BaseNode rootNode;

    public TreeImpl(Configurator configurator)
    {
        this.configurator = configurator;
        init();
    }

    public Node getRootNode()
    {
        return rootNode;
    }

    public Node getNode(String path) throws NodeNotFoundError
    {
        NullParameterError.check("path", path);
        
        String[] names = path.split(Node.NODE_SEPARATOR);
        
        Node node = rootNode;
        for (String name: names)
        {
            node = node.getChildren(name);
            if (node==null)
                throw new NodeNotFoundError(path);
        }
        
        return node;
    }

    public void remove(Node node)
    {
        Collection<Node> childrens = node.getChildrens();
        
        if (childrens!=null)
            for (Node children: childrens)
                remove(children);
        
        configurator.delete(node);
    }

    private void init() 
    {
        buildTree();
    }

    private void buildTree()
    {
        Collection<BaseNode> nodes = configurator.getObjects(BaseNode.class, "level ascending");

        if (nodes == null || nodes.size() == 0)
        {
            createRootNode();
        } else
        {
            Iterator<BaseNode> it = nodes.iterator();
            rootNode = it.next();
            Map<Long, BaseNode> cache = new HashMap<Long, BaseNode>();
            cache.put(rootNode.getId(), rootNode);
            while (it.hasNext())
            {
                BaseNode node = it.next();
                cache.get(node.getParent().getId()).addChildren(node);
                cache.put(node.getId(), node);
            }
        }
    }

    private void createRootNode()
    {
        rootNode = new ContainerNode("/");
        
        configurator.saveInTransaction(rootNode);
        
        createSystemSubtree();
    }

    private void createSystemSubtree()
    {
        SystemNode systemNode = new SystemNode();
        rootNode.addChildren(systemNode);
        
        configurator.saveInTransaction(systemNode);
        
        DataSourcesNode dataSourcesNode = new DataSourcesNode();
        systemNode.addChildren(dataSourcesNode);
        
        configurator.saveInTransaction(dataSourcesNode);
    }
}

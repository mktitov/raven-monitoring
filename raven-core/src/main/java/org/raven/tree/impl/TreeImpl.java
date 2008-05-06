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
import java.util.Iterator;
import org.raven.conf.Configurator;
import org.raven.tree.Node;
import org.raven.tree.NodeNotFoundError;
import org.raven.tree.Tree;
import org.raven.tree.store.TreeStore;
import org.raven.tree.store.TreeStoreError;
import org.weda.internal.exception.NullParameterError;

/**
 *
 * @author Mikhail Titov
 */
public class TreeImpl implements Tree
{
    public static Tree INSTANCE;
    
    private final Configurator configurator;
    private final TreeStore treeStore;
    private Node rootNode;

    public TreeImpl(Configurator configurator)
    {
        this.configurator = configurator;
        this.treeStore = configurator.getTreeStore();
        INSTANCE = this;
        
        reloadTree();
    }

    public Node getRootNode()
    {
        return rootNode;
    }

    public Node getNode(String path) throws NodeNotFoundError
    {
        NullParameterError.check("path", path);
        
        if (path.length()<2 || !path.startsWith(Node.NODE_SEPARATOR))
            throw new NodeNotFoundError(String.format("Invalid path (%s)", path));
        
        String[] names = path.substring(1).split(Node.NODE_SEPARATOR);
        
        Node node = rootNode;
        for (String name: names)
        {
            node = node.getChildren(name);
            if (node==null)
                throw new NodeNotFoundError(path);
        }
        
        return node;
    }

    public void reloadTree() throws TreeStoreError
    {

        rootNode = treeStore.getRootNode();

        if (rootNode == null)
        {
            createRootNode();
        }
        
        initNode(rootNode);
    }

    public void remove(Node node)
    {
        Collection<Node> childrens = node.getChildrens();
        
        if (childrens!=null)
            for (Node children: childrens)
                remove(children);
    
        node.shutdown();
        
        configurator.getTreeStore().removeNode(node.getId());
    }

    private void createRootNode()
    {
        rootNode = new ContainerNode("");
        
        treeStore.saveNode(rootNode);
        
        createSystemSubtree();
    }

    private void createSystemSubtree()
    {
        SystemNode systemNode = new SystemNode();
        rootNode.addChildren(systemNode);
        
        treeStore.saveNode(systemNode);
        
        DataSourcesNode dataSourcesNode = new DataSourcesNode();
        systemNode.addChildren(dataSourcesNode);
        
        treeStore.saveNode(dataSourcesNode);
    }

    private void initNode(Node node)
    {
        node.init();
        if (node.getChildrens()!=null)
        {
            Iterator<Node> it = node.getChildrens().iterator();
            while (it.hasNext())
                initNode(it.next());
        }
    }
}

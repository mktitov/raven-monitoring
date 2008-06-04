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
import java.util.List;
import java.util.Map;
import org.raven.conf.Configurator;
import org.raven.impl.NodeClassTransformerWorker;
import org.raven.template.TemplateVariable;
import org.raven.tree.AttributeReference;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeNotFoundError;
import org.raven.tree.Tree;
import org.raven.tree.store.TreeStore;
import org.raven.tree.store.TreeStoreError;
import org.weda.internal.exception.NullParameterError;
import org.weda.internal.services.ResourceProvider;

/**
 *
 * @author Mikhail Titov
 */
public class TreeImpl implements Tree
{
    public static Tree INSTANCE;
    
    private final Configurator configurator;
    private final TreeStore treeStore;
    private final Class[] nodesTypes;
    private final Map<Class, AttributeReferenceValues> referenceValuesProviders;
    private Node rootNode;

    public TreeImpl(
            Map<Class, AttributeReferenceValues> referenceValuesProviders
            , Configurator configurator, ResourceProvider resourceProvider) 
        throws Exception
    {
        this.configurator = configurator;
        this.treeStore = configurator.getTreeStore();
        this.referenceValuesProviders = referenceValuesProviders;
        INSTANCE = this;
        
        List<String> nodesTypesList = 
                resourceProvider.getResourceStrings(
                    NodeClassTransformerWorker.NODES_TYPES_RESOURCE);
        
        nodesTypes = new Class[nodesTypesList.size()];
        int i=0;
        for (String nodeType: nodesTypesList)
            nodesTypes[i++] = Class.forName(nodeType);
        
        reloadTree();
    }

    public Class[] getAvailableNodesTypes()
    {
        return nodesTypes;
    }

    public Class[] getNodeAttributesTypes(Node node)
    {
        if (node.isTemplate())
            return new Class[]{
                String.class, Integer.class, Double.class, Node.class, AttributeReference.class
                , TemplateVariable.class};
        else
            return new Class[]{
                String.class, Integer.class, Double.class, Node.class, AttributeReference.class};
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
        shutdown();
        rootNode = null;
    
        rootNode = treeStore.getRootNode();
        if (rootNode == null)
        {
            createRootNode();
        }
        initNode(rootNode);
    }
    
    public void shutdown()
    {
        if (rootNode!=null)
            shutdownNode(rootNode);
    }

    public void remove(Node node)
    {
        Collection<Node> childrens = node.getChildrens();
        
        if (childrens!=null)
            for (Node children: childrens)
                remove(children);
    
        
        node.shutdown();
        
        treeStore.removeNode(node.getId());
        
        if (node.getParent()!=null)
            node.getParent().removeChildren(node);
    }

    public List<String> getReferenceValuesForAttribute(NodeAttribute attr)
    {
        AttributeReferenceValues provider = referenceValuesProviders.get(attr.getType());
        if (provider!=null)
        {
            return provider.getReferenceValues(attr);
        }
        else
        {
            for (Map.Entry<Class, AttributeReferenceValues> entry: 
                    referenceValuesProviders.entrySet())
            {
                if (entry.getKey().isAssignableFrom(attr.getType()))
                    return entry.getValue().getReferenceValues(attr);
            }
            return null;
        }
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
        if (!node.isInitializeAfterChildrens())
        {
            node.init();
            if (node.getStatus()==Node.Status.INITIALIZED && node.isAutoStart())
                node.start();
        }
        if (node.getChildrens()!=null)
        {
            Iterator<Node> it = node.getChildrens().iterator();
            while (it.hasNext())
                initNode(it.next());
        }
        if (node.isInitializeAfterChildrens())
        {
            node.init();
            if (node.getStatus()==Node.Status.INITIALIZED && node.isAutoStart())
                node.start();
        }
    }

    private void shutdownNode(Node node)
    {
       Collection<Node> childrens = node.getChildrens();
       
       if (childrens!=null)
           for (Node children: childrens)
               shutdownNode(children);
       
       node.shutdown();
    }
}

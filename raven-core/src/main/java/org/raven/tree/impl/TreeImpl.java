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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.raven.annotations.NodeClass;
import org.raven.conf.Configurator;
import org.raven.impl.NodeClassTransformerWorker;
import org.raven.template.TemplateVariable;
import org.raven.template.TemplatesNode;
import org.raven.tree.AttributeReference;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeNotFoundError;
import org.raven.tree.NodeTuner;
import org.raven.tree.Tree;
import org.raven.tree.TreeError;
import org.raven.tree.store.TreeStore;
import org.raven.tree.store.TreeStoreError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.internal.exception.NullParameterError;
import org.weda.internal.services.ResourceProvider;

/**
 *
 * @author Mikhail Titov
 */
public class TreeImpl implements Tree
{
    protected Logger logger = LoggerFactory.getLogger(Node.class);
    
    public static Tree INSTANCE;
    
    private final Configurator configurator;
    private final TreeStore treeStore;
    private final Map<Class, AttributeReferenceValues> referenceValuesProviders;
    private final Map<Class, List<Class>> nodeTypes = new HashMap<Class, List<Class>>();
    private final Set<Class> anyChildTypeNodes = new HashSet<Class>();
    private Node rootNode;
    private SystemNode systemNode;
    private DataSourcesNode dataSourcesNode;
    private TemplatesNode templatesNode;

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
        
        for (String nodeType: nodesTypesList)
            addNodeType(Class.forName(nodeType));
        
        reloadTree();
    }

    public List<Class> getChildNodesTypes(Class nodeType)
    {
        nodeType = anyChildTypeNodes.contains(nodeType)? Void.class : nodeType;
        return nodeTypes.get(nodeType);
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
        logger.info("Reloading tree");
        shutdown();
        rootNode = null;
    
        rootNode = treeStore.getRootNode();
        if (rootNode == null)
            createRootNode();
        
        createSystemNodes();
        
        initNode(rootNode, true);
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
            for (Node children: new ArrayList<Node>(childrens))
                remove(children);
    
        
        node.shutdown();
        
        treeStore.removeNode(node.getId());
        
        if (node.getParent()!=null)
            node.getParent().removeChildren(node);
    }

    public Node copy(
            Node source, Node destination, String newNodeName, NodeTuner nodeTuner
            , boolean store, boolean validateNodeType)
    {
        if (validateNodeType)
        {
            List<Class> childTypes = destination.getChildNodeTypes();
            if (childTypes!=null)
            {
                boolean isValidChildType = false;
                for (Class childType: childTypes)
                    if (childType.equals(source.getClass()))
                    {
                        isValidChildType = true;
                        break;
                    }
                if (!isValidChildType)
                    throw new TreeError(String.format(
                            "The source node type (%s) is not a valid type for " +
                            "destination node (%s)"
                            , source.getClass().getName(), destination.getPath()));
            }
        }
        try
        {
            Node clone = (Node) source.clone();
            if (newNodeName!=null)
                clone.setName(newNodeName);
            destination.addChildren(clone);
            saveClonedNode(source, clone, destination.getPath(), clone, nodeTuner, store);
            initNode(clone, false);
            
            return clone;
        } 
        catch (CloneNotSupportedException ex)
        {
            throw new TreeError("Node (%s) clone error", ex);
        }
    }
    
    public void start(Node node)
    {
        if (node.getStatus()==Status.INITIALIZED)
            node.start();
        
        if (node.getChildrens()!=null)
            for (Node child: node.getChildrens())
                start(child);
    }

    public void stop(Node node)
    {
        if (node.getStatus()==Status.STARTED)
            node.stop();
        
        if (node.getChildrens()!=null)
            for (Node child: node.getChildrens())
                stop(child);
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

    public List<Node> getTempltateNodes()
    {
        Collection<Node> result = templatesNode.getSortedChildrens();
        return result==null? Collections.EMPTY_LIST : new ArrayList<Node>(result);
    }

    private void addChildsToParent(Class parent, Class... childs)
    {

        List<Class> childTypes = nodeTypes.get(parent);
        if (childTypes == null)
        {
            childTypes = new ArrayList<Class>();
            nodeTypes.put(parent, childTypes);
        }
        for (Class child: childs)
            if (!child.equals(Void.class))
                childTypes.add(child);
    }
    
    private void createRootNode()
    {
        rootNode = new ContainerNode("");
        treeStore.saveNode(rootNode);
    }

    private void createSystemNodes()
    {
        createSystemSubtree();
        createTempatesSubtree();
    }

    private void createSystemSubtree()
    {
        systemNode = (SystemNode) rootNode.getChildren(SystemNode.NAME);
        if (systemNode==null)
        {
            systemNode = new SystemNode();
            rootNode.addChildren(systemNode);
            treeStore.saveNode(systemNode);
        }
        
        dataSourcesNode = (DataSourcesNode) systemNode.getChildren(DataSourcesNode.NAME);
        if (dataSourcesNode==null)
        {
            dataSourcesNode = new DataSourcesNode();
            systemNode.addChildren(dataSourcesNode);
            treeStore.saveNode(dataSourcesNode);
        }
    }

    private void createTempatesSubtree()
    {
        templatesNode = (TemplatesNode) rootNode.getChildren(TemplatesNode.NAME);
        if (templatesNode==null)
        {
            templatesNode = new TemplatesNode();
            rootNode.addChildren(templatesNode);
            treeStore.saveNode(templatesNode);
        }
    }

    private void initNode(Node node, boolean autoStart)
    {
        if (logger.isDebugEnabled())
            logger.debug(String.format("Initializing node (%s)", node.getPath()));
        if (!node.isInitializeAfterChildrens())
        {
            node.init();
            if (autoStart && node.getStatus()==Node.Status.INITIALIZED && node.isAutoStart())
                node.start();
        }
        if (node.getChildrens()!=null)
        {
            Iterator<Node> it = node.getChildrens().iterator();
            while (it.hasNext())
                initNode(it.next(), autoStart);
        }
        if (node.isInitializeAfterChildrens())
        {
            node.init();
            if (autoStart && node.getStatus()==Node.Status.INITIALIZED && node.isAutoStart())
                node.start();
        }
    }

    private void saveClonedNode(
            final Node source, final Node clonedSource, final String destPath, final Node clone
            , final NodeTuner nodeTuner, final boolean store)
    {
        if (nodeTuner!=null)
            nodeTuner.tuneNode(clone);
        if (store)
            configurator.getTreeStore().saveNode(clone);
        Collection<NodeAttribute> attrs = clone.getNodeAttributes();
        if (attrs!=null)
        {
            String sourcePath = source.getPath();
            for (NodeAttribute attr: attrs)
            {
                if (   (attr.isAttributeReference() || Node.class.isAssignableFrom(attr.getType()))
                    && attr.getRawValue()!=null && attr.getRawValue().startsWith(sourcePath))
                {
                    attr.setRawValue(
                            destPath
                            + Node.NODE_SEPARATOR+clonedSource.getName()
                            + attr.getRawValue().substring(sourcePath.length()));
                }
                if (store)
                    configurator.getTreeStore().saveNodeAttribute(attr);
            }
        }
        
        Collection<Node> childs = clone.getChildrens();
        if (childs!=null)
            for (Node child: childs)
                saveClonedNode(source, clonedSource, destPath, child, nodeTuner, store);
    }

    private void shutdownNode(Node node)
    {
       Collection<Node> childrens = node.getChildrens();
       
       if (childrens!=null)
           for (Node children: childrens)
               shutdownNode(children);
       
       node.shutdown();
    }
    
    private void addNodeType(Class nodeType)
    {
        NodeClass ann = (NodeClass) nodeType.getAnnotation(NodeClass.class);
        if (ann==null)
            throw new TreeError(String.format(
                    "Annotation @NodeClass not found in the node class (%s)", nodeType.getName()));
        
        if (ann.anyChildTypes())
            anyChildTypeNodes.add(nodeType);
        addChildsToParent(ann.parentNode(), nodeType);
        addChildsToParent(nodeType, ann.childNodes());
    }
}

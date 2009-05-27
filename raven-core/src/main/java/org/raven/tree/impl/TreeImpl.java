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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.raven.annotations.NodeClass;
import org.raven.conf.Configurator;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.expr.BindingSupport;
import org.raven.impl.ClassNameComparator;
import org.raven.impl.NodeClassTransformerWorker;
import org.raven.log.impl.NodeLoggerNode;
import org.raven.net.impl.NetworkResponseServiceNode;
import org.raven.sched.impl.SchedulersNode;
import org.raven.template.impl.TemplateVariable;
import org.raven.template.impl.TemplatesNode;
import org.raven.tree.AttributeReference;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.AttributeValueHandlerRegistry;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.NodeListener;
import org.raven.tree.NodePathResolver;
import org.raven.tree.NodeTuner;
import org.raven.tree.PathInfo;
import org.raven.tree.ScanOperation;
import org.raven.tree.ScanOptions;
import org.raven.tree.ScannedNodeHandler;
import org.raven.tree.SearchFilter;
import org.raven.tree.SearchOptions;
import org.raven.tree.Tree;
import org.raven.tree.TreeError;
import org.raven.tree.TreeException;
import org.raven.tree.UnableMoveRootException;
import org.raven.tree.UnableMoveToSelfException;
import org.raven.tree.store.TreeStore;
import org.raven.tree.store.TreeStoreError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.beans.ObjectUtils;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.ReferenceValueCollection;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueCollectionImpl;
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

    private final AttributeReferenceValues attributeReferenceValues;
    private final Configurator configurator;
    private final TreeStore treeStore;
    private final NodePathResolver pathResolver;
    private final AttributeValueHandlerRegistry valueHandlerRegistry;

    private final Map<Class, List<Class>> nodeTypes = new HashMap<Class, List<Class>>();
    private final Set<Class> anyChildTypeNodes = new HashSet<Class>();
    private final Set<Class> importParentChildTypeNodes = new HashSet<Class>();
    private RootNode rootNode;
    private SystemNode systemNode;
    private DataSourcesNode dataSourcesNode;
    private TemplatesNode templatesNode;
    private ConnectionPoolsNode connectionPoolsNode;
    private SchedulersNode schedulersNode;
    private NodeLoggerNode nodeLoggerNode;
	private QueuesNode queuesNode;
    private SchemasNode schemasNode;
    private LocalDatabaseNode localDatabaseNode;
    private NetworkResponseServiceNode responseServiceNode;
    private ServicesNode servicesNode;

    public TreeImpl(
            AttributeReferenceValues attributeReferenceValues
            , Configurator configurator
            , ResourceProvider resourceProvider
            , NodePathResolver pathResolver
            , AttributeValueHandlerRegistry valueHandlerRegistry) 
        throws Exception
    {
        this.configurator = configurator;
        this.treeStore = configurator.getTreeStore();
//        this.referenceValuesProviders = referenceValuesProviders;
        this.pathResolver = pathResolver;
        this.attributeReferenceValues = attributeReferenceValues;
        this.valueHandlerRegistry = valueHandlerRegistry;
        
        INSTANCE = this;
        
        List<String> nodesTypesList = 
                resourceProvider.getResourceStrings(
                    NodeClassTransformerWorker.NODES_TYPES_RESOURCE);
        
        for (String nodeType: nodesTypesList)
            addNodeType(Class.forName(nodeType));
        
//        reloadTree();
    }

    public List<Class> getChildNodesTypes(Node node)
    {
        Set<Class> childTypes = new HashSet<Class>();
        collectChildTypes(node, childTypes);
        
        List<Class> types = new ArrayList<Class>(childTypes);
        Collections.sort(types, new ClassNameComparator());
        
        return types;
    }

    public Class[] getNodeAttributesTypes(Node node)
    {
        if (node.isTemplate())
            return new Class[]{
                String.class, Integer.class, Double.class, Boolean.class, Node.class
                , AttributeReference.class, TemplateVariable.class};
        else
            return new Class[]{
                String.class, Integer.class, Double.class, Boolean.class, Node.class
                , AttributeReference.class};
    }

    public Node getRootNode()
    {
        return rootNode;
    }

    public Node getNode(String path) throws InvalidPathException
    {
        NullParameterError.check("path", path);
        
        PathInfo<Node> pathInfo = pathResolver.resolvePath(path, rootNode);
        
        return pathInfo.getReferencedObject();
    }

    public void reloadTree() throws TreeStoreError
    {
        long curTime = System.currentTimeMillis();
        logger.info("Reloading tree");
        shutdown();
        rootNode = null;
    
        rootNode = (RootNode) treeStore.getRootNode();
        if (rootNode == null)
            createRootNode();
        
        createSystemNodes();

		try
		{
			logger.info("Initializing tree nodes.");
			initNode(rootNode, null);
			long operationTime = (System.currentTimeMillis()-curTime)/1000;
			logger.info(String.format("Tree nodes initialized in %d seconds", operationTime));

			logger.info("Starting tree nodes");
			long curTime2 = System.currentTimeMillis();
			start(rootNode, true);
			operationTime = (System.currentTimeMillis()-curTime2)/1000;
			logger.info(String.format("Tree nodes started in %d seconds", operationTime));

			operationTime = (System.currentTimeMillis()-curTime)/1000;
			logger.info(String.format("Tree reloaded in %d seconds", operationTime));
		}
		catch(Throwable e)
		{
			logger.error("Error initializing or starting node", e);
		}
    }
    
    public void shutdown()
    {
        logger.info("Shutdowning the tree");
        if (rootNode!=null)
            shutdownNode(rootNode);
        logger.info("Tree shutdowned");
    }

    public void remove(Node node)
    {
        if (logger.isDebugEnabled())
            logger.debug(String.format("Removing node (%s)", node.getPath()));
        
        node.setStatus(Status.REMOVING);
        
        Collection<Node> childrens = node.getChildrens();
        
        if (childrens!=null)
            for (Node children: new ArrayList<Node>(childrens))
                remove(children);
    
        
        node.shutdown();
        
        
        if (node.getParent()!=null)
            node.getParent().removeChildren(node);
        
        treeStore.removeNode(node.getId());
    }

    public Node copy(
            Node source, Node destination, String newNodeName, NodeTuner nodeTuner
            , boolean store, boolean validateNodeType, boolean useEffectiveChildrens)
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
//            Node clone = (Node) source.clone();
//            if (newNodeName!=null)
//                clone.setName(newNodeName);
//            destination.addChildren(clone);
            Node clone = source.cloneTo(destination, newNodeName, nodeTuner, useEffectiveChildrens);
            saveClonedNode(source, clone, destination.getPath(), clone, store);
            initNode(clone, nodeTuner);
            
            return clone;
        } 
        catch (CloneNotSupportedException ex)
        {
            throw new TreeError("Node (%s) clone error", ex);
        }
    }

    public void move(Node source, Node destination) throws TreeException
    {
        Node sourceParent = source.getParent();
        if (sourceParent==null)
            throw new UnableMoveRootException();

        Node node = destination;
        while (node!=null && node!=source)
            node = node.getParent();
        if (node!=null)
            throw new UnableMoveToSelfException(source, destination);

        byte oldLevel = source.getLevel();
        sourceParent.detachChildren(source);
        destination.addChildren(source);
        source.save();

        if (source.getChildrenCount()>0)
        {
            boolean saveChilds = oldLevel!=source.getLevel();
            ScannedNodeHandler handler = new SaveSubtreeHandler(saveChilds);
            scanSubtree(source, handler, ScanOptionsImpl.EMPTY_OPTIONS);
        }
        
    }

    public List<Node> search(Node searchFromNode, SearchOptions options, SearchFilter filter) 
    {
        List<Node> result = new ArrayList<Node>();
        ScannedNodeHandler handler = new SearchScannedNodeHandler(result, options, filter);
        scanSubtree(searchFromNode, handler, ScanOptionsImpl.EMPTY_OPTIONS);
        return result;
    }
    
    public void start(Node node, boolean autoStartOnly)
    {
		try
		{
			if (!node.isStartAfterChildrens() && node.getStatus()==Status.INITIALIZED
                && (!autoStartOnly || node.isAutoStart()))
            {
				node.start();
            }
		}
		catch(NodeError e)
		{
			logger.error("Error starting node (%s)", e);
		}

        Collection<Node> childs = node.getSortedChildrens();
		if (childs!=null)
			for (Node child: childs)
				start(child, autoStartOnly);

		try
		{
			if (node.isStartAfterChildrens() && node.getStatus()==Status.INITIALIZED
                && (!autoStartOnly || node.isAutoStart()))
            {
				node.start();
            }
		}
		catch(NodeError e)
		{
			logger.error("Error starting node (%s)", e);
		}
    }

    public void stop(Node node)
    {
        if (node.getStatus()==Status.STARTED)
            node.stop();
        
        if (node.getChildrens()!=null)
            for (Node child: node.getChildrens())
                stop(child);
    }

    public boolean scanSubtree(
            Node startingPoint, ScannedNodeHandler handler, ScanOptions options)
    {
        Collection<Node> childrens = options.sortBeforeScan()?
            startingPoint.getSortedChildrens() : startingPoint.getChildrens();
        if (childrens!=null)
            for (Node node: childrens)
            {
                ScanOperation operation = ScanOperation.CONTINUE;
                if (   (   options.includeNodeTypes()==null 
                        || ObjectUtils.in(node.getClass(), options.includeNodeTypes()))
                    && (   options.includeStatuses()==null
                        || options.includeStatuses().length==0 
                        || ObjectUtils.in(node.getStatus(), options.includeStatuses())))
                {
                    operation = handler.nodeScanned(node);
                }
                if (operation==ScanOperation.CONTINUE)
                {
                    boolean continueScan = scanSubtree(node, handler, options);
                    if (!continueScan)
                        return false;
                }
                else if (operation==ScanOperation.STOP)
                    return false;
            }
        return true;
    }

    public List<ReferenceValue> getReferenceValuesForAttribute(NodeAttribute attr)
    {
        ReferenceValueCollection values = new ReferenceValueCollectionImpl(Integer.MAX_VALUE, null);
        try
        {
            attributeReferenceValues.getReferenceValues(attr, values);
            List<ReferenceValue> result = values.asList();
            return result.size()==0? null : values.asList();
        } 
        catch (TooManyReferenceValuesException ex)
        {
            return null;
        }
    }

    public List<Node> getTempltateNodes()
    {
        Collection<Node> result = templatesNode.getSortedChildrens();
        return result==null? Collections.EMPTY_LIST : new ArrayList<Node>(result);
    }

    public List<ReferenceValue> getAttributeValueHandlerTypes(NodeAttribute attr)
    {
        List<ReferenceValue> types = valueHandlerRegistry.getValueHandlerTypes();
        List<ReferenceValue> sortedTypes = new ArrayList<ReferenceValue>(types);
        Collections.sort(sortedTypes, new ReferenceValueComparator());

        return sortedTypes;
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

    private void collectChildTypes(Node node, Set<Class> types) 
    {
        Class[] typesArr = null;
        if (anyChildTypeNodes.contains(node.getClass()))
            typesArr = new Class[]{Void.class, node.getClass()};
        else
            typesArr = new Class[]{node.getClass()};
        for (Class nodeType: typesArr)
        {
            List<Class> childTypes = nodeTypes.get(nodeType);
            if (childTypes!=null)
                types.addAll(childTypes);
        }
        
        if (node.getParent()!=null && importParentChildTypeNodes.contains(node.getClass()))
            collectChildTypes(node.getParent(), types);
    }
    
    private void createRootNode()
    {
        rootNode = new RootNode();
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

        schedulersNode = (SchedulersNode) systemNode.getChildren(SchedulersNode.NAME);
        if (schedulersNode==null)
        {
            schedulersNode = new SchedulersNode();
            systemNode.addChildren(schedulersNode);
            treeStore.saveNode(schedulersNode);
        }

        localDatabaseNode = (LocalDatabaseNode) systemNode.getChildren(LocalDatabaseNode.NAME);
        if (localDatabaseNode==null)
        {
            localDatabaseNode = new LocalDatabaseNode();
            localDatabaseNode.setParent(systemNode);
            treeStore.saveNode(localDatabaseNode);
            systemNode.addChildren(localDatabaseNode);
        }
        
        connectionPoolsNode = (ConnectionPoolsNode)systemNode.getChildren(ConnectionPoolsNode.NAME);
        if (connectionPoolsNode==null)
        {
            connectionPoolsNode = new ConnectionPoolsNode();
            systemNode.addChildren(connectionPoolsNode);
            treeStore.saveNode(connectionPoolsNode);
        }
        
        dataSourcesNode = (DataSourcesNode) systemNode.getChildren(DataSourcesNode.NAME);
        if (dataSourcesNode==null)
        {
            dataSourcesNode = new DataSourcesNode();
            systemNode.addChildren(dataSourcesNode);
            treeStore.saveNode(dataSourcesNode);
        }

		queuesNode = (QueuesNode) systemNode.getChildren(QueuesNode.NAME);
		if (queuesNode==null)
		{
			queuesNode = new QueuesNode();
			systemNode.addChildren(queuesNode);
			treeStore.saveNode(queuesNode);
		}

        schemasNode = (SchemasNode) systemNode.getChildren(SchemasNode.NAME);
        if (schemasNode==null)
        {
            schemasNode = new SchemasNode();
            schemasNode.setParent(systemNode);
            treeStore.saveNode(schemasNode);
            systemNode.addChildren(schemasNode);
        }

        servicesNode = (ServicesNode) systemNode.getChildren(ServicesNode.NAME);
        if (servicesNode==null)
        {
            servicesNode = new ServicesNode();
            servicesNode.setParent(systemNode);
            treeStore.saveNode(servicesNode);
            systemNode.addChildren(servicesNode);
        }

        responseServiceNode = (NetworkResponseServiceNode)
                servicesNode.getChildren(NetworkResponseServiceNode.NAME);
        if (responseServiceNode==null)
        {
            responseServiceNode = new NetworkResponseServiceNode();
            responseServiceNode.setParent(servicesNode);
            treeStore.saveNode(responseServiceNode);
            servicesNode.addChildren(responseServiceNode);
        }
        
        nodeLoggerNode = (NodeLoggerNode) servicesNode.getChildren(NodeLoggerNode.NAME);
        if (nodeLoggerNode==null)
        {
            nodeLoggerNode = new NodeLoggerNode();
            nodeLoggerNode.setParent(servicesNode);
            treeStore.saveNode(nodeLoggerNode);
            servicesNode.addChildren(nodeLoggerNode);
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

    private void initNode(Node node, NodeTuner nodeTuner)
    {
        if (logger.isDebugEnabled())
            logger.debug(String.format("Initializing node (%s)", node.getPath()));
        if (!node.isInitializeAfterChildrens())
        {
            node.init();
            if (nodeTuner!=null)
                nodeTuner.finishTuning(node);
        }
        Collection<Node> childs = node.getSortedChildrens();
        if (childs!=null)
        {
            Iterator<Node> it = childs.iterator();
            while (it.hasNext())
                initNode(it.next(), nodeTuner);
        }
        if (node.isInitializeAfterChildrens())
        {
            node.init();
            if (nodeTuner!=null)
                nodeTuner.finishTuning(node);
        }
    }

    private void saveClonedNode(
            final Node source, final Node clonedSource, final String destPath, final Node clone
            , final boolean store)
    {
        if (store)
            configurator.getTreeStore().saveNode(clone);
        Collection<NodeAttribute> attrs = clone.getNodeAttributes();
        if (attrs!=null)
        {
//            String sourcePath = source.getPath();
            for (NodeAttribute attr: attrs)
            {
//                if (   (attr.isAttributeReference() || Node.class.isAssignableFrom(attr.getType()))
//                    && attr.getRawValue()!=null && attr.getRawValue().startsWith(sourcePath))
//                {
//                    attr.setRawValue(
//                            destPath
//                            + Node.NODE_SEPARATOR+clonedSource.getName()
//                            + attr.getRawValue().substring(sourcePath.length()));
//                }
                if (store)
                    configurator.getTreeStore().saveNodeAttribute(attr);
            }
        }
        
        Collection<Node> childs = clone.getChildrens();
        if (childs!=null)
            for (Node child: childs)
                saveClonedNode(source, clonedSource, destPath, child, store);
    }

    private void shutdownNode(Node node)
    {
       Collection<Node> childrens = node.getSortedChildrens();
       
       if (childrens!=null)
           for (Node children: childrens)
               shutdownNode(children);

       String nodePath = node.getPath();
       try{
           node.shutdown();
       }catch(Throwable e)
       {
           logger.error(String.format("Error stoping node (%s)", nodePath), e);
       }
    }
    
    private void addNodeType(Class nodeType)
    {
        NodeClass ann = (NodeClass) nodeType.getAnnotation(NodeClass.class);
        if (ann==null)
            throw new TreeError(String.format(
                    "Annotation @NodeClass not found in the node class (%s)", nodeType.getName()));
        
        if (ann.anyChildTypes())
            anyChildTypeNodes.add(nodeType);
        if (ann.importChildTypesFromParent())
            importParentChildTypeNodes.add(nodeType);
        addChildsToParent(ann.parentNode(), nodeType);
        addChildsToParent(nodeType, ann.childNodes());
    }

    public void addGlobalBindings(String bindingSupportId, BindingSupport bindingSupport)
    {
        rootNode.addBindingSupport(bindingSupportId, bindingSupport);
    }

    public void removeGlobalBindings(String bindingSupportId)
    {
        rootNode.removeBindingSupport(bindingSupportId);
    }
    
    private class SearchScannedNodeHandler implements ScannedNodeHandler
    {
        private final List<Node> foundNodes;
        private final SearchOptions searchOptions;
        private final SearchFilter searchFilter;

        public SearchScannedNodeHandler(
                List<Node> foundNodes, SearchOptions searchOptions, SearchFilter searchFilter) 
        {
            this.foundNodes = foundNodes;
            this.searchOptions = searchOptions;
            this.searchFilter = searchFilter;
        }

        public ScanOperation nodeScanned(Node node) 
        {
            if (searchFilter.filter(node))
            {
                foundNodes.add(node);
                if (searchOptions.isFindFirst())
                    return ScanOperation.STOP;
            }
            return ScanOperation.CONTINUE;
        }
    }

    private class ReferenceValueComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            ReferenceValue r1 = (ReferenceValue) o1;
            ReferenceValue r2 = (ReferenceValue) o2;
            
            return r1.getValueAsString().compareTo(r2.getValueAsString());
        }
    }

    private class SaveSubtreeHandler implements ScannedNodeHandler
    {
        private final boolean saveNodes;

        public SaveSubtreeHandler(boolean saveNodes)
        {
            this.saveNodes = saveNodes;
        }

        public ScanOperation nodeScanned(Node node)
        {
            if (saveNodes)
                node.save();
            Collection<NodeListener> listeners = node.getListeners();
            if (listeners!=null && listeners.isEmpty())
                for (NodeListener listener: listeners)
                    listener.nodeMoved(node);
            return ScanOperation.CONTINUE;
        }
    }
}

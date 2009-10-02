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

import java.io.InputStream;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.raven.annotations.NodeClass;
import org.raven.audit.impl.AuditorNode;
import org.raven.conf.Configurator;
import org.raven.conf.impl.AuthorizationNode;
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
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
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
    protected Logger logger = LoggerFactory.getLogger(Tree.class);
    
    public static Tree INSTANCE;

    private final AttributeReferenceValues attributeReferenceValues;
    @SuppressWarnings("unused")
	private final Configurator configurator;
    private final TreeStore treeStore;
    private final NodePathResolver pathResolver;
    private final AttributeValueHandlerRegistry valueHandlerRegistry;

    @SuppressWarnings("unchecked")
	private final Map<Class, List<Class>> nodeTypes = new HashMap<Class, List<Class>>();
    @SuppressWarnings("unchecked")
	private final Set<Class> anyChildTypeNodes = new HashSet<Class>();
    @SuppressWarnings("unchecked")
	private final Set<Class> importParentChildTypeNodes = new HashSet<Class>();
    private final AtomicInteger dynamicNodeId = new AtomicInteger();
    private final AtomicInteger dynamicAttributeId = new AtomicInteger();
    private RootNode rootNode;
    private SystemNode systemNode;
    private AuthorizationNode authorizationNode;    
    private DataSourcesNode dataSourcesNode;
    private TemplatesNode templatesNode;
    private ConnectionPoolsNode connectionPoolsNode;
    private SchedulersNode schedulersNode;
    private NodeLoggerNode nodeLoggerNode;
    private AuditorNode auditorNode;
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

    @SuppressWarnings("unchecked")
	public List<Class> getChildNodesTypes(Node node)
    {
        Set<Class> childTypes = new HashSet<Class>();
        collectChildTypes(node, childTypes);
        
        List<Class> types = new ArrayList<Class>(childTypes);
        Collections.sort(types, new ClassNameComparator());
        
        return types;
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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
            NodeTuner wrapper = new CopyBinaryAttrsTuner(nodeTuner);
            Node clone = source.cloneTo(destination, newNodeName, wrapper, useEffectiveChildrens);
            saveClonedNode(source, clone, destination.getPath(), clone, store);
            initNode(clone, wrapper);
            
            return clone;
        } 
        catch (CloneNotSupportedException ex)
        {
            throw new TreeError("Node (%s) clone error", ex);
        }
    }

    public void move(Node source, Node destination, String newSourceName) throws TreeException
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
        if (newSourceName!=null)
            source.setName(newSourceName);
        destination.addChildren(source);
        source.save();

        fireNodeMovedEvent(source);
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
                if (logger.isDebugEnabled())
                    logger.debug("Starting node "+node.getPath());
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
                if (logger.isDebugEnabled())
                    logger.debug("Starting node "+node.getPath());
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

    @SuppressWarnings("unchecked")
	public List<Node> getTempltateNodes()
    {
        Collection<Node> result = templatesNode.getSortedChildrens();
        return result==null? Collections.EMPTY_LIST : new ArrayList<Node>(result);
    }

    @SuppressWarnings("unchecked")
	public List<ReferenceValue> getAttributeValueHandlerTypes(NodeAttribute attr)
    {
        List<ReferenceValue> types = valueHandlerRegistry.getValueHandlerTypes();
        List<ReferenceValue> sortedTypes = new ArrayList<ReferenceValue>(types);
        Collections.sort(sortedTypes, new ReferenceValueComparator());

        return sortedTypes;
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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
        saveNode(rootNode);
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
            saveNode(systemNode);
        }

        authorizationNode = (AuthorizationNode) systemNode.getChildren(AuthorizationNode.NODE_NAME);
        if (authorizationNode==null)
        {
        	authorizationNode = new AuthorizationNode();
        	authorizationNode.setParent(systemNode);
            saveNode(authorizationNode);
            systemNode.addChildren(authorizationNode);
        }
        
        schedulersNode = (SchedulersNode) systemNode.getChildren(SchedulersNode.NAME);
        if (schedulersNode==null)
        {
            schedulersNode = new SchedulersNode();
            systemNode.addChildren(schedulersNode);
            saveNode(schedulersNode);
        }

        localDatabaseNode = (LocalDatabaseNode) systemNode.getChildren(LocalDatabaseNode.NAME);
        if (localDatabaseNode==null)
        {
            localDatabaseNode = new LocalDatabaseNode();
            localDatabaseNode.setParent(systemNode);
            saveNode(localDatabaseNode);
            systemNode.addChildren(localDatabaseNode);
        }
        
        connectionPoolsNode = (ConnectionPoolsNode)systemNode.getChildren(ConnectionPoolsNode.NAME);
        if (connectionPoolsNode==null)
        {
            connectionPoolsNode = new ConnectionPoolsNode();
            systemNode.addChildren(connectionPoolsNode);
            saveNode(connectionPoolsNode);
        }
        
        dataSourcesNode = (DataSourcesNode) systemNode.getChildren(DataSourcesNode.NAME);
        if (dataSourcesNode==null)
        {
            dataSourcesNode = new DataSourcesNode();
            systemNode.addChildren(dataSourcesNode);
            saveNode(dataSourcesNode);
        }

		queuesNode = (QueuesNode) systemNode.getChildren(QueuesNode.NAME);
		if (queuesNode==null)
		{
			queuesNode = new QueuesNode();
			systemNode.addChildren(queuesNode);
			saveNode(queuesNode);
		}

        schemasNode = (SchemasNode) systemNode.getChildren(SchemasNode.NAME);
        if (schemasNode==null)
        {
            schemasNode = new SchemasNode();
            schemasNode.setParent(systemNode);
            saveNode(schemasNode);
            systemNode.addChildren(schemasNode);
        }

        servicesNode = (ServicesNode) systemNode.getChildren(ServicesNode.NAME);
        if (servicesNode==null)
        {
            servicesNode = new ServicesNode();
            servicesNode.setParent(systemNode);
            saveNode(servicesNode);
            systemNode.addChildren(servicesNode);
        }

        responseServiceNode = (NetworkResponseServiceNode)
                servicesNode.getChildren(NetworkResponseServiceNode.NAME);
        if (responseServiceNode==null)
        {
            responseServiceNode = new NetworkResponseServiceNode();
            responseServiceNode.setParent(servicesNode);
            saveNode(responseServiceNode);
            servicesNode.addChildren(responseServiceNode);
        }
        
        nodeLoggerNode = (NodeLoggerNode) servicesNode.getChildren(NodeLoggerNode.NAME);
        if (nodeLoggerNode==null)
        {
            nodeLoggerNode = new NodeLoggerNode();
            nodeLoggerNode.setParent(servicesNode);
            saveNode(nodeLoggerNode);
            servicesNode.addChildren(nodeLoggerNode);
        }

        auditorNode = (AuditorNode) servicesNode.getChildren(AuditorNode.NAME);
        if (auditorNode==null)
        {
        	auditorNode = new AuditorNode();
        	auditorNode.setParent(servicesNode);
            saveNode(auditorNode);
            servicesNode.addChildren(auditorNode);
        }
        
    }

    private void createTempatesSubtree()
    {
        templatesNode = (TemplatesNode) rootNode.getChildren(TemplatesNode.NAME);
        if (templatesNode==null)
        {
            templatesNode = new TemplatesNode();
            rootNode.addChildren(templatesNode);
            saveNode(templatesNode);
        }
    }

    private void initNode(Node node, NodeTuner nodeTuner)
    {
        if (logger.isDebugEnabled())
            logger.debug(String.format("Initializing node (%s)", node.getPath()));
        if (!node.isInitializeAfterChildrens())
        {
            if (logger.isDebugEnabled())
                logger.debug("Initializing node "+node.getPath());
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
            if (logger.isDebugEnabled())
                logger.debug("Initializing node "+node.getPath());
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
            saveNode(clone);
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
                {
                    saveNodeAttribute(attr);
                }
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
    
    @SuppressWarnings("unchecked")
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

    public void saveNode(Node node)
    {
        if (node.isDynamic())
        {
            if (node.getId()==0)
                node.setId(dynamicNodeId.decrementAndGet());
        }
        else
            treeStore.saveNode(node);
    }

    public void saveNodeAttribute(NodeAttribute attribute)
    {
        if (attribute.getOwner().isDynamic())
        {
            if (attribute.getId()==0)
                attribute.setId(dynamicAttributeId.decrementAndGet());
        }
        else if (attribute.getOwner().getId()>0)
            treeStore.saveNodeAttribute(attribute);
    }

    public void saveNodeAttributeBinaryData(NodeAttribute attribute, InputStream data)
    {
        if (attribute.getOwner().isDynamic())
            throw new TreeError("Can not save binary data for attribute of the DYNAMIC node");
        else
            treeStore.saveNodeAttributeBinaryData(attribute, data);
    }

    private void fireNodeMovedEvent(Node node)
    {
        Collection<NodeListener> listeners = node.getListeners();
        if (listeners!=null && !listeners.isEmpty())
            for (NodeListener listener: listeners)
                listener.nodeMoved(node);
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
            fireNodeMovedEvent(node);
            return ScanOperation.CONTINUE;
        }
    }

    class CopyBinaryAttrsTuner implements NodeTuner
    {
        public final static String CLONED_FROM_NODE = "CLONED_FROM_NODE";
        
        private final NodeTuner wrappedTuner;

        public CopyBinaryAttrsTuner(NodeTuner wrappedTuner)
        {
            this.wrappedTuner = wrappedTuner;
        }

        public Node cloneNode(Node sourceNode)
        {
            Node res = null;
            if (wrappedTuner!=null)
                res = wrappedTuner.cloneNode(sourceNode);
            return res;
        }

        public void tuneNode(Node sourceNode, Node sourceClone)
        {
            if (wrappedTuner!=null)
                wrappedTuner.tuneNode(sourceNode, sourceClone);
            NodeAttributeImpl attr =
                    new NodeAttributeImpl(CLONED_FROM_NODE, Node.class, sourceNode.getPath(), null);
            try
            {
                attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
                attr.setOwner(sourceClone);
                sourceClone.addNodeAttribute(attr);
            }
            catch (Exception ex)
            {
                throw new NodeError(
                        String.format("Error creating attribute (%s)", CLONED_FROM_NODE), ex);
            }
        }

        public void finishTuning(Node sourceClone)
        {
            NodeAttribute cloneFromAttr = sourceClone.getNodeAttribute(CLONED_FROM_NODE);
            try
            {
                if (sourceClone.getId()>0)
                {
                    Collection<NodeAttribute> attrs = sourceClone.getNodeAttributes();
                    if (attrs!=null && !attrs.isEmpty())
                    {
                        Node sourceNode = cloneFromAttr.getRealValue();
                        for (NodeAttribute attr: attrs)
                        {
                            if (   DataFile.class.isAssignableFrom(attr.getType())
                                && DataFileValueHandlerFactory.TYPE.equals(attr.getValueHandlerType()))
                            {
                                NodeAttribute sourceAttr = sourceNode.getNodeAttribute(attr.getName());
                                DataFile sourceFile = sourceAttr.getRealValue();
                                DataFile clonedFile = (DataFile)attr.getRealValue();
                                try{
                                    clonedFile.setDataStream(sourceFile.getDataStream());
                                }catch(DataFileException e)
                                {
                                    sourceClone.getLogger().error(
                                            String.format(
                                                "Error copy binary data from (%s) to (%s)"
                                                , sourceAttr.getPath(), attr.getPath())
                                            , e);
                                }
                            }
                        }
                    }
                }
            }
            finally
            {
                sourceClone.removeNodeAttribute(CLONED_FROM_NODE);
                treeStore.removeNodeAttribute(cloneFromAttr.getId());
            }
            if (wrappedTuner!=null)
                wrappedTuner.finishTuning(sourceClone);
        }
        
    }
}

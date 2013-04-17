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

import groovy.lang.Closure;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.script.Bindings;
import org.raven.annotations.Parameter;
import org.raven.api.NodeAttributeAccess;
import org.raven.conf.Configurator;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.ExpressionAttributeValueHandler;
import org.raven.log.LogLevel;
import org.raven.log.NodeLogger;
import org.raven.template.impl.TemplateEntry;
import org.raven.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;
import org.weda.beans.ClassDescriptor;
import org.weda.beans.ObjectUtils;
import org.weda.beans.PropertyDescriptor;
import org.weda.converter.TypeConverterException;
import org.weda.internal.annotations.Service;
import org.weda.services.ClassDescriptorRegistry;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class BaseNode implements Node, NodeListener, Logger
{
    public final static String LOGLEVEL_ATTRIBUTE = "logLevel";
    public final static String AUTOSTART_ATTRIBUTE = "autoStart";

    private static Logger sl4jLogger = LoggerFactory.getLogger(Node.class);
    protected Logger logger; // = LoggerFactory.getLogger(Node.class);
    
    @Service
    protected static ClassDescriptorRegistry descriptorRegistry;
    @Service
    protected static TypeConverter converter;
    @Service
    protected static Configurator configurator;
    @Service
    protected static Tree tree;
    @Service
    protected static NodePathResolver pathResolver;
    @Service
    private static NodeLogger nodeLogger;

    @Parameter(defaultValue="WARN")
    private LogLevel logLevel;
    
    private int id;
    
    private String name;
    private byte level = 0;
    private int index = 0;
    
    private Node parent;
    
    private Map<String, Node> nodes;
    private Map<String, NodeAttribute> nodeAttributes;
    private Map<Node, Node> dependentNodes;
    private Collection<NodeListener>  listeners;
    private Map<NodeAttribute, Set<NodeAttributeListener>> attributesListeners;
    private Lock attributeListenersLock;
    
    private boolean initializeAfterChildrens = false;
    private boolean startAfterChildrens = false;
    private boolean childrensDynamic = false;
    private Status status;
    private Lock statusLock;
    
    private Map<String, NodeParameter> parameters;
    private boolean subtreeListener = false;
    private Map<String, Object> variables;

//    public BaseNode(Class[] childNodeTypes, boolean container, boolean readOnly)
//    {
//        this.childNodeTypes = childNodeTypes;
//        this.container = container;
//        this.readOnly = readOnly;
//    }
    public BaseNode()
    {
        initFields();
    }
    
    public BaseNode(String name)
    {
        this();
        this.name = name;
    }

    protected boolean includeLogLevel()
    {
        return true;
    }
    
    protected void initFields()
    {
    	logger = this;
        parent = null;
        dependentNodes = new ConcurrentHashMap<Node, Node>();
        listeners = new CopyOnWriteArraySet<NodeListener>();
        attributesListeners = new ConcurrentHashMap<NodeAttribute, Set<NodeAttributeListener>>();
        attributeListenersLock = new ReentrantLock();
        status = Status.CREATED;
        statusLock = new ReentrantLock();
        parameters = null;
        nodeAttributes = new ConcurrentHashMap<String, NodeAttribute>();
        nodes = new ConcurrentHashMap<String, Node>();
        variables = new ConcurrentHashMap<String, Object>();
        index = 0;
    }

    public LogLevel getLogLevel()
    {
    	if(getParent()==null || !includeLogLevel())
    		return LogLevel.NONE;
    	if(getStatus()!= Status.INITIALIZED && getStatus()!= Status.STARTED)
    		return LogLevel.NONE;
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel)
    {
        this.logLevel = logLevel;
    }

    public Logger getLogger()
    {
        return logger;
    }

    public int getId() 
    {
        return id;
    }

    public String generateBindingSupportId()
    {
        return this.getClass().getName()+id;
    }

//    private void initDependentNodes()
//    {
//        if (dependentNodes != null)
//            for (Node node : dependentNodes)
//                initDependentNode(node);
//            
//        if (attributesListeners != null)
//            for (Set<NodeAttributeListener> listeners : attributesListeners.values())
//                for (NodeAttributeListener listener : listeners)
//                    if (listener instanceof NodeAttribute)
//                        initDependentNode(((NodeAttribute) listener).getOwner());
//    }
//
//    private void initDependentNode(Node node)
//    {
//        if (node.getStatus()==Status.CREATED)
//        {
//            node.init();
//            if (node.isAutoStart())
//                node.start();
//        }
//    }
    
    private void processListeners(NodeAttributeImpl attr, Object newValue, Object oldValue)
    {
        this.nodeAttributeValueChanged(attr.getOwner(), attr, oldValue, newValue);
        
        if (listeners != null)
        {
            for (NodeListener listener : listeners)
            {
                listener.nodeAttributeValueChanged(this, attr, oldValue, newValue);
            }
        }
        if (attributesListeners != null)
        {
            Set<NodeAttributeListener> listenersSet = attributesListeners.get(attr);
            if (listenersSet != null)
            {
                attributeListenersLock.lock();
                try{
                    for (NodeAttributeListener listener : listenersSet)
                    {
                        listener.nodeAttributeValueChanged(this, attr, oldValue, newValue);
                    }
                }finally{
                    attributeListenersLock.unlock();
                }
            }
        }
    }
    
//    private Node getTemplateVariablesNo

    private void processNodeAttributeDependency(NodeAttributeImpl attr, String oldValue)
    {
//        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void processNodeDependency(
            NodeAttributeImpl attr, Object newValue, Object oldValue) throws TypeConverterException
    {
        if (Node.class.isAssignableFrom(attr.getType()))
        {
            if (oldValue != null)
            {
//                Node oldRef = (Node) converter.convert(attr.getType(), oldValue, null);
                ((Node)oldValue).removeDependentNode(this);
            }
            if (newValue != null)
            {
                Node node = (Node) converter.convert(attr.getType(), newValue, null);
                ((Node)newValue).addDependentNode(this);
            }
        }
    }

    public void setId(int id) 
    {
        this.id = id;
    }
    
    @Deprecated
    public List<Node> getChildrenList() { 
        return getNodes(); 
    }

    public boolean isTemplate()
    {
        Node pNode = this;
        while ( (pNode=pNode.getParent())!=null )
            if (pNode instanceof TemplateEntry)
                return true;
        return false;
    }

    public Node getTemplate() 
    {
        Node pNode = this;
        while ( (pNode=pNode.getParent())!=null )
            if (pNode instanceof TemplateEntry)
                return pNode.getParent();
        return null;
    }

    public void setStatus(Status status)
    {
        statusLock.lock();
        try{
            if (this.status != status)
            {
                Status oldStatus = this.status;
                this.status = status;

                fireStatusChanged(oldStatus, status);
            }
        }finally{
            statusLock.unlock();
        }
    }

    public Status getStatus() {
        statusLock.lock();
        try{
            return status;
        }finally{
            statusLock.unlock();
        }
    }

    public boolean isStarted() {
        return Status.STARTED.equals(getStatus());
    }

    public boolean isInitialized() {
        return Status.INITIALIZED.equals(getStatus());
    }

    public void addListener(NodeListener listener)
    {
        if (listeners.contains(listener))
            return;
        listeners.add(listener);
        if (listener.isSubtreeListener())
        {
            Collection<Node> childs = getChildrens();
            if (childs!=null)
                for (Node children: childs)
                    children.addListener(listener);
        }
    }
    
    public void removeListener(NodeListener listener)
    {
        Collection<Node> childs = nodes.values();
        if (listeners.remove(listener) && listener.isSubtreeListener() && childs!=null)
            for (Node children: childs)
                children.removeListener(listener);
    }
    
    public Collection<NodeListener> getListeners()
    {
        return listeners;
    }

    public void addChildren(Node node)
    {
        if (nodes.containsKey(node.getName()))
            throw new NodeError(String.format(
                    "Node (%s) already contains children node with name (%s)"
                    , getPath(), node.getName()));
        node.setParent(this);
        int nodeIndex = node.getIndex();
        int newIndex = checkNodeIndex(nodeIndex);
        if (nodeIndex!=newIndex)
        {
            node.setIndex(newIndex);
            if (node.getId()>0)
                node.save();
        }
        
        nodes.put(node.getName(), node);
        node.addListener(this);
        
        if (listeners!=null)
            for (NodeListener listener: listeners)
                if (listener.isSubtreeListener())
                    node.addListener(listener);
        fireChildrenAdded(node);
    }

    private int checkNodeIndex(int index)
    {
        int maxIndex = 0;
        boolean hasIndex = false;
        for (Node child: nodes.values())
        {
            if (maxIndex<child.getIndex())
                maxIndex = child.getIndex();
            if (index==child.getIndex())
                hasIndex = true;
        }
        if (hasIndex || index==0)
            return maxIndex+1;
        else
            return index;
    }

    public void addAndSaveChildren(Node node)
    {
        if (nodes.containsKey(node.getName()))
            throw new NodeError(String.format(
                    "Node (%s) already contains children node with name (%s)"
                    , getPath(), node.getName()));
        node.setParent(this);
        node.save();
        addChildren(node);
        node.init();
    }

    public void detachChildren(Node node)
    {
        if (nodes!=null)
        {
            Node removedNode = nodes.remove(node.getName());
            if (removedNode!=null)
                removedNode.removeListener(this);
        }
    }

    public void removeChildren(Node node)
    {
        if (logger.isDebugEnabled())
            logger.debug(String.format(
                    "Removing children node (%s) from (%s) node", node.getPath(), getPath()));
        
        if (nodes!=null)
        {
            Node removedNode = nodes.remove(node.getName());
            if (removedNode!=null)
            {
                removedNode.removeListener(this);
                removedNode.remove();
            }
        }
    }
    
    public boolean addDependentNode(Node dependentNode)
    {
        if (dependentNodes.containsKey(dependentNode))
            return false;
        else
        {
            dependentNodes.put(dependentNode, dependentNode);
            fireDependentNodeAdded(dependentNode);
            return true;
        }
    }

    public void addNodeAttributeDependency(
            String attributeName, NodeAttributeListener listener)
    {
        NodeAttribute attr = getAttr(attributeName);
        if (attr==null)
            throw new NodeError(String.format(
                    "Attribute (%s) not found in the node (%s)", attributeName, getPath()));
        
        attributeListenersLock.lock();
        try{
            Set<NodeAttributeListener> listeners = attributesListeners.get(attr);
            if (listeners==null)
            {
                listeners = new HashSet<NodeAttributeListener>();
                attributesListeners.put(attr, listeners);
            }

            listeners.add(listener);
        }finally
        {
            attributeListenersLock.unlock();
        }
    }

    public boolean removeDependentNode(Node dependentNode)
    {
        if (dependentNodes.containsKey(dependentNode))
        {
            dependentNodes.remove(dependentNode);
            return true;
        }else
            return false;
    }

    @Deprecated
    public void addNodeAttribute(NodeAttribute attr) {
        addAttr(attr);
    }

    public void addAttr(NodeAttribute attr) {
        nodeAttributes.put(attr.getName(), attr);
    }

    public NodeAttribute addUniqAttr(String protoAttrName, Object value) throws Exception {
        return addUniqAttr(protoAttrName, value, false);
    }
    
    public NodeAttribute addUniqAttr(String protoAttrName, Object value, boolean reuseAttrWithNullValue) 
            throws Exception 
    {
        NodeAttribute protoAttr = getAttr(protoAttrName);
        if (protoAttr==null) throw new Exception(String.format(
                "Not found prototype attribute (%s) for node (%s)", protoAttrName, this));
        NodeAttribute nullValAttr = null;
        for (NodeAttribute attr: getAttrs())
            if (attr.getName().startsWith(protoAttrName)) {
                if (ObjectUtils.equals(value, attr.getRealValue())) return attr;
                else if (reuseAttrWithNullValue && nullValAttr==null && attr.getRealValue()==null)
                    nullValAttr = attr;
            }
        if (nullValAttr!=null) {
            nullValAttr.setValue(converter.convert(String.class, value, null));
            nullValAttr.save();
            return nullValAttr;
        } else
            return tryAddAttr(protoAttr, value, 1);
    }
    
    private NodeAttribute tryAddAttr(NodeAttribute baseAttr, Object val, int index) throws Exception {
        NodeAttribute attr = getAttr(baseAttr.getName()+index);
        if (attr!=null) return tryAddAttr(baseAttr, val, index+1);
        else {
            attr = new NodeAttributeImpl(baseAttr.getName()+index, baseAttr.getType(), null, null);
            attr.setOwner(this);
            attr.setValueHandlerType(baseAttr.getValueHandlerType());
            addAttr(attr);
            attr.init();
            attr.setValue(converter.convert(String.class, val, null));
            attr.save();
            return attr;
        }
    }
    
    @Deprecated
    public void removeNodeAttribute(String name) {
        NodeAttribute attr = nodeAttributes.remove(name);
        if (attr!=null)
            fireNodeAttributeRemoved(attr);
    }

    public void removeAttr(String name) {
        NodeAttribute attr = nodeAttributes.remove(name);
        if (attr!=null)
            fireNodeAttributeRemoved(attr);
    }

    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        if (ObjectUtils.equals(this.name, name))
            return;
        if (parent!=null && parent.getChildren(name)!=null)
            throw new NodeError(String.format(
                    "Node (%s) already has children with name (%s)", parent.getPath(), name));

        String oldName = this.name;
        this.name = name;
        if (   ObjectUtils.in(getStatus(), Status.INITIALIZED, Status.STARTED) 
            && !ObjectUtils.equals(oldName, name))
        {
            fireNameChanged(oldName, name);
        }
    }

    public byte getLevel()
    {
        level = 0; Node node = this;
        while ( (node=node.getParent())!=null )
            ++level;
        return level;
    }

    public int getIndex()
    {
        return index;
    }


    public void setIndex(int index)
    {
        int oldIndex =this.index;
        this.index = index;
//        if (oldIndex!=index)
//            fireNodeIndexChanged(oldIndex, index);
    }
    
//    public boolean isReadOnly()
//    {
//        return readOnly;
//    }
//    
//    public boolean isContainer()
//    {
//        return container;
//    }

    public List<Class> getChildNodeTypes() {
        return tree.getChildNodesTypes(this);
    }

    public List<Node> getNodes() {
        Collection<Node> childs = nodes.values();
        if (childs.isEmpty())
            return Collections.EMPTY_LIST;
        List<Node> sortedChildrens = new ArrayList<Node>(childs);
        Collections.sort(sortedChildrens);
        return sortedChildrens;
    }

    public Node find(final Closure<Boolean> filter) {
        List<Node> res = tree.search(this, new SearchOptionsImpl().setFindFirst(true), new SearchFilter() {
            public boolean filter(Node node) {
                return filter.call(node);
            }
        });
        return res.isEmpty()? null : res.get(0);
    }

    public List<Node> findAll(final Closure<Boolean> filter) {
        List<Node> res = tree.search(this, new SearchOptionsImpl().setFindFirst(false), new SearchFilter() {
            public boolean filter(Node node) {
                return filter.call(node);
            }
        });
        return res;
    }

    public int getNodesCount() {
        return nodes.size();
    }
    
    @Deprecated
    public int getChildrenCount() {
        return getNodesCount();
    }
    
    @Deprecated
    public Collection<Node> getChildrens() {
        return nodes.values();
//        return getNodes();
//        return nodes==null? Collections.EMPTY_LIST : nodes.values();
    }
    
    @Deprecated
    public List<Node> getSortedChildrens() {
        return getNodes();
    }

    public boolean isConditionalNode() {
        return false;
    }

    public Node getEffectiveParent() 
    {
        Node node = getParent();
        while (node.isConditionalNode())
            node = node.getParent();
        return node;
    }

    @Deprecated
    public Collection<Node> getEffectiveChildrens() {
        return getEffectiveNodes();
    }
    
    public Collection<Node> getEffectiveNodes() {
        List<Node> sortedChildrens = getNodes();
        Map<Node, Collection<Node>> effChilds = new HashMap<Node, Collection<Node>>();
        //calculating effective childrens in right direction
        for (Node child: sortedChildrens)
            if (child.isConditionalNode())
                effChilds.put(child, child.getEffectiveChildrens());
        //composing sortedChildrens in reverse direction
        for (int i=sortedChildrens.size()-1; i>=0; --i) {
            Node node = sortedChildrens.get(i);
            if (node.isConditionalNode()) {
                sortedChildrens.remove(i);
                Collection<Node> list = effChilds.get(node);
                if (list!=null)
                    sortedChildrens.addAll(i, list);
            }
        }
        return sortedChildrens.isEmpty()? null : sortedChildrens;
    }

    @Deprecated
    public Node getChildren(String name) {
        return nodes==null? null : nodes.get(name);
    }

    public Node getNode(String name) {
        return nodes.get(name);
    }

    public boolean hasNode(String name) {
        return nodes.containsKey(name);
    }

    @Deprecated
    public Node getChildrenByPath(String path) {
        return getNodeByPath(path);
    }

    public Node getNodeByPath(String path) {
        try {
            PathInfo<Node> pathInfo = pathResolver.resolvePath(path, this);
            return pathInfo.getReferencedObject();
        } catch (Exception e) {
            return null;
        }
    }

    @Deprecated
    public Collection<NodeAttribute> getNodeAttributes() {
        return getAttrs();
    }

    public Collection<NodeAttribute> getAttrs() {
        return nodeAttributes.values();
    }

    public boolean hasAttr(String name) {
        return nodeAttributes.containsKey(name);
    }

    @Deprecated
    public NodeAttribute getNodeAttribute(String name) {
        return getAttr(name);
    }

    public NodeAttribute getAttr(String name) {
        return nodeAttributes.get(name);
    }

    public Set<Node> getDependentNodes() {
        return dependentNodes.keySet();
    }

    public Node getParent()
    {
        return parent;
    }

    public void setParent(Node parent)
    {
        this.parent = parent;
//        byte oldLevel = level;
//        level = 0; Node node = this;
//        while ( (node=node.getParent())!=null )
//            ++level;
//        
//        if (level!=oldLevel && childrens!=null)
//            for (Node child: childrens.values())
//                child.setParent(this);
    }

    public String getPath()
    {
        return pathResolver.getAbsolutePath(this);
    }

    public boolean isInitializeAfterChildrens()
    {
        return initializeAfterChildrens;
    }

    public void setInitializeAfterChildrens(boolean initializeAfterChildrens)
    {
        this.initializeAfterChildrens = initializeAfterChildrens;
    }

    public boolean isStartAfterChildrens()
    {
        return startAfterChildrens;
    }

    public void setStartAfterChildrens(boolean startAfterChildrens)
    {
        this.startAfterChildrens = startAfterChildrens;
    }


    public boolean isDynamic() 
    {
        return parent==null? false : parent.isChildrensDynamic();
    }

    public void setChildrensDynamic(boolean childrensDynamic)
    {
        this.childrensDynamic = childrensDynamic;
    }

    public boolean isChildrensDynamic()
    {
        boolean dynamic = childrensDynamic;
        Node nodeParent = parent;
        while (!dynamic && nodeParent!=null)
        {
            dynamic = nodeParent.isChildrensDynamic();
            nodeParent = nodeParent.getParent();
        }
        return dynamic;
    }

    public boolean isAutoStart()
    {
        if (ObjectUtils.in(getStatus(), Status.INITIALIZED, Status.STARTED))
        {
            NodeAttribute autoStartAttr = getNodeAttribute(AUTOSTART_ATTRIBUTE);
            if (autoStartAttr!=null && Boolean.class.equals(autoStartAttr.getType()))
            {
                Boolean autoStart = autoStartAttr.getRealValue();
                return autoStart==null? false : autoStart;
            }
        }
        
        return true;
    }

    public boolean isContainer()
    {
        return getChildNodeTypes()!=null;
    }
    
    public void init() throws NodeError
    {
        try
        {
            if (nodeAttributes!=null)
                for (NodeAttribute attr: nodeAttributes.values())
                    attr.init();
//            boolean dependenciesInitialized = true;
//            if (nodeAttributes != null)
//            {
//                for (NodeAttribute attr : nodeAttributes.values())
//                {
//                    Node node = null;
//                    if (Node.class.isAssignableFrom(attr.getType()) && attr.getValue()!=null)
//                    {
//                        node = converter.convert(Node.class, attr.getValue(), null);
//                        node.addDependentNode(this);
//                        if (node.getStatus()==Status.CREATED)
//                        {
//                            dependenciesInitialized = false;
//                        } 
//                    }
//                    if (   AttributeReference.class.isAssignableFrom(attr.getType()) 
//                              && attr.getAttributeReference()!=null)
//                    {
//                        if (   attr.getAttributeReference().getAttribute().getOwner().getStatus()
//                            == Status.CREATED)
//                        {
//                            dependenciesInitialized = false;
//                        }
//                    }
//                }
//            }
//            
//            if (!dependenciesInitialized)
//            {
//                return;
//            }
            extractNodeParameters();
            syncAttributesAndParameters();
//            syncAttributesGenerators();
            doInit();
            
            setStatus(Status.INITIALIZED);
            
//            initDependentNodes();
                                
        } catch (Exception e)
        {
            throw new NodeError(
                    String.format("Node (%s) initialization error", getPath())
                    , e);
        }
    }
    
    protected void doInit() throws Exception
    {
    }
    
    protected void doStart() throws Exception 
    {
    }

    public boolean start() throws NodeError
    {
        try
        {
            if (Status.STARTED.equals(getStatus()))
                return true;
            if (isTemplate())
                return false;
            if (nodeAttributes!=null) {
                //TODO: Не работает :(
                for (NodeAttribute attr: nodeAttributes.values())
                    if (attr.isGeneratorType())
                        syncParentAttributes(attr);
                for (NodeAttribute attr: nodeAttributes.values())
                    if ( (attr.isRequired() && attr.getValue()==null)
                       && (   !(attr.getValueHandler() instanceof ExpressionAttributeValueHandler)
                           || !attr.isExpressionValid() )
                       && !ObjectUtils.in(attr.getValueHandlerType()
                            , ActionAttributeValueHandlerFactory.TYPE
                            , RefreshAttributeValueHandlerFactory.TYPE))
                    {
                        error(String.format(
                                "Error switching node (%s) to the STARTED state. " +
                                "Value for required attribute (%s) not seted "
                                , getPath(), attr.getName()));
                        return false;
                    }
                
            }
            doStart();
            setStatus(Status.STARTED);
        }
        catch (Exception e)
        {
            error(String.format("Error starting node (%s)", getPath()), e);
            return false;
        }
        return true;
    }

    public synchronized void stop() throws NodeError
    {
        try
        {
            variables.clear();
            doStop();
//            if (!Status.REMOVING.equals(getStatus()))
            setStatus(Status.INITIALIZED);
        }
        catch(Exception e)
        {
            error(String.format("Error stoping node (%s)", getPath()), e);
        }
    }

    protected void doStop() throws Exception
    {
    }

    public synchronized void remove() 
    {
//        status = Status.REMOVED;
        setStatus(Status.REMOVED);
        fireNodeRemoved();
    }
    
    public synchronized void shutdown() throws NodeShutdownError
    {
        if (logger.isDebugEnabled())
            logger.debug(String.format("Shutdowning node (%s)", getPath()));
        if (getStatus()==Status.STARTED)
            stop();
        if (nodeAttributes!=null)
            for (NodeAttribute attr: nodeAttributes.values())
                attr.shutdown();
//                if (Node.class.isAssignableFrom(attr.getType()) && attr.getRealValue()!=null)
//                {
//                    Node node = attr.getRealValue();
//                    node.removeDependentNode(this);
//                }
        fireNodeShutdownedEvent(this);
    }

    void fireAttributeValueChanged(NodeAttributeImpl attr, Object oldValue, Object newValue)
    {
        processNodeDependency(attr, newValue, oldValue);
        processAttributeGeneration(attr, newValue);
        processListeners(attr, newValue, oldValue);
    }
    
    void fireAttributeNameChanged(NodeAttribute attr, String oldName, String newName)
    {
        nodeAttributes.remove(oldName);
        nodeAttributes.put(newName, attr);
        
        if (listeners!=null)
            for (NodeListener listener: listeners)
                listener.nodeAttributeNameChanged(attr, oldName, newName);
        if (attributesListeners!=null)
        {
            Set<NodeAttributeListener> listeners = attributesListeners.get(attr);
            if (listeners!=null)
                for (NodeAttributeListener listener: listeners)
                    listener.nodeAttributeNameChanged(attr, oldName, newName);
        }
    }
    
    private void fireChildrenAdded(Node children)
    {
        if (listeners!=null)
            for (NodeListener listener: listeners)
                listener.childrenAdded(this, children);
    }
    
    private void fireDependentNodeAdded(Node dependentNode) 
    {
        if (listeners!=null)
            for (NodeListener listener: listeners)
                listener.dependendNodeAdded(this, dependentNode);
    }


    private void fireNodeRemoved()
    {
        if (listeners!=null)
            for (NodeListener listener: listeners)
                listener.nodeRemoved(this);
    }

    private void fireNodeIndexChanged(int oldIndex, int newIndex)
    {
        if (listeners!=null)
            for (NodeListener listener: listeners)
                listener.nodeIndexChanged(this, oldIndex, newIndex);
    }

    @Deprecated
    public NodeAttribute getParentAttribute(String attributeName) {
        return getParentAttr(attributeName);
    }

    public NodeAttribute getParentAttr(String attributeName) {
        Node node = this;
        while ( (node=node.getParent())!=null ) {
            NodeAttribute attr = node.getAttr(attributeName);
            if (attr!=null)
                return attr;
        }
        return null;
    }

    @Deprecated
    public String getParentAttributeValue(String attributeName) {
        return getParentAttrValue(attributeName);
    }

    public String getParentAttrValue(String attributeName) {
        Node node = this;
        while ( (node=node.getParent())!=null ) {
            NodeAttribute attr = node.getAttr(attributeName);
            if (attr!=null)
                return attr.getValue();
        }
        return null;
    }

    @Deprecated
    public <T> T getParentAttributeRealValue(String attributeName) {
        Node node = this;
        while ( (node=node.getParent())!=null )
        {
            NodeAttribute attr = node.getNodeAttribute(attributeName);
            if (attr!=null)
                return (T) attr.getRealValue();
        }
        return null;
    }
    
    public <T> T getParentAttrRealValue(String attributeName) {
        Node node = this;
        while ( (node=node.getParent())!=null ) {
            NodeAttribute attr = node.getAttr(attributeName);
            if (attr!=null)
                return (T) attr.getRealValue();
        }
        return null;
    }

    public void save()
    {
        tree.saveNode(this);
    }

    private void extractNodeParameters()
    {
        if (parameters!=null)
            parameters = null;
        ClassDescriptor classDescriptor = descriptorRegistry.getClassDescriptor(getClass());
        PropertyDescriptor[] descs = classDescriptor.getPropertyDescriptors();
        for (PropertyDescriptor desc: descs)
            if (desc.isReadable())
                for (Annotation ann: desc.getAnnotations())
                    if (ann instanceof Parameter)
                    {
                        NodeParameter param = new NodeParameterImpl((Parameter) ann, this, desc);
                        
                        if (parameters==null)
                            parameters = new HashMap<String, NodeParameter>();
                        
                        parameters.put(desc.getName(), param);
                        
                        break;
                    }
    }

    private void fireNameChanged(String oldName, String name)
    {
        if (listeners!=null)
            for (NodeListener listener: listeners)
                listener.nodeNameChanged(this, oldName, name);
    }

    private void fireNodeAttributeRemoved(NodeAttribute attr)
    {
        if (getStatus()==Status.CREATED)
            return;
        try
        {
            if (listeners!=null)
            {
                List<NodeListener> listenersToRemove = 
                        new ArrayList<NodeListener>(listeners.size());
                for (NodeListener listener: listeners)
                {
                    boolean removeListener = listener.nodeAttributeRemoved(this, attr);
                    if (removeListener)
                        listenersToRemove.add(listener);
                }
                if (listenersToRemove.size()>0)
                    listeners.removeAll(listenersToRemove);
            }
            if (attributesListeners!=null)
            {
                Set<NodeAttributeListener> listenersSet = attributesListeners.get(attr);
                if (listenersSet!=null)
                    for (Iterator<NodeAttributeListener> it=listenersSet.iterator(); it.hasNext();)
                    {
                        boolean removeListener = it.next().nodeAttributeRemoved(this, attr);
                        if (removeListener)
                            it.remove();
                    }
            }
        } catch (Error e)
        {
            nodeAttributes.put(attr.getName(), attr);
            throw e;
        }
    }

    private void fireStatusChanged(Status oldStatus, Status status)
    {
        if (listeners!=null)
            for (NodeListener listener: listeners)
                listener.nodeStatusChanged(this, oldStatus, status);
    }

    private void fireNodeShutdownedEvent(BaseNode aThis)
    {
        if (listeners!=null)
            for (NodeListener listener: listeners)
                listener.nodeShutdowned(this);
    }

    private void processAttributeGeneration(NodeAttributeImpl attr, Object newValue) 
    {
        try
        {
            if (attr.isGeneratorType())
            {
                syncParentAttributes(attr);
            }
        } catch (Exception e)
        {
            logger.error(String.format(
                    "Error generating child attributes for (%s) attribute", attr.getName()), e);
        }
    }

    protected boolean allowAttributesGeneration(NodeAttribute attr)
    {
        return true;
    }

    private void syncAttributesGenerators() throws Exception
    {
        if (nodeAttributes==null)
            return;
        
        List<NodeAttribute> attrs = new ArrayList<NodeAttribute>(nodeAttributes.values());
        for (NodeAttribute attr: attrs)
            if (attr.isGeneratorType())
                syncParentAttributes(attr);
    }
    
    private void syncParentAttributes(NodeAttribute parent) throws Exception
    {
        if (parent.getRealValue() == null || !allowAttributesGeneration(parent))
            removeChildAttributes(parent.getName(), null);
        else  {
            AttributesGenerator attributesGenerator = (AttributesGenerator)parent.getRealValue();
            Collection<NodeAttribute> newAttrs = attributesGenerator.generateAttributes();
            removeChildAttributes(parent.getName(), newAttrs);

            if (newAttrs != null) {
                for (NodeAttribute newAttr : newAttrs) {

                    if (nodeAttributes.containsKey(newAttr.getName()))
                        continue;

                    NodeAttribute clone = null;
                    try {
                        clone = (NodeAttribute) newAttr.clone();
                    } catch (CloneNotSupportedException ex) {
                        throw new NodeError(String.format(
                                "Error in the node (%s). Attribute (%s) clone error"
                                , getPath(), clone.getName()), ex);
                    }
                    clone.setOwner(this);
                    clone.setParentAttribute(parent.getName());

                    addNodeAttribute(clone);
                    try {
                        clone.init();
                        clone.save();
                    }  catch (Exception e) {
                        logger.error(String.format(
                                "Error initializing the attribute (%s) of the node (%s)"
                                , clone.getName(), getPath()), e);
                    }
                }
            }
        }
    }

    private void syncAttributesAndParameters() throws Exception
    {
        if (nodeAttributes!=null)
        {
            Iterator<Map.Entry<String, NodeAttribute>> it = nodeAttributes.entrySet().iterator();
            while (it.hasNext())
            {
                NodeAttributeImpl attr = (NodeAttributeImpl) it.next().getValue();
                if (attr.getParameterName()!=null)
                {
                    NodeParameter param = 
                            parameters==null? null : parameters.get(attr.getParameterName());
                    if (param==null)
                        it.remove();
                    else
                    {
                        param.setNodeAttribute(attr);
                        attr.setParameter(param);
//                        if (!attr.isAttributeReference())
//                            param.setValue(attr.getValue());
                    }
                }
            }
        }
        if (parameters != null)
        {
            for (NodeParameter param: parameters.values())
                if (param.getNodeAttribute()==null)
                    createNodeAttribute(param);
        }
    }
    
    private void createNodeAttribute(NodeParameter param) 
        throws Exception
    {
        if (LOGLEVEL_ATTRIBUTE.equals(param.getName()) && (getParent()==null || !includeLogLevel()))
            return;
        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setOwner(this);
        attr.setName(param.getDisplayName());
        attr.setParameterName(param.getName());
        attr.setParameter(param);
        attr.setDescription(param.getDescription());
        attr.setRawValue(param.getDefaultValue());
        attr.setType(param.getType());
        attr.setValueHandlerType(param.getValueHandlerType());
        tree.saveNodeAttribute(attr);
        attr.init();
        
        addNodeAttribute(attr);
        
    }
    
    void removeChildAttributes(String parentName, Collection<NodeAttribute> leaveAttributes)
    {
        Iterator<Map.Entry<String, NodeAttribute>> it = nodeAttributes.entrySet().iterator();
        while (it.hasNext()) {
            NodeAttribute childAttr = it.next().getValue();
            if (parentName.equals(childAttr.getParentAttribute())) {
                boolean removeAttr = true;
                if (leaveAttributes!=null) {
                    for (NodeAttribute attr: leaveAttributes)
                        if (   childAttr.getName().equals(attr.getName()) 
                            && (  attr.getType().isAssignableFrom(childAttr.getType())))
//                                || (   childAttr.getAttributeReference()!=null 
//                                    && attr.getType().isAssignableFrom(
//                                       childAttr.getAttributeReference().getAttribute().getType())))
                        {
                            removeAttr = false;
                            break;
                        }
                }
                if (removeAttr) {
                    configurator.getTreeStore().removeNodeAttribute(childAttr.getId());
                    it.remove();
                }
            }
        }
        
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || getClass() != obj.getClass())
            return false;
        final BaseNode other = (BaseNode) obj;
        if (this.id != other.id)
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 13 * hash + (int) (this.id ^ (this.id >>> 32));
        return hash;
    }

    //groovy support methods
    public Object propertyMissing(String name, Object value) {
        if (name.startsWith("$")) {
            NodeAttribute attr = getAttr(name.substring(1));
            if (attr!=null) {
                try {
                    setAttrValue(attr, value);
                    return attr.getRealValue();
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }
            throw new IllegalArgumentException(String.format(
                "Attribute (%s) not found in the node (%s)", name.substring(1), getPath()));
        }
        throw new MissingPropertyException(String.format("Property (%s) not found in (%s)", name, getPath()));
    }
    
    public Object propertyMissing(String name) {
        if (name.startsWith("$")) {
            NodeAttribute attr = getAttr(name.substring(1));
            if (attr!=null)
                return attr.getRealValue();
            throw new IllegalArgumentException(String.format(
                "Attribute (%s) not found in the node (%s)", name.substring(1), getPath()));
        }
        throw new MissingPropertyException(String.format("Property (%s) not found in (%s)", name, getPath()));
    }
    
    public Object methodMissing(String name, Object args) {
        Object[] list = (Object[]) args;
        if (name.startsWith("$") && list.length==1 && list[0] instanceof Map) {
            NodeAttribute attr = getAttr(name.substring(1));
            if (attr!=null) 
                return getAttrValue(attr, (Map)list[0]);
            throw new IllegalArgumentException(String.format(
                "Attribute (%s) not found in the node (%s)", name.substring(1), getPath()));
        } 
        throw new MissingMethodException(name, getClass(), list);
    }
    
    public NodeAttribute getAt(String attrName) {
        return getAttr(attrName);
    }
    //end of groovy support
    
    private void setAttrValue(NodeAttribute attr, Object value) throws Exception {
        String strValue = converter.convert(String.class, value, null);
        attr.setValue(strValue);
        tree.saveNodeAttribute(attr);
    }
  
    private Object getAttrValue(NodeAttribute attr, Map args) {
        BindingSupport varsSupport = tree.getGlobalBindings(Tree.EXPRESSION_VARS_BINDINGS);
        boolean initiated = varsSupport.contains(
                ExpressionAttributeValueHandler.RAVEN_EXPRESSION_VARS_INITIATED_BINDING);
        try {
            varsSupport.put(ExpressionAttributeValueHandler.RAVEN_EXPRESSION_ARGS_BINDING, args);
            return attr.getRealValue();
        } finally {
            if (!initiated)
                varsSupport.reset();
        }
    }
    public void setSubtreeListener(boolean subtreeListener)
    {
        this.subtreeListener = subtreeListener;
    }

    public boolean isSubtreeListener()
    {
        return subtreeListener;
    }

    public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus)
    {
    }

    public void nodeShutdowned(Node node)
    {
    }

    public void nodeNameChanged(Node node, String oldName, String newName)
    {
        if (   nodes!=null 
            && nodes.containsKey(oldName) 
            && node.equals(nodes.get(oldName)))
        {
            nodes.remove(oldName);
            nodes.put(newName, node);
        }
    }

    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue)
    {
    }

    public boolean nodeAttributeRemoved(Node node, NodeAttribute attribute)
    {
        return false;
    }

    public void removeNodeAttributeDependency(String attributeName, NodeAttributeListener listener)
    {
    }

    public void nodeAttributeNameChanged(NodeAttribute attribute, String oldName, String newName)
    {
    }

    public void childrenAdded(Node owner, Node children)
    {
    }

    public void dependendNodeAdded(Node node, Node dependentNode) {
    }

    public void nodeMoved(Node node)
    {
    }
    
    public void nodeRemoved(Node removedNode)
    {
    }

    public void nodeIndexChanged(Node node, int oldIndex, int newIndex) {
    }

    public int compareTo(Node o)
    {
        return index-o.getIndex();
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        BaseNode clone = (BaseNode) super.clone();
        clone.setId(0);
        clone.initFields();
        
        if (nodeAttributes!=null)
            for (NodeAttribute attr: nodeAttributes.values()) 
            {
                NodeAttribute attrClone = (NodeAttribute) attr.clone();
                attrClone.setOwner(clone);
                clone.addNodeAttribute(attrClone);
//                attrClone.init();
            }
//            
//        if (childrens!=null)
//            for (Node child: childrens.values())
//            {
//                Node childClone = (Node) child.clone();
//                clone.addChildren(childClone);
//            }
        
        return clone;
    }
    
    public Node cloneTo(
            Node dest, String newNodeName, NodeTuner nodeTuner, boolean useEffectiveChildrens) 
        throws CloneNotSupportedException
    {
        Node clone = null;
        
        if (nodeTuner!=null)
            clone = nodeTuner.cloneNode(this);
        
        if (clone==null)
                clone = (Node) clone();
        
        if (newNodeName!=null)
            clone.setName(newNodeName);
        
        if (nodeTuner!=null)
            nodeTuner.tuneNode(this, clone);
        
        dest.addChildren(clone);
        
        Collection<Node> childs = 
                useEffectiveChildrens? getEffectiveChildrens() : getSortedChildrens();
        if (childs!=null)
            for (Node child: childs)
                child.cloneTo(clone, null, nodeTuner, useEffectiveChildrens);
        
        return clone;
    }

    @Override
    public String toString() {
        return getPath();
    }

    public void formExpressionBindings(Bindings bindings) 
    {
        if (parent!=null)
            parent.formExpressionBindings(bindings);
    }

    private static final String[] ex = {"org.apache.myfaces", "javax.faces"};

    public static void getTraceX(StringBuffer sb, Throwable t) {
        boolean stop = false;
        sb.append(t.getClass().getCanonicalName());
        sb.append(": ").append(t.getMessage()).append("\n");
        StackTraceElement[] stea = t.getStackTrace();
        for (StackTraceElement ste : stea) {
            String x = ste.toString();
            sb.append("at ").append(x).append("\n");
            for (String s : ex) {
                if (x.startsWith(s)) {
                    stop = true;
                    sb.append("...\n");
                    break;
                }
            }
            if (stop) 
                break;
        }
    }

	public static String getTrace(Throwable t)
	{
		StringBuffer sb = new StringBuffer("");
		getTraceX(sb, t);
		Throwable z = t;
		while( (z = z.getCause())!=null )
		{
			sb.append("Caused by: ");
			getTraceX(sb, z);
		}	
		return sb.toString();
	}	
	
        public boolean isLogLevelEnabled(LogLevel level) {
		return getLogLevel().ordinal() <= level.ordinal();
	}

	public void debug(String arg0) {
		sl4jLogger.debug(arg0);
		if(getLogLevel().ordinal() <= LogLevel.DEBUG.ordinal())
			nodeLogger.write(this, LogLevel.DEBUG, arg0);
	}

	public void debug(String arg0, Object arg1) {
		sl4jLogger.debug(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.DEBUG.ordinal())
		{
			String x = MessageFormatter.format(arg0,arg1);
			nodeLogger.write(this, LogLevel.DEBUG, x);
		}	
	}

	public void debug(String arg0, Object[] arg1) {
		sl4jLogger.debug(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.DEBUG.ordinal())
		{
			String x = MessageFormatter.arrayFormat(arg0, arg1);
			nodeLogger.write(this, LogLevel.DEBUG, x);
		}	
	}

	public void debug(String arg0, Throwable arg1) 
	{
		sl4jLogger.debug(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.DEBUG.ordinal())
			nodeLogger.write(this, LogLevel.DEBUG, arg0 +" : "+ getTrace(arg1));
	}

	public void debug(Marker arg0, String arg1) {
		sl4jLogger.debug(arg0,arg1);
	}

	public void debug(String arg0, Object arg1, Object arg2) {
		sl4jLogger.debug(arg0,arg1,arg2);
		if(getLogLevel().ordinal() <= LogLevel.DEBUG.ordinal())
		{
			String x = MessageFormatter.format(arg0, arg1, arg2);
			nodeLogger.write(this, LogLevel.DEBUG, x);
		}	
	}

	public void debug(Marker arg0, String arg1, Object arg2) {
		sl4jLogger.debug(arg0,arg1,arg2);
	}

	public void debug(Marker arg0, String arg1, Object[] arg2) {
		sl4jLogger.debug(arg0,arg1,arg2);
	}

	public void debug(Marker arg0, String arg1, Throwable arg2) {
		sl4jLogger.debug(arg0,arg1,arg2);
	}

	public void debug(Marker arg0, String arg1, Object arg2, Object arg3) {
		sl4jLogger.debug(arg0,arg1,arg2,arg3);
	}

	public void trace(String arg0) {
		sl4jLogger.trace(arg0);
		if(getLogLevel().ordinal() <= LogLevel.TRACE.ordinal())
			nodeLogger.write(this, LogLevel.TRACE, arg0);
	}

	public void trace(String arg0, Object arg1) {
		sl4jLogger.trace(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.TRACE.ordinal())
		{
			String x = MessageFormatter.format(arg0,arg1);
			nodeLogger.write(this, LogLevel.TRACE, x);
		}	
	}

	public void trace(String arg0, Object[] arg1) {
		sl4jLogger.trace(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.TRACE.ordinal())
		{
			String x = MessageFormatter.arrayFormat(arg0, arg1);
			nodeLogger.write(this, LogLevel.TRACE, x);
		}	
	}

	public void trace(String arg0, Throwable arg1) 
	{
		sl4jLogger.trace(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.TRACE.ordinal())
			nodeLogger.write(this, LogLevel.TRACE, arg0 +" : "+ getTrace(arg1));
	}

	public void trace(Marker arg0, String arg1) {
		sl4jLogger.trace(arg0,arg1);
	}

	public void trace(String arg0, Object arg1, Object arg2) {
		sl4jLogger.trace(arg0,arg1,arg2);
		if(getLogLevel().ordinal() <= LogLevel.TRACE.ordinal())
		{
			String x = MessageFormatter.format(arg0, arg1, arg2);
			nodeLogger.write(this, LogLevel.TRACE, x);
		}	
	}

	public void trace(Marker arg0, String arg1, Object arg2) {
		sl4jLogger.trace(arg0,arg1,arg2);
	}

	public void trace(Marker arg0, String arg1, Object[] arg2) {
		sl4jLogger.trace(arg0,arg1,arg2);
	}

	public void trace(Marker arg0, String arg1, Throwable arg2) {
		sl4jLogger.trace(arg0,arg1,arg2);
	}

	public void trace(Marker arg0, String arg1, Object arg2, Object arg3) {
		sl4jLogger.trace(arg0,arg1,arg2,arg3);
	}

	public void info(String arg0) {
		sl4jLogger.info(arg0);
		if(getLogLevel().ordinal() <= LogLevel.INFO.ordinal())
			nodeLogger.write(this, LogLevel.INFO, arg0);
	}

	public void info(String arg0, Object arg1) {
		sl4jLogger.info(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.INFO.ordinal())
		{
			String x = MessageFormatter.format(arg0,arg1);
			nodeLogger.write(this, LogLevel.INFO, x);
		}	
	}

	public void info(String arg0, Object[] arg1) {
		sl4jLogger.info(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.INFO.ordinal())
		{
			String x = MessageFormatter.arrayFormat(arg0, arg1);
			nodeLogger.write(this, LogLevel.INFO, x);
		}	
	}

	public void info(String arg0, Throwable arg1) 
	{
		sl4jLogger.info(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.INFO.ordinal())
			nodeLogger.write(this, LogLevel.INFO, arg0 +" : "+ getTrace(arg1));
	}

	public void info(Marker arg0, String arg1) {
		sl4jLogger.info(arg0,arg1);
	}

	public void info(String arg0, Object arg1, Object arg2) {
		sl4jLogger.info(arg0,arg1,arg2);
		if(getLogLevel().ordinal() <= LogLevel.INFO.ordinal())
		{
			String x = MessageFormatter.format(arg0, arg1, arg2);
			nodeLogger.write(this, LogLevel.INFO, x);
		}	
	}

	public void info(Marker arg0, String arg1, Object arg2) {
		sl4jLogger.info(arg0,arg1,arg2);
	}

	public void info(Marker arg0, String arg1, Object[] arg2) {
		sl4jLogger.info(arg0,arg1,arg2);
	}

	public void info(Marker arg0, String arg1, Throwable arg2) {
		sl4jLogger.info(arg0,arg1,arg2);
	}

	public void info(Marker arg0, String arg1, Object arg2, Object arg3) {
		sl4jLogger.info(arg0,arg1,arg2,arg3);
	}

	public void warn(String arg0) {
		sl4jLogger.warn(arg0);
		if(getLogLevel().ordinal() <= LogLevel.WARN.ordinal())
			nodeLogger.write(this, LogLevel.WARN, arg0);
	}

	public void warn(String arg0, Object arg1) {
		sl4jLogger.warn(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.WARN.ordinal())
		{
			String x = MessageFormatter.format(arg0,arg1);
			nodeLogger.write(this, LogLevel.WARN, x);
		}	
	}

	public void warn(String arg0, Object[] arg1) {
		sl4jLogger.warn(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.WARN.ordinal())
		{
			String x = MessageFormatter.arrayFormat(arg0, arg1);
			nodeLogger.write(this, LogLevel.WARN, x);
		}	
	}

	public void warn(String arg0, Throwable arg1) {
		sl4jLogger.warn(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.WARN.ordinal())
		{
			nodeLogger.write(this, LogLevel.WARN, arg0 +" : "+ getTrace(arg1));
		}	
	}

	public void warn(Marker arg0, String arg1) {
		sl4jLogger.warn(arg0,arg1);
	}

	public void warn(String arg0, Object arg1, Object arg2) {
		sl4jLogger.warn(arg0,arg1,arg2);
		if(getLogLevel().ordinal() <= LogLevel.WARN.ordinal())
		{
			String x = MessageFormatter.format(arg0, arg1, arg2);
			nodeLogger.write(this, LogLevel.WARN, x);
		}	
	}

	public void warn(Marker arg0, String arg1, Object arg2) {
		sl4jLogger.warn(arg0,arg1,arg2);
	}

	public void warn(Marker arg0, String arg1, Object[] arg2) {
		sl4jLogger.warn(arg0,arg1,arg2);
	}

	public void warn(Marker arg0, String arg1, Throwable arg2) {
		sl4jLogger.warn(arg0,arg1,arg2);
	}

	public void warn(Marker arg0, String arg1, Object arg2, Object arg3) {
		sl4jLogger.warn(arg0,arg1,arg2,arg3);
	}

	public void error(String arg0) {
		sl4jLogger.error(arg0);
		if(getLogLevel().ordinal() <= LogLevel.ERROR.ordinal())
			nodeLogger.write(this, LogLevel.ERROR, arg0);
	}

	public void error(String arg0, Object arg1) {
		sl4jLogger.error(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.ERROR.ordinal())
		{
			String x = MessageFormatter.format(arg0,arg1);
			nodeLogger.write(this, LogLevel.ERROR, x);
		}	
	}

	public void error(String arg0, Object[] arg1) {
		sl4jLogger.error(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.ERROR.ordinal())
		{
			String x = MessageFormatter.arrayFormat(arg0, arg1);
			nodeLogger.write(this, LogLevel.ERROR, x);
		}	
	}

	public void error(String arg0, Throwable arg1) 
	{
		sl4jLogger.error(arg0,arg1);
		if(getLogLevel().ordinal() <= LogLevel.ERROR.ordinal())
			nodeLogger.write(this, LogLevel.ERROR, arg0 +" : "+ getTrace(arg1));
	}

	public void error(Marker arg0, String arg1) {
		sl4jLogger.error(arg0,arg1);
	}

	public void error(String arg0, Object arg1, Object arg2) {
		sl4jLogger.error(arg0,arg1,arg2);
		if(getLogLevel().ordinal() <= LogLevel.ERROR.ordinal())
		{
			String x = MessageFormatter.format(arg0, arg1, arg2);
			nodeLogger.write(this, LogLevel.ERROR, x);
		}	
	}

	public void error(Marker arg0, String arg1, Object arg2) {
		sl4jLogger.error(arg0,arg1,arg2);
	}

	public void error(Marker arg0, String arg1, Object[] arg2) {
		sl4jLogger.error(arg0,arg1,arg2);
	}

	public void error(Marker arg0, String arg1, Throwable arg2) {
		sl4jLogger.error(arg0,arg1,arg2);
	}

	public void error(Marker arg0, String arg1, Object arg2, Object arg3) {
		sl4jLogger.error(arg0,arg1,arg2,arg3);
	}

	public boolean isDebugEnabled() {
		return sl4jLogger.isDebugEnabled();
	}

	public boolean isDebugEnabled(Marker arg0) {
		return sl4jLogger.isDebugEnabled(arg0);
	}

	public boolean isErrorEnabled() {
		return sl4jLogger.isErrorEnabled();
	}

	public boolean isErrorEnabled(Marker arg0) {
		return sl4jLogger.isErrorEnabled(arg0);
	}

	public boolean isInfoEnabled() {
		return sl4jLogger.isInfoEnabled();
	}

	public boolean isInfoEnabled(Marker arg0) {
		return sl4jLogger.isInfoEnabled(arg0);
	}

	public boolean isTraceEnabled() {
		return sl4jLogger.isTraceEnabled();
	}

	public boolean isTraceEnabled(Marker arg0) {
		return sl4jLogger.isTraceEnabled(arg0);
	}

	public boolean isWarnEnabled() {
		return sl4jLogger.isWarnEnabled();
	}

	public boolean isWarnEnabled(Marker arg0) {
		return sl4jLogger.isWarnEnabled(arg0);
	}

	public String getPrefix() {
		return "";
	}

    public Map<String, Object> getVariables() {
        return variables;
    }
}

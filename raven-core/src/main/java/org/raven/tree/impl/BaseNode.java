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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.raven.tree.AttributesGenerator;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.NodeParameter;
import org.raven.annotations.Parameter;
import org.raven.conf.Configurator;
import org.raven.template.TemplateEntry;
import org.raven.tree.NodeAttributeListener;
import org.raven.tree.NodeListener;
import org.raven.tree.NodePathResolver;
import org.raven.tree.NodeShutdownError;
import org.raven.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class BaseNode implements Node, NodeListener
{
    protected Logger logger = LoggerFactory.getLogger(Node.class);
    
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
    
    private int id;
    
    private String name;
    private byte level = 0;
    private int index = 0;
    
    private Node parent;
    
    private Map<String, Node> childrens;
    private Map<String, NodeAttribute> nodeAttributes;
    private Set<Node> dependentNodes;
    private Collection<NodeListener>  listeners;
    private Map<NodeAttribute, Set<NodeAttributeListener>> attributesListeners;
    
    private boolean autoStart = true;
    private boolean initializeAfterChildrens = false;
    private Status status = Status.CREATED;
    
    private Map<String, NodeParameter> parameters;
    private boolean subtreeListener = false;

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
    
    protected void initFields()
    {
        parent = null;
        dependentNodes = null;
        listeners = null;
        attributesListeners = null;
        status = Status.CREATED;
        parameters = null;
        nodeAttributes = null;
        childrens = null;
    }

    public Logger getLogger()
    {
        return logger;
    }

    public int getId() 
    {
        return id;
    }

    private void initDependentNodes()
    {
        if (dependentNodes != null)
            for (Node node : dependentNodes)
                initDependentNode(node);
            
        if (attributesListeners != null)
            for (Set<NodeAttributeListener> listeners : attributesListeners.values())
                for (NodeAttributeListener listener : listeners)
                    if (listener instanceof NodeAttribute)
                        initDependentNode(((NodeAttribute) listener).getOwner());
    }

    private void initDependentNode(Node node)
    {
        if (node.getStatus()==Status.CREATED)
        {
            node.init();
            if (node.isAutoStart())
                node.start();
        }
    }
    
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
                for (NodeAttributeListener listener : listenersSet)
                {
                    listener.nodeAttributeValueChanged(this, attr, oldValue, newValue);
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
    
    public List<Node> getChildrenList() 
    { 
        Collection<Node> nc = this.getChildrens();
        ArrayList<Node> na = new ArrayList<Node>();
        if(nc!=null) na.addAll(nc);
        return na; 
    }

    public boolean isTemplate()
    {
        Node pNode = this;
        while ( (pNode=pNode.getParent())!=null )
            if (pNode instanceof TemplateEntry)
                return true;
        return false;
    }

    public void setStatus(Status status)
    {
        if (this.status != status)
        {
            Status oldStatus = this.status;
            this.status = status;

            fireStatusChanged(oldStatus, status);
        }
    }

    public Status getStatus()
    {
        return status;
    }

    public synchronized void addListener(NodeListener listener)
    {
        if (listeners==null)
            listeners = new HashSet<NodeListener>();
        
        listeners.add(listener);
        if (listener.isSubtreeListener() && childrens!=null)
            for (Node children: childrens.values())
                children.addListener(listener);
    }
    
    public synchronized void removeListener(NodeListener listener)
    {
        if (listeners!=null)
            if (listeners.remove(listener) && listener.isSubtreeListener() && childrens!=null)
                for (Node children: childrens.values())
                    children.removeListener(listener);
    }
    
    public synchronized Collection<NodeListener> getListeners()
    {
        return listeners;
    }

    public synchronized void addChildren(Node node)
    {
        if (childrens==null)
            childrens = new TreeMap<String, Node>();
        
        node.setParent(this);
        if (node.getIndex()==0)
            node.setIndex(childrens.size()+1);
        
        childrens.put(node.getName(), node);
        node.addListener(this);
        
        if (listeners!=null)
            for (NodeListener listener: listeners)
                if (listener.isSubtreeListener())
                    node.addListener(listener);
        fireChildrenAdded(node);
    }

    public void removeChildren(Node node)
    {
        if (logger.isDebugEnabled())
            logger.debug(String.format(
                    "Removing children node (%s) from (%s) node", node.getPath(), getPath()));
        
        if (childrens!=null)
        {
            Node removedNode = childrens.remove(node.getName());
            if (removedNode!=null)
                removedNode.remove();
//                fireNodeRemoved(removedNode);
        }
    }
    
    public synchronized boolean addDependentNode(Node dependentNode)
    {
        if (dependentNodes==null)
            dependentNodes = new HashSet<Node>();
        
        return dependentNodes.add(dependentNode);
    }

    public synchronized void addNodeAttributeDependency(
            String attributeName, NodeAttributeListener listener)
    {
        NodeAttribute attr = getNodeAttribute(attributeName);
        if (attr==null)
            throw new NodeError(String.format(
                    "Attribute (%s) not found in the node (%s)", attributeName, getPath()));
        
        if (attributesListeners==null)
            attributesListeners = new HashMap<NodeAttribute, Set<NodeAttributeListener>>();
        
        Set<NodeAttributeListener> listeners = attributesListeners.get(attr);
        if (listeners==null)
        {
            listeners = new HashSet<NodeAttributeListener>();
            attributesListeners.put(attr, listeners);
        }
        
        listeners.add(listener);
    }

    public synchronized boolean removeDependentNode(Node dependentNode)
    {
        return dependentNodes==null? false : dependentNodes.remove(dependentNode);
    }

    public synchronized void addNodeAttribute(NodeAttribute attr)
    {
        if (nodeAttributes==null)
            nodeAttributes = new TreeMap<String, NodeAttribute>();
        
        nodeAttributes.put(attr.getName(), attr);
    }

    public synchronized void removeNodeAttribute(String name)
    {
        if (nodeAttributes!=null)
        {
            NodeAttribute attr = nodeAttributes.remove(name);
            if (attr!=null)
                fireNodeAttributeRemoved(attr);
        }
    }

    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        String oldName = this.name;
        this.name = name;
        if (   ObjectUtils.in(status, Status.INITIALIZED, Status.STARTED) 
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
        this.index = index;
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

    public List<Class> getChildNodeTypes()
    {
        return tree.getChildNodesTypes(getClass());
    }

    public int getChildrenCount()
    {
        return childrens==null? 0 : childrens.size();
    }
    
    public Collection<Node> getChildrens()
    {
        return childrens==null? null : childrens.values();
    }
    
    public Collection<Node> getSortedChildrens()
    {
        return childrens==null? null : new TreeSet<Node>(childrens.values());
    }

    public synchronized Node getChildren(String name)
    {
        return childrens==null? null : childrens.get(name);
    }

    public synchronized Collection<NodeAttribute> getNodeAttributes()
    {
        return nodeAttributes==null? null : nodeAttributes.values();
    }

    public synchronized NodeAttribute getNodeAttribute(String name)
    {
        return nodeAttributes==null? null : nodeAttributes.get(name);
    }

    public synchronized Set<Node> getDependentNodes()
    {
        return dependentNodes;
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

    public boolean isAutoStart()
    {
        return autoStart;
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
            if (isTemplate())
                return false;
            if (nodeAttributes!=null)
                for (NodeAttribute attr: nodeAttributes.values())
                    if (attr.isRequired() && attr.getValue()==null)
                    {
                        logger.info(String.format(
                                "Error switching node (%s) to the STARTED state. " +
                                "Value for required attribute (%s) not seted "
                                , getPath(), attr.getName()));
                        return false;
                    }
            doStart();
            setStatus(Status.STARTED);
        }catch (Exception e)
        {
            logger.error(
                    String.format("Error starting node (%s)", getPath())
                    , e);
            return false;
        }
        return true;
    }

    public synchronized void stop() throws NodeError
    {
        setStatus(Status.INITIALIZED);
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
        if (status==Status.STARTED)
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

    private void fireNodeRemoved()
    {
        if (listeners!=null)
            for (NodeListener listener: listeners)
                listener.nodeRemoved(this);
    }

    public NodeAttribute getParentAttribute(String attributeName)
    {
        Node node = this;
        while ( (node=node.getParent())!=null )
        {
            NodeAttribute attr = node.getNodeAttribute(attributeName);
            if (attr!=null)
                return attr;
        }
        return null;
    }

    /**
     * Method returns the first not null value of the attribute, with name passed in the 
     * <code>attributeName</code> parameter, of the nearest parent or null if parents does not
     * contains the attribute with name passed in the parameter.
     * @param attributeName the name of the attribute
     */
    public String getParentAttributeValue(String attributeName)
    {
        Node node = this;
        while ( (node=node.getParent())!=null )
        {
            NodeAttribute attr = node.getNodeAttribute(attributeName);
            if (attr!=null)
                return attr.getValue();
        }
        return null;
    }

    public <T> T getParentAttributeRealValue(String attributeName)
    {
        Node node = this;
        while ( (node=node.getParent())!=null )
        {
            NodeAttribute attr = node.getNodeAttribute(attributeName);
            if (attr!=null)
                return (T) attr.getRealValue();
        }
        return null;
    }

    public void save()
    {
        configurator.getTreeStore().saveNode(this);
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
                        NodeParameter param = new NodeParameterImpl((Parameter) ann,this, desc);
                        
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
                for (Iterator<NodeListener> it=listeners.iterator(); it.hasNext();)
                {
                    boolean removeListener = it.next().nodeAttributeRemoved(this, attr);
                    if (removeListener)
                        it.remove();
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

    private synchronized void fireStatusChanged(Status oldStatus, Status status)
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
                    "Error generating child attributes for (%s) attribute"), attr.getName());
        }
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
        if (parent.getValue() == null)
            removeChildAttributes(parent.getName(), null);
        else
        {
            AttributesGenerator attributesGenerator = (AttributesGenerator)converter.convert(
                        parent.getType(), parent.getValue(), null);

            Collection<NodeAttribute> newAttrs = attributesGenerator.generateAttributes();

            removeChildAttributes(parent.getName(), newAttrs);

            if (newAttrs != null)
            {
                for (NodeAttribute newAttr : newAttrs)
                {
                    if (nodeAttributes.containsKey(newAttr.getName()))
                        continue;

                    NodeAttribute clone = null;
                    try
                    {
                        clone = (NodeAttribute) newAttr.clone();
                    } catch (CloneNotSupportedException ex)
                    {
                        throw new NodeError(String.format(
                                "Error in the node (%s). Attribute (%s) clone error"
                                , getPath(), clone.getName()), ex);
                    }
                    clone.setOwner(this);
                    clone.setParentAttribute(parent.getName());

                    addNodeAttribute(clone);
                    try
                    {
                        clone.init();
                        clone.save();
                    } 
                    catch (Exception exception)
                    {
                        logger.error(String.format(
                                "Error initializing the attribute (%s) of the node (%s)"
                                , clone.getName(), getPath()))                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  ;
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
    
//    private void syncParameterWithAttribute()
    
    private void createNodeAttribute(NodeParameter param) 
        throws Exception
    {
        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setOwner(this);
        attr.setName(param.getDisplayName());
        attr.setParameterName(param.getName());
        attr.setParameter(param);
        attr.setDescription(param.getDescription());
        attr.setRawValue(param.getDefaultValue());
        attr.setType(param.getType());
        addNodeAttribute(attr);

        configurator.getTreeStore().saveNodeAttribute(attr);
        
        attr.init();
    }
    
    void removeChildAttributes(String parentName, Collection<NodeAttribute> leaveAttributes)
    {
        Iterator<Map.Entry<String, NodeAttribute>> it = nodeAttributes.entrySet().iterator();
        while (it.hasNext())
        {
            NodeAttribute childAttr = it.next().getValue();
            if (parentName.equals(childAttr.getParentAttribute()))
            {
                boolean removeAttr = true;
                if (leaveAttributes!=null)
                {
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
                if (removeAttr)
                {
                    configurator.getTreeStore().removeNodeAttribute(childAttr.getId());
                    it.remove();
                }
            }
        }
        
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final BaseNode other = (BaseNode) obj;
        if (this.id != other.id)
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 13 * hash + (int) (this.id ^ (this.id >>> 32));
        return hash;
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
        if (   childrens!=null 
            && childrens.containsKey(oldName) 
            && node.equals(childrens.get(oldName)))
        {
            childrens.remove(oldName);
            childrens.put(newName, node);
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

    public void nodeRemoved(Node removedNode)
    {
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
    
    public Node cloneTo(Node dest, String newNodeName) throws CloneNotSupportedException
    {
        Node clone = (Node) clone();
        if (newNodeName!=null)
            clone.setName(newNodeName);
        dest.addChildren(clone);
        
//        if (nodeAttributes!=null)
//            for (NodeAttribute attr: nodeAttributes.values()) 
//            {
//                NodeAttribute attrClone = (NodeAttribute) attr.clone();
//                attrClone.setOwner(clone);
//                clone.addNodeAttribute(attrClone);
////                attrClone.init();
//            }
            
        if (childrens!=null)
            for (Node child: childrens.values())
            {
                child.cloneTo(clone, null);
            }
        
        return clone;
    }

    @Override
    public String toString()
    {
        return name;
    }

}

/*
 *  Copyright 2008 tim.
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
import java.util.Collections;
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
import org.raven.tree.NodeListener;
import org.raven.tree.NodeShutdownError;
import org.raven.tree.Tree;
import org.raven.tree.store.TreeStoreError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.beans.ClassDescriptor;
import org.weda.beans.ObjectUtils;
import org.weda.beans.PropertyDescriptor;
import org.weda.constraints.ConstraintException;
import org.weda.internal.annotations.Service;
import org.weda.services.ClassDescriptorRegistry;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class BaseNode implements Node, NodeListener, Comparable<Node>
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
    
    private int id;
    
    private String name;
    private byte level = 0;
    private int index = 0;
    private final Class[] childNodeTypes;
    private final boolean container;
    private final boolean readOnly;
    
    private Node parent;
    
    private Map<String, Node> childrens;
    private Map<String, NodeAttribute> nodeAttributes;
    private Set<Node> dependentNodes;
    private Collection<NodeListener>  listeners;
    
    private boolean autoStart = true;
    private boolean initializeAfterChildrens = false;
    private Status status = Status.CREATED;
    
    private Map<String, NodeParameter> parameters;

    public BaseNode(Class[] childNodeTypes, boolean container, boolean readOnly)
    {
        this.childNodeTypes = childNodeTypes;
        this.container = container;
        this.readOnly = readOnly;
    }

    public int getId() 
    {
        return id;
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


    protected void setStatus(Status status)
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
    }

    public void removeChildren(Node node)
    {
        if (childrens!=null)
            childrens.remove(node.getName());
    }
    
    public synchronized boolean addDependentNode(Node dependentNode)
    {
        if (dependentNodes==null)
            dependentNodes = new HashSet<Node>();
        
        return dependentNodes.add(dependentNode);
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

    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        String oldName = this.name;
        this.name = name;
        if (status!=Status.CREATED && !ObjectUtils.equals(oldName, name))
        {
            fireNameChanged(oldName, name);
        }
    }

    public byte getLevel()
    {
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
    
    public boolean isReadOnly()
    {
        return readOnly;
    }
    
    public boolean isContainer()
    {
        return container;
    }

    public Class[] getChildNodeTypes()
    {
        return childNodeTypes!=null? childNodeTypes : tree.getAvailableNodesTypes();
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
        level = 0; Node node = this;
        while ( (node=node.getParent())!=null )
            ++level;
    }

    public String getPath()
    {
        StringBuffer path = new StringBuffer(name);
        Node node = this;
        while ( (node=node.getParent()) != null )
            path.insert(0, node.getName()+Node.NODE_SEPARATOR);
        
        return path.toString();
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
    
    public void init() throws NodeError
    {
        try
        {
            boolean dependenciesInitialized = true;
            if (nodeAttributes != null)
            {
                for (NodeAttribute attr : nodeAttributes.values())
                {
                    if (Node.class.isAssignableFrom(attr.getType()) && attr.getValue()!=null)
                    {
                        Node node = converter.convert(Node.class, attr.getValue(), null);
                        node.addDependentNode(this);
                        if (node.getStatus()==Status.CREATED)
                        {
                            dependenciesInitialized = false;
                        }
                    }
                }
            }
            
            if (!dependenciesInitialized)
            {
                return;
            }
            extractNodeLogicParameters();
            syncAttributesAndParameters();
            
            setStatus(Status.INITIALIZED);
            
            if (dependentNodes != null)
                for (Node node : dependentNodes)
                    if (node.getStatus()==Status.CREATED)
                    {
                        node.init();
                        if (node.isAutoStart())
                            node.start();
                    }
                                
        } catch (Exception e)
        {
            throw new NodeError(
                    String.format("Node (%s) initialization error", getPath())
                    , e);
        }
    }

    public boolean start() throws NodeError
    {
        if (nodeAttributes!=null)
            for (NodeAttribute attr: nodeAttributes.values())
                if (attr.isRequired() && attr.getValue()==null)
                {
                    logger.info(
                            "Error switching node (%s) to the STARTED state. " +
                            "Value for required attribute (%s) not seted "
                            , getPath(), attr.getName());
                    return false;
                }
        
        setStatus(Status.STARTED);
        
        return true;
    }

    public synchronized void stop() throws NodeError
    {
        setStatus(Status.INITIALIZED);
    }
    
    public synchronized void shutdown() throws NodeShutdownError
    {
        if (status==Status.STARTED)
            stop();
        if (nodeAttributes!=null)
            for (NodeAttribute attr: nodeAttributes.values())
                if (attr instanceof Node && attr.getValue()!=null)
                {
                    Node node = attr.getRealValue();
                    node.removeDependentNode(this);
                }
    }

    void fireAttributeValueChanged(NodeAttributeImpl attr, String oldValue)
    {
        if (Node.class.isAssignableFrom(attr.getType()))
        {
            if (oldValue!=null)
            {
                Node oldRef = (Node) converter.convert(attr.getType(), oldValue, null);
                oldRef.removeDependentNode(this);
            }
            if (attr.getValue()!=null)
            {
                Node node = (Node) converter.convert(attr.getType(), attr.getValue(), null);
                node.addDependentNode(this);
            }
        }
        if (attr.isGeneratorType())
        {
            Iterator<Map.Entry<String, NodeAttribute>> it = nodeAttributes.entrySet().iterator();
            while (it.hasNext())
            {
                NodeAttribute childAttr = it.next().getValue();
                if (attr.getName().equals(childAttr.getParentAttribute()))
                {
                    configurator.getTreeStore().removeNodeAttribute(childAttr.getId());
                    it.remove();
                }
            }
            
            if (attr.getValue()!=null)
            {
                AttributesGenerator attributesGenerator = 
                        (AttributesGenerator) converter.convert(
                            attr.getType(), attr.getValue(), null);
                
                Collection<NodeAttribute> newAttrs = attributesGenerator.generateAttributes();
                if (newAttrs!=null)
                    for (NodeAttribute newAttr: newAttrs)
                    {
                        NodeAttribute clone = null;
                        try
                        {
                            clone = (NodeAttribute) newAttr.clone();
                        } catch (CloneNotSupportedException ex)
                        {
                            throw new NodeError(
                                    String.format(
                                        "Error in the node (%s). Attribute (%s) clone error"
                                        , getPath(), clone.getName())
                                    , ex);
                        }
                        clone.setOwner(this);
                        clone.setParentAttribute(attr.getName());
                        
                        configurator.getTreeStore().saveNodeAttribute(clone);
                        
                        addNodeAttribute(clone);
                    }
            }
        }
        if (listeners!=null)
            for (NodeListener listener: listeners)
                listener.nodeAttributeValueChanged(this, attr, oldValue, attr.getValue());
    }
    
    /**
     * Method returns the first not null value of the attribute, with name passed in the 
     * <code>attributeName</code> parameter, of the nearest parent or null if parents does not
     * contains the attribute with name passed in the parameter.
     * @param attributeName the name of the attribute
     * @return
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

    private void extractNodeLogicParameters()
    {
        if (parameters!=null)
            parameters = null;
        ClassDescriptor classDescriptor = descriptorRegistry.getClassDescriptor(getClass());
        PropertyDescriptor[] descs = classDescriptor.getPropertyDescriptors();
        for (PropertyDescriptor desc: descs)
            if (desc.isReadable() && desc.isWriteable())
                for (Annotation ann: desc.getAnnotations())
                    if (ann instanceof Parameter)
                    {
                        NodeParameter param = new NodeParameterImpl(this, desc);
                        
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

    private synchronized void fireStatusChanged(Status oldStatus, Status status)
    {
        if (listeners!=null)
            for (NodeListener listener: listeners)
                listener.nodeStatusChanged(this, oldStatus, status);
    }

    private void syncAttributesAndParameters() throws ConstraintException, TreeStoreError
    {
        if (nodeAttributes!=null)
        {
            Iterator<Map.Entry<String, NodeAttribute>> it = nodeAttributes.entrySet().iterator();
            while (it.hasNext())
            {
                NodeAttribute attr = it.next().getValue();
                if (attr.getParameterName()!=null)
                {
                    NodeParameter param = 
                            parameters==null? null : parameters.get(attr.getParameterName());
                    if (param==null)
                        it.remove();
                    else
                    {
                        param.setNodeAttribute(attr);
                        param.setValue(attr.getValue());
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
        throws TreeStoreError, ConstraintException
    {
        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setOwner(this);
        attr.setName(param.getDisplayName());
        attr.setParameterName(param.getName());
        attr.setParameter(param);
        attr.setDescription(param.getDescription());
        //TODO: убрать
        
        attr.setValue(converter.convert(String.class, param.getValue(), param.getPattern()));
        attr.setType(param.getType());

        addNodeAttribute(attr);

        configurator.getTreeStore().saveNodeAttribute(attr);
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

    public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus)
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
            Node node, NodeAttribute attribute, String oldValue, String newValue)
    {
    }

    public int compareTo(Node o)
    {
        return index-o.getIndex();
    }
}

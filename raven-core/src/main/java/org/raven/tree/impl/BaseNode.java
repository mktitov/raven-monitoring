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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.raven.tree.AttributesGenerator;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.NodeLogic;
import org.raven.tree.NodeLogicParameter;
import org.raven.annotations.Parameter;
import org.raven.conf.Configurator;
import org.raven.tree.NodeShutdownError;
import org.raven.tree.store.TreeStoreError;
import org.weda.beans.ClassDescriptor;
import org.weda.beans.PropertyDescriptor;
import org.weda.constraints.ConstraintException;
import org.weda.internal.annotations.Service;
import org.weda.services.ClassDescriptorRegistry;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class BaseNode<T extends NodeLogic> implements Node<T>
{
    @Service
    private ClassDescriptorRegistry descriptorRegistry;
    @Service
    private TypeConverter converter;
    @Service
    private Configurator configurator;
    
    private int id;
    
    private String name;
    private byte level = 0;
    private final Class[] childNodeTypes;
    private final boolean container;
    private final boolean readOnly;
    
    private Node parent;
    
    private Map<String, Node> childrens;
    private Map<String, NodeAttribute> nodeAttributes;
    private Class<? extends T> nodeLogicType;
    private T nodeLogic;
    
    private Set<Node> dependentNodes;
    
    private boolean initialized = false;
    
    private Map<String, NodeLogicParameter> parameters;

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

    public void addChildren(Node node)
    {
        if (childrens==null)
            childrens = new HashMap<String, Node>();
        
        node.setParent(this);
        
        childrens.put(node.getName(), node);
    }

    public void addDependentNode(Node dependentNode)
    {
        if (dependentNodes==null)
            dependentNodes = new HashSet<Node>();
        
        dependentNodes.add(dependentNode);
    }

    public void addNodeAttribute(NodeAttribute attr)
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
        this.name = name;
    }

    public byte getLevel()
    {
        return level;
    }
    
    public boolean isReadOnly()
    {
        return readOnly;
    }
    
    public Class<? extends T> getNodeLogicType()
    {
        return nodeLogicType;
    }

    public void setNodeLogicType(Class<? extends T> nodeLogicType) 
    {
        try
        {
            if (initialized && nodeLogicType != this.nodeLogicType)
            {
                this.nodeLogicType = nodeLogicType;
                if (nodeLogicType != null)
                {
                    createNodeLogic();
                }
                syncAttributesAndParameters();
            } else
            {
                this.nodeLogicType = nodeLogicType;
            }
        } catch (Exception e)
        {
            throw new NodeError(
                    String.format("Error while setting node (%s) logic type", getPath())
                    , e);
        }
    }

    public boolean isContainer()
    {
        return container;
    }

    public Class[] getChildNodeTypes()
    {
        return childNodeTypes;
    }

    public Collection<Node> getChildrens()
    {
        return childrens==null? null : childrens.values();
    }

    public Node getChildren(String name)
    {
        return childrens==null? null : childrens.get(name);
    }

    public Collection<NodeAttribute> getNodeAttributes()
    {
        return nodeAttributes==null? null : nodeAttributes.values();
    }

    public NodeAttribute getNodeAttribute(String name)
    {
        return nodeAttributes==null? null : nodeAttributes.get(name);
    }

    public Set<Node> getDependentNodes()
    {
        return dependentNodes;
    }

    public T getNodeLogic()
    {
        return nodeLogic;
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
            path.insert(0, node.getName()+"/");
        
        return path.toString();
    }

    public boolean isInitialized()
    {
        return initialized;
    }
    
    public void init() throws NodeError
    {
        boolean dependenciesInitialized = true;
        if (nodeAttributes!=null)
        {
            for (NodeAttribute attr: nodeAttributes.values())
            {
                if (Node.class.isAssignableFrom(attr.getType()))
                {
                    Node node = converter.convert(Node.class, attr.getValue(), null);
                    node.addDependentNode(this);
                    if (!node.isInitialized())
                        dependenciesInitialized = false;
                }
            }
        }
            
        if (!dependenciesInitialized)
            return;
            
        if (nodeLogicType!=null)
        {
            try
            {
                createNodeLogic();
                syncAttributesAndParameters();
                
                nodeLogic.init();
                
            } catch (Exception ex)
            {
                throw new NodeError(String.format(
                        "Error initializing node (%s)", getPath())
                        , ex);
            }
        }
            
        initialized = true;
        
        if (dependentNodes!=null)
        {
            for (Node node: dependentNodes)
                if (!node.isInitialized())
                    node.init();
        }
        
    }

    public void shutdown() throws NodeShutdownError
    {
        if (nodeLogic!=null)
            nodeLogic.shutdown();
    }

    void fireAttributeValueChanged(NodeAttributeImpl attr)
    {
        if (attr.isGeneratorType())
        {
            Iterator<Map.Entry<String, NodeAttribute>> it = nodeAttributes.entrySet().iterator();
            while (it.hasNext())
            {
                NodeAttribute childAttr = it.next().getValue();
                if (attr.getName().equals(childAttr.getParentAttribute()))
                    it.remove();
            }
            
            if (attr.getValue()!=null)
            {
                AttributesGenerator attributesGenerator = 
                        (AttributesGenerator) converter.convert(
                            attr.getType(), attr.getValue(), null);
                attributesGenerator.generateAttributes(this, attr.getName());
            }
        }
    }
    
    /**
     * Method returns the first not null value of the attribute, with name passed in the 
     * <code>attributeName</code> parameter, of the nearest parent or null if parents does not
     * contains the attribute with name passed in the parameter.
     * @param attributeName the name of the attribute
     * @return
     */
    String getParentNodeAttributeValue(String attributeName)
    {
        Node node = this;
        while ( (node=node.getParent())!=null )
        {
            NodeAttribute attr = node.getNodeAttribute(attributeName);
            if (attr!=null)
            {
                String val = attr.getValue();
                if (val!=null)
                    return val;
            }
        }
        return null;
    }

    private void createNodeLogic() throws NodeError
    {
        try
        {
            nodeLogic = nodeLogicType.newInstance();
            nodeLogic.setOwner(this);
            extractNodeLogicParameters();            
        } catch (Exception e)
        {
            throw new NodeError(
                    String.format(
                        "Error creating the node logic (%s)", nodeLogicType.getName())
                    , e);
        }
    }

    private void extractNodeLogicParameters()
    {
        if (parameters!=null)
            parameters = null;
        ClassDescriptor classDescriptor = descriptorRegistry.getClassDescriptor(nodeLogicType);
        PropertyDescriptor[] descs = classDescriptor.getPropertyDescriptors();
        for (PropertyDescriptor desc: descs)
            if (desc.isReadable() && desc.isWriteable())
                for (Annotation ann: desc.getAnnotations())
                    if (ann instanceof Parameter)
                    {
                        NodeLogicParameter param = new NodeLogicParameterImpl(nodeLogic, desc);
                        
                        if (parameters==null)
                            parameters = new HashMap<String, NodeLogicParameter>();
                        
                        parameters.put(desc.getName(), param);
                        
                        break;
                    }
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
                    NodeLogicParameter param = 
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
            for (NodeLogicParameter param: parameters.values())
                if (param.getNodeAttribute()==null)
                    createNodeAttribute(param);
        }
    }
    
    private void createNodeAttribute(NodeLogicParameter param) 
        throws TreeStoreError, ConstraintException
    {
        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setOwner(this);
        attr.setName(param.getDisplayName());
        attr.setParameterName(param.getName());
        attr.setParameter(param);
        attr.setDescription(param.getDescription());
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
        final BaseNode<T> other = (BaseNode<T>) obj;
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

}

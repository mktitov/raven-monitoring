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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.Key;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Value;
import org.raven.tree.AttributesGenerator;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeInitializationError;
import org.raven.tree.NodeLogic;
import org.raven.tree.NodeLogicParameter;
import org.raven.annotations.Parameter;
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
@PersistenceCapable(detachable="true", identityType=IdentityType.APPLICATION)
@Inheritance()
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="0")
public class BaseNode<T extends NodeLogic> implements Node<T>
{
    @Service
    private ClassDescriptorRegistry descriptorRegistry;
    @Service
    private TypeConverter converter;
    
    @PrimaryKey()
    @Persistent(valueStrategy=IdGeneratorStrategy.NATIVE)
    private long id;
    
    private String name;
    private int level = 0;
    private final Class[] childNodeTypes;
    private final boolean container;
    private final boolean readOnly;
    
    @Persistent(types=BaseNode.class, defaultFetchGroup="true")
    private Node parent;
    
//    @Persistent(defaultFetchGroup="true")
//    @Key(mappedBy="name")
//    @Value(types=BaseNode.class)
    @NotPersistent
    private Map<String, Node> childrens;
    
    @Persistent(defaultFetchGroup="true", mappedBy="owner", dependentValue="true")
    @Key(mappedBy="name")
    @Value(types=NodeAttributeImpl.class)
    private Map<String, NodeAttribute> nodeAttributes;
    @Persistent
    private String nodeLogicTypeName;
    @NotPersistent()
    private Class<? extends T> nodeLogicType;
    @NotPersistent
    private T nodeLogic;
    
    @NotPersistent
    private Collection<Node> dependentNodes;
    
    @NotPersistent
    private boolean initialized = false;
    
    @NotPersistent
    private Map<String, NodeLogicParameter> parameters;

    public BaseNode(Class[] childNodeTypes, boolean container, boolean readOnly)
    {
        this.childNodeTypes = childNodeTypes;
        this.container = container;
        this.readOnly = readOnly;
    }

    public long getId() 
    {
        return id;
    }

    public void setId(long id) 
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
            dependentNodes = new LinkedList<Node>();
        
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
    
    public boolean isReadOnly()
    {
        return readOnly;
    }
    
    public Class<? extends T> getNodeLogicType()
    {
        return nodeLogicType;
    }

    public void setNodeLogicType(Class<? extends T> nodeLogicType) throws ConstraintException
    {
        if (initialized && ObjectUtils.equals(nodeLogicType.getName(), nodeLogicTypeName))
        {
            nodeLogicTypeName = nodeLogicType.getName();
            this.nodeLogicType = nodeLogicType;
            if (nodeLogicType!=null)
                createNodeLogic();
            syncAttributesAndParameters();
        }else
            this.nodeLogicType = nodeLogicType;
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
        return nodeAttributes.values();
    }

    public NodeAttribute getNodeAttribute(String name)
    {
        return nodeAttributes==null? null : nodeAttributes.get(name);
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
    
    public void init() throws NodeInitializationError
    {
        boolean dependenciesInitialized = true;
        if (nodeAttributes!=null)
        {
            for (NodeAttribute attr: nodeAttributes.values())
                if (Node.class.isAssignableFrom(attr.getType()))
                {
                    Node node = converter.convert(Node.class, attr.getValue(), null);
                    node.addDependentNode(this);
                    if (!node.isInitialized())
                        dependenciesInitialized = false;
                }
        }
            
        if (!dependenciesInitialized)
            return;
            
        if (nodeLogicTypeName!=null)
        {
            try
            {
                nodeLogicType = (Class<T>) Class.forName(nodeLogicTypeName);
                createNodeLogic();
                syncAttributesAndParameters();
            } catch (Exception ex)
            {
                throw new NodeInitializationError(String.format(
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

    private void createNodeLogic() throws NodeInitializationError
    {
        try
        {
            nodeLogic = nodeLogicType.newInstance();
            nodeLogic.setOwner(this);
            extractNodeLogicParameters();            
        } catch (Exception e)
        {
            throw new NodeInitializationError(
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

    private void syncAttributesAndParameters() throws ConstraintException
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
    {
        try
        {
            NodeAttributeImpl attr = new NodeAttributeImpl();
            attr.setName(param.getDisplayName());
            attr.setParameterName(param.getName());
            attr.setParameter(param);
            attr.setDescription(param.getDescription());
            attr.setValue(converter.convert(String.class, param.getValue(), param.getPattern()));
            attr.setType(param.getType());
            attr.setOwner(this);

            nodeAttributes.put(attr.getName(), attr);
        } catch (ConstraintException ex)
        {
        }
    }
}

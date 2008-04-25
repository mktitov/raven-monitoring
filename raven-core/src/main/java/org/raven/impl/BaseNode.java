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

package org.raven.impl;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.raven.AttributesGenerator;
import org.raven.Node;
import org.raven.NodeAttribute;
import org.raven.NodeInitializationError;
import org.raven.NodeLogic;
import org.raven.NodeLogicParameter;
import org.raven.annotations.Parameter;
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
    
    private String name;
    private final Class[] childNodeTypes;
    private List<Node> childrens;
    private List<NodeAttribute> nodeAttributes;
    private Class<? extends T> nodeLogicType;
    private T nodeLogic;
    private int initializationPriority;
    private Node parentNode;
    
    private Collection<Node> dependentNodes;
    
    private boolean initialized = false;
    
    private Map<String, NodeLogicParameter> parameters;

    public BaseNode(Class[] childNodeTypes)
    {
        this.childNodeTypes = childNodeTypes;
    }

    public void addDependentNode(Node dependentNode)
    {
        if (dependentNodes==null)
            dependentNodes = new LinkedList<Node>();
        dependentNodes.add(dependentNode);
    }

    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public Class<? extends T> getNodeLogicType()
    {
        return nodeLogicType;
    }

    public void setNodeLogicType(Class<? extends T> nodeLogicType) throws ConstraintException
    {
        if (initialized && nodeLogicType != this.nodeLogicType)
        {
            this.nodeLogicType = nodeLogicType;
            if (nodeLogicType!=null)
                createNodeLogic();
            syncAttributesAndParameters();
        }else
            this.nodeLogicType = nodeLogicType;
    }

    public Class[] getChildNodeTypes()
    {
        return childNodeTypes;
    }

    public List<Node> getChildrens()
    {
        return childrens;
    }

    public int getInitializationPriority()
    {
        return initializationPriority;
    }

    public List<NodeAttribute> getNodeAttributes()
    {
        return nodeAttributes;
    }

    public T getNodeLogic()
    {
        return nodeLogic;
    }

    public Node getParentNode()
    {
        return parentNode;
    }

    public void setParentNode(Node parentNode)
    {
        this.parentNode = parentNode;
    }

    public String getPath()
    {
        StringBuffer path = new StringBuffer(name);
        Node parent;
        while ( (parent=getParentNode()) != null )
            path.insert(0, parent.getName()+"/");
        
        return path.toString();
    }

    public boolean isInitialized()
    {
        return initialized;
    }
    
    public void init() throws NodeInitializationError
    {
        if (nodeAttributes!=null)
            for (NodeAttribute attr: nodeAttributes)
                if (Node.class.isAssignableFrom(attr.getType()))
                {
                    Node node = converter.convert(Node.class, attr.getValue(), null);
                    if (!node.isInitialized())
                    {
                        node.addDependentNode(this);
                        break;
                    }
                }
            
        if (nodeLogicType!=null)
        {
            try
            {
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
                node.init();
            dependentNodes = null;
        }
        
    }

    void fireAttributeValueChanged(NodeAttributeImpl attr)
    {
        if (attr.isGeneratorType())
        {
            ListIterator<NodeAttribute> it = nodeAttributes.listIterator();
            while (it.hasNext())
            {
                NodeAttribute childAttr = it.next();
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
     * contain the attribute.
     * @param attributeName
     * @return
     */
    String getParentNodeAttributeValue(String attributeName)
    {
        Node parent;
        while ( (parent=getParentNode())!=null )
        {
            
        }
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
            ListIterator<NodeAttribute> it = nodeAttributes.listIterator();
            while (it.hasNext())
            {
                NodeAttribute attr = it.next();
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

            nodeAttributes.add(attr);
        } catch (ConstraintException ex)
        {
        }
    }
}

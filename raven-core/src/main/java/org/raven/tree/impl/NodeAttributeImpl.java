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
import java.util.List;
import org.raven.conf.Configurator;
import org.raven.tree.AttributeReference;
import org.raven.tree.AttributeValueHandlerListener;
import org.raven.tree.AttributesGenerator;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeAttributeError;
import org.raven.tree.NodeParameter;
import org.raven.tree.Tree;
import org.weda.beans.ObjectUtils;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueImpl;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NodeAttributeImpl 
        implements NodeAttribute, Cloneable, AttributeValueHandlerListener//, NodeAttributeListener
{
    @Service
    private static TypeConverter converter;
    @Service
    private static Configurator configurator;
    @Service
    private static Tree tree;

    private int id;
    private String name;
    private String parameterName;
    private String description;
    private String parentAttribute;
    private Class type;
    private String value;
    private String valueHandlerType;
    
    private boolean required;
    private BaseNode owner;
    //TODO: remove fild parameter 
    private NodeParameter parameter;
    //TODO: remove fild attributeReference 
    private AttributeReference attributeReference;
    private ParentAttributeValueHandler valueHandler;
    
    private boolean initialized = false;

    public NodeAttributeImpl()
    {
    }

    public NodeAttributeImpl(String name, Class type, Object value, String description)
    {
        this.name = name;
        this.description = description;
        this.type = type;
        this.value = converter.convert(String.class, value, null);
    }
    
    public void init() throws Exception
    {
        valueHandler = new ParentAttributeValueHandler(this);
        valueHandler.setWrappedHandlerType(valueHandlerType);
        valueHandler.addListener(this);
//        valueHandler.setData(value);
        initialized = true;
    }
    
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public void setParameter(NodeParameter parameter)
    {
        this.parameter = parameter;
    }

    public String getName()
    {
        return name;
    }

    public String getParameterName()
    {
        return parameterName;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isRequired()
    {
        if (parameter==null)
            return required;
        else
            return parameter.isRequired();
    }

    public void setRequired(boolean required)
    {
        this.required = required;
    }

    public <T> T getRealValue()
    {
        return (T) valueHandler.handleData();
//        if (isAttributeReference())
//            return attributeReference == null ? 
//                null : (T)attributeReference.getAttribute().getRealValue();
//        else {
//            if (parameter!=null)
//            {
//                return (T)parameter.getData();
//            }
//            else
//            {
//                if (value==null)
//                    return (T) converter.convert(
//                            type, owner.getParentAttributeRealValue(name), null);
//                else
//                    return (T) converter.convert(type, value, null);
//            }
//        }
    }

    public String getValue()
    {
        return converter.convert(String.class, valueHandler.handleData(), null);
//        
//        if (owner.getStatus()==Status.CREATED)
//            return value;
//        
//        if (isAttributeReference())
//        {
//            if (value==null)
//                return null;
//            else
//                return attributeReference.getAttribute().getValue();
//        } else
//        {
//            if (parameter!=null)
//                return converter.convert(
//                        String.class, parameter.getValue(), parameter.getPattern());
//            else if (value!=null)
//                return value;
//            else
//                return owner.getParentAttributeValue(name);
//        }
    }

    public String getRawValue()
    {
        return valueHandler==null? value : valueHandler.getData();
    }
    
    public void setRawValue(String rawValue)
    {
        this.value = rawValue;
    }
    
    public Class getType() 
    {
//        if (isAttributeReference())
//            return getAttributeReference()==null? 
//                null : attributeReference.getAttribute().getType();
        return type;
    }

    public String getValueHandlerType()
    {
        return valueHandlerType;
    }

    public void setValueHandlerType(String valueHandlerType) throws Exception
    {
        this.valueHandlerType = valueHandlerType;
        if (initialized)
            valueHandler.setWrappedHandlerType(valueHandlerType);
    }
    
    public String getParentAttribute()
    {
        return parentAttribute;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setName(String name)
    {
        if (name==null)
            throw new NodeAttributeError("The name of the attribute can not be empty");
        
        if (!ObjectUtils.equals(this.name, name))
        {
            if (   this.name!=null 
                && ObjectUtils.in(owner.getStatus(), Status.INITIALIZED, Status.STARTED))
            {
                if (owner.getNodeAttribute(name)!=null)
                    throw new NodeAttributeError(String.format(
                            "The attribute with name (%s) is already exists in the node (%s)"
                            , name, owner.getPath()));
                
                String oldValue = this.name;
                this.name = name;
                owner.fireAttributeNameChanged(this, oldValue, this.name);
            } else
                this.name = name;
        }
    }

    public Node getOwner()
    {
        return owner;
    }

    public void setOwner(Node owner)
    {
        this.owner = (BaseNode) owner;
    }

    public void setParameterName(String parameterName)
    {
        this.parameterName = parameterName;
    }

    public void setParentAttribute(String parentAttribute)
    {
        this.parentAttribute = parentAttribute;
    }

    public void setType(Class type)
    {
        this.type = type;
    }

    public void setValue(String value) throws Exception
    {
        if (initialized)
            valueHandler.setData(value);
        else
            this.value = value;
//        if (!ObjectUtils.equals(this.value, value))
//        {
//            String oldValue = this.value;
//            this.value = value;
//            valueHandler.setData(value);
//            
//            if (owner.getStatus()!=Status.CREATED)
//            {
//                owner.fireAttributeValueChanged(this, oldValue, oldValue);
//            }
            
//            if (isAttributeReference())
//            {
//                processAttributeReference(value, oldValue);
//            }
//            else {
//                if (owner.getStatus()!=Status.CREATED)
//                {
//                    if (parameter!=null)
//                        parameter.setData(value);
//
//                    owner.fireAttributeValueChanged(this, oldValue, value);
//                }
//            }
//        }
    }

    public boolean isGeneratorType()
    {
//        if (isAttributeReference())
//            return getAttributeReference()==null? 
//                false : attributeReference.getAttribute().isGeneratorType();
        return type!=null && AttributesGenerator.class.isAssignableFrom(type);
    }

    //TODO: привести к виду List<ReferenceValue>
    public List<ReferenceValue> getReferenceValues() throws TooManyReferenceValuesException
    {
        if (!valueHandler.isReferenceValuesSupported())
            return null;
        
        List<ReferenceValue> result = null;
        if (parameter!=null)
        {
                List<ReferenceValue> parameterRefValues = parameter.getReferenceValues();
                if (parameterRefValues!=null)
                {
                    result = new ArrayList<ReferenceValue>(parameterRefValues.size());
                    for (ReferenceValue refValue: parameterRefValues)
                    {
                        ReferenceValue value = new ReferenceValueImpl(
                                converter.convert(String.class, refValue.getValue(), null)
                                , refValue.getValueAsString());
                        result.add(value);
                    }
                }
        }
        if (result==null)
            result = tree.getReferenceValuesForAttribute(this);
        
        return result;
    }

    public void save()
    {
        configurator.getTreeStore().saveNodeAttribute(this);
    }

    public void shutdown() 
    {
        if (valueHandler!=null)
            valueHandler.close();
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        NodeAttributeImpl clone = (NodeAttributeImpl) super.clone();
        clone.id = 0;
        clone.owner = null;
        clone.valueHandler = null;
        clone.initialized = false;
        clone.value = getRawValue();
        return clone;
    }

//    public boolean isAttributeReference()
//    {
//        return type==null? false : AttributeReference.class.isAssignableFrom(type);
//    }

    public boolean isExpression()
    {
        return valueHandler==null? false : valueHandler.isExpressionSupported();
    }

    public boolean isExpressionValid()
    {
        return valueHandler.isExpressionValid();
    }

    public void validateExpression() throws Exception
    {
        valueHandler.validateExpression();
    }
    
//    public AttributeReference getAttributeReference()
//    {
//        if (attributeReference==null && value!=null && isAttributeReference())
//        {
//            attributeReference = (AttributeReference) converter.convert(type, value, null);
//            attributeReference.getAttribute().getOwner().addNodeAttributeDependency(
//                    attributeReference.getAttribute().getName(), this);
//        }
//        return attributeReference;
//    }
//    
//    private void processAttributeReference(String newValue, String oldValue)
//    {
//        AttributeReference oldRef = null;
//        String oldRefValue = null;
//        String newRefValue = null;
//        if (attributeReference!=null)
//        {
//            oldRef = attributeReference;
//            oldRefValue = oldRef.getAttribute().getValue();
//            attributeReference.getAttribute().getOwner().removeNodeAttributeDependency(
//                    attributeReference.getAttribute().getName(), this);
//        }
//        if (newValue!=null && owner.getStatus()!=Status.CREATED)
//        {
//            attributeReference = (AttributeReference) converter.convert(type, newValue, null);
//            newRefValue = attributeReference.getAttribute().getValue();
//            attributeReference.getAttribute().getOwner().addNodeAttributeDependency(
//                    attributeReference.getAttribute().getName(), this);
//        } else
//            attributeReference = null;
//        
//        if (   oldRef!=null && oldRef.getAttribute().isGeneratorType() 
//            && (attributeReference==null || !attributeReference.getAttribute().isGeneratorType()))
//        {
//            owner.removeChildAttributes(name, null);
//        }
//        
//        if (    !ObjectUtils.equals(oldRefValue, newRefValue) 
//            || (attributeReference!=null && attributeReference.getAttribute().isGeneratorType()))
//        {
//            owner.fireAttributeValueChanged(this, oldRefValue, newRefValue);
//        }
//    }

//    public void nodeAttributeValueChanged(
//            Node node, NodeAttribute attribute, Object oldValue, Object newValue)
//    {
//        owner.fireAttributeValueChanged(this, oldValue, newValue);
//    }

//    public void nodeAttributeRemoved(Node node, NodeAttribute attribute)
//    {
//        try
//        {
//            setValue(null);
//        } catch (ConstraintException ex)
//        {
//            throw new NodeAttributeError(ex);
//        }
//    }
//
//    public void nodeAttributeNameChanged(NodeAttribute attribute, String oldName, String newName)
//    {
//        value = converter.convert(String.class, attributeReference, null);
//        configurator.getTreeStore().saveNodeAttribute(this);
//    }

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
        final NodeAttributeImpl other = (NodeAttributeImpl) obj;
        if (this.id != other.id)
        {
            return false;
        }
        if (this.name != other.name && (this.name == null || !this.name.equals(other.name)))
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 79 * hash + this.id;
        return hash;
    }

    public void valueChanged(Object oldValue, Object newValue)
    {
        owner.fireAttributeValueChanged(this, oldValue, newValue);
    }

    public void expressionInvalidated(Object oldValue)
    {
        owner.fireAttributeValueChanged(this, oldValue, null);
    }
}

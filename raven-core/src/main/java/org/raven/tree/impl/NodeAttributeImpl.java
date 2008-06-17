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
import org.raven.tree.AttributesGenerator;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeAttributeError;
import org.raven.tree.NodeAttributeListener;
import org.raven.tree.NodeParameter;
import org.raven.tree.Tree;
import org.weda.beans.ObjectUtils;
import org.weda.constraints.ConstraintException;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.internal.annotations.Service;
import org.weda.internal.exception.NullParameterError;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NodeAttributeImpl implements NodeAttribute, Cloneable, NodeAttributeListener
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
    private NodeParameter parameter;
    private AttributeReference attributeReference;

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
        if (isAttributeReference())
            return attributeReference == null ? 
                null : (T)attributeReference.getAttribute().getRealValue();
        else {
            if (parameter!=null)
            {
                return (T)parameter.getValue();
            }
            else
            {
                if (value==null)
                    return (T) converter.convert(
                            type, owner.getParentAttributeRealValue(name), null);
                else
                    return (T) converter.convert(type, value, null);
            }
        }
    }

    public String getValue()
    {
        NullParameterError.check("owner", owner);
        
        if (owner.getStatus()==Status.CREATED)
            return value;
        
        if (isAttributeReference())
        {
            if (value==null)
                return null;
            else
                return attributeReference.getAttribute().getValue();
        } else
        {
            if (parameter!=null)
                return converter.convert(
                        String.class, parameter.getValue(), parameter.getPattern());
            else if (value!=null)
                return value;
            else
                return owner.getParentAttributeValue(name);
        }
    }

    public String getRawValue()
    {
        return value;
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

    public void setValueHandlerType(String valueHandlerType)
    {
        this.valueHandlerType = valueHandlerType;
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
            if (this.name!=null && owner.getStatus()!=Status.CREATED)
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

    public void setValue(String value) throws ConstraintException
    {
        if (!ObjectUtils.equals(this.value, value))
        {
            String oldValue = this.value;
            this.value = value;
            if (isAttributeReference())
            {
                processAttributeReference(value, oldValue);
            }
            else {
                if (owner.getStatus()!=Status.CREATED)
                {
                    if (parameter!=null)
                        parameter.setValue(value);

                    owner.fireAttributeValueChanged(this, oldValue, value);
                }
            }
        }
    }

    public boolean isGeneratorType()
    {
        if (isAttributeReference())
            return getAttributeReference()==null? 
                false : attributeReference.getAttribute().isGeneratorType();
        return type!=null && AttributesGenerator.class.isAssignableFrom(type);
    }

    public List<String> getReferenceValues()
    {
        List<String> result = null;
        if (parameter!=null && !isAttributeReference())
        {
            try
            {
                List<ReferenceValue> refValues = parameter.getReferenceValues();
                if (refValues!=null)
                {
                    result = new ArrayList<String>(refValues.size());
                    for (ReferenceValue refValue: refValues)
                        result.add(converter.convert(
                            String.class, refValue.getValue(), parameter.getPattern()));
                }
            } catch (TooManyReferenceValuesException ex)
            {
            }
        }
        if (result==null)
            result = tree.getReferenceValuesForAttribute(this);
        
        return result;
        
//        if (isAttributeReference())
//            return null;
//        if (parameter==null)
//            return null;
//        else {
//            try
//            {
//                List<ReferenceValue> refValues = parameter.getReferenceValues();
//                if (refValues==null)
//                    return null;
//                List<String> values = new ArrayList<String>(refValues.size());
//                for (ReferenceValue refValue: refValues)
//                    values.add(converter.convert(
//                        String.class, refValue.getValue(), parameter.getPattern()));
//                return values;
//            } catch (TooManyReferenceValuesException ex)
//            {
//                return null;
//            }
//        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        NodeAttributeImpl clone = (NodeAttributeImpl) super.clone();
        clone.id = 0;
        clone.owner = null;
        
        return clone;
    }

    public boolean isAttributeReference()
    {
        return type==null? false : AttributeReference.class.isAssignableFrom(type);
    }

    public boolean isExpression()
    {
        return valueHandlerType==null? false : true;
    }
    
    public AttributeReference getAttributeReference()
    {
        if (attributeReference==null && value!=null && isAttributeReference())
        {
            attributeReference = (AttributeReference) converter.convert(type, value, null);
            attributeReference.getAttribute().getOwner().addNodeAttributeDependency(
                    attributeReference.getAttribute().getName(), this);
        }
        return attributeReference;
    }
    
    private void processAttributeReference(String newValue, String oldValue)
    {
        AttributeReference oldRef = null;
        String oldRefValue = null;
        String newRefValue = null;
        if (attributeReference!=null)
        {
            oldRef = attributeReference;
            oldRefValue = oldRef.getAttribute().getValue();
            attributeReference.getAttribute().getOwner().removeNodeAttributeDependency(
                    attributeReference.getAttribute().getName(), this);
        }
        if (newValue!=null && owner.getStatus()!=Status.CREATED)
        {
            attributeReference = (AttributeReference) converter.convert(type, newValue, null);
            newRefValue = attributeReference.getAttribute().getValue();
            attributeReference.getAttribute().getOwner().addNodeAttributeDependency(
                    attributeReference.getAttribute().getName(), this);
        } else
            attributeReference = null;
        
        if (   oldRef!=null && oldRef.getAttribute().isGeneratorType() 
            && (attributeReference==null || !attributeReference.getAttribute().isGeneratorType()))
        {
            owner.removeChildAttributes(name, null);
        }
        
        if (    !ObjectUtils.equals(oldRefValue, newRefValue) 
            || (attributeReference!=null && attributeReference.getAttribute().isGeneratorType()))
        {
            owner.fireAttributeValueChanged(this, oldRefValue, newRefValue);
        }
    }

    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, String oldValue, String newValue)
    {
        owner.fireAttributeValueChanged(this, oldValue, newValue);
    }

    public void nodeAttributeRemoved(Node node, NodeAttribute attribute)
    {
        try
        {
            setValue(null);
        } catch (ConstraintException ex)
        {
            throw new NodeAttributeError(ex);
        }
    }

    public void nodeAttributeNameChanged(NodeAttribute attribute, String oldName, String newName)
    {
        value = converter.convert(String.class, attributeReference, null);
        configurator.getTreeStore().saveNodeAttribute(this);
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
    
}

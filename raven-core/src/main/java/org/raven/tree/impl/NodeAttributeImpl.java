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

import org.raven.tree.AttributesGenerator;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeParameter;
import org.weda.beans.ObjectUtils;
import org.weda.constraints.ConstraintException;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NodeAttributeImpl implements NodeAttribute, Cloneable
{
    @Service
    private TypeConverter converter;
    
    private int id;
    private String name;
    private String parameterName;
    private String description;
    private String parentAttribute;
    private Class type;
    private String value;
    private boolean required;
    
    private BaseNode owner;
    private NodeParameter parameter;

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
        if (parameter!=null)
        {
            return (T)parameter.getValue();
        }
        else
        {
            if (value==null)
                return (T) owner.getParentAttributeRealValue(name);
            else
                return (T) converter.convert(type, value, null);
        }
    }

    public String getValue()
    {
        if (value!=null)
            return value;
        else
            return owner.getParentAttributeValue(name);
    }

    public Class getType() 
    {
        return type;
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
        this.name = name;
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
            this.value = value;
            
            if (owner.isInitialized())
            {
                if (parameter!=null)
                    parameter.setValue(value);

                owner.fireAttributeValueChanged(this);
            }
        }
    }

    public boolean isGeneratorType()
    {
        return type!=null && AttributesGenerator.class.isAssignableFrom(type);
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        NodeAttributeImpl clone = (NodeAttributeImpl) super.clone();
        
        return clone;
    }
    
    
}

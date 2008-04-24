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

package org.raven.impl;

import org.raven.NodeAttribute;
import org.raven.NodeLogicParameter;
import org.weda.beans.ObjectUtils;
import org.weda.constraints.ConstraintException;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NodeAttributeImpl implements NodeAttribute
{
    @Service
    private TypeConverter converter;
    
    private String name;
    private String parameterName;
    private String description;
    private NodeAttribute parentAttribute;
    private Class type;
    private String value;
    
    private BaseNode owner;
    private NodeLogicParameter parameter;

    public void setParameter(NodeLogicParameter parameter)
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

    public String getValue()
    {
        return value;
    }

    public Class getType()
    {
        return type;
    }

    public NodeAttribute getParentAttribute()
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

    public void setOwner(BaseNode owner)
    {
        this.owner = owner;
    }

    public void setParameterName(String parameterName)
    {
        this.parameterName = parameterName;
    }

    public void setParentAttribute(NodeAttribute parentAttribute)
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
            
            if (parameter!=null && owner.isInitialized())
                parameter.setValue(value);
            
            owner.fireAttributeValueChanged(this);
        }
    }
}

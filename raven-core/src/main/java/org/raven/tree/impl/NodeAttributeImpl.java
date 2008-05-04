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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import org.raven.tree.AttributesGenerator;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeAttributeError;
import org.raven.tree.NodeLogicParameter;
import org.weda.beans.ObjectUtils;
import org.weda.constraints.ConstraintException;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
@PersistenceCapable(detachable="true", identityType=IdentityType.DATASTORE)
public class NodeAttributeImpl implements NodeAttribute
{
    @Service
    private TypeConverter converter;
    
    @Persistent
    private String name;
    @Persistent
    private String parameterName;
    @Persistent
    private String description;
    @Persistent
    private String parentAttribute;
    @NotPersistent
    private Class type;
    @Persistent
    private String typeName;
    @Persistent
    private String value;
    
    @Persistent()
    private BaseNode owner;
    @NotPersistent
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
        try
        {
            if (type == null && typeName != null)
            {
                type = Class.forName(typeName);
            }
            return type;
        } catch (ClassNotFoundException ex)
        {
            throw new NodeAttributeError("Invalid attribute type", ex.getCause());
        }
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
        typeName = type.getName();
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
    
    
}

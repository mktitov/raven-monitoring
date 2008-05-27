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
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeParameter;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.GetOperation;
import org.weda.beans.PropertyDescriptor;
import org.weda.beans.SetOperation;
import org.weda.constraints.ConstraintException;
import org.weda.internal.annotations.Service;
import org.weda.services.PropertyOperationCompiler;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NodeParameterImpl implements NodeParameter
{
    @Service
    private PropertyOperationCompiler operationCompiler;
    @Service
    private TypeConverter converter;
    
    private final Node node;
    private final String name;        
    
    private PropertyDescriptor propertyDescriptor;
    private NodeAttribute nodeAttribute;
    private GetOperation getter;
    private SetOperation setter;
    private boolean required = false;

    public NodeParameterImpl(Node node, PropertyDescriptor desc)
    {
        this.node = node;
        this.name = desc.getName();
        
        propertyDescriptor = desc;
        getter = operationCompiler.compileGetOperation(node.getClass(), name);
        setter = operationCompiler.compileSetOperation(node.getClass(), name);
        
        for (Annotation ann: propertyDescriptor.getAnnotations())
            if (ann instanceof NotNull)
            {
                required = true;
                break;
            }
    }

    public String getName()
    {
        return name;
    }

    public String getDisplayName()
    {
        return propertyDescriptor.getDisplayName();
    }

    public String getDescription()
    {
        return propertyDescriptor.getDescription();
    }

    public Class getType()
    {
        return propertyDescriptor.getType().isPrimitive()? 
            propertyDescriptor.getWrapperType() : propertyDescriptor.getType();
    }

    public boolean isRequired()
    {
        return required;
    }

    public Object getValue()
    {
        return getter.getValue(node);
    }

    public void setValue(Object value) throws ConstraintException
    {
        Object val = converter.convert(getType(), value, getPattern());
        if (node.getStatus()!=Status.CREATED)
            propertyDescriptor.check(val);
        setter.setValue(node, val);
    }

    public PropertyDescriptor getPropertyDescriptor()
    {
        return propertyDescriptor;
    }
    
    public void setNodeAttribute(NodeAttribute nodeAttribute)
    {
        this.nodeAttribute = nodeAttribute;
    }

    public NodeAttribute getNodeAttribute()
    {
        return nodeAttribute;
    }

    public String getPattern()
    {
        return propertyDescriptor.getPattern();
    }
    
}

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

import org.raven.tree.AttributeValueHandler;
import org.raven.tree.AttributeValueHandlerListener;
import org.raven.tree.AttributeValueHandlerRegistry;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeAttributeListener;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class ParentAttributeValueHandler 
        extends AbstractAttributeValueHandler 
        implements AttributeValueHandlerListener, NodeAttributeListener
{
    @Service
    private static AttributeValueHandlerRegistry handlerRegistry;
    @Service
    private static TypeConverter converter;
    
    private AttributeValueHandler wrappedHandler;
    private NodeAttribute parentAttribute;
    private String data;
    private String wrappedHandlerType;

    public ParentAttributeValueHandler(NodeAttribute attribute)
    {
        super(attribute);
        data = attribute.getRawValue();
    }

    public AttributeValueHandler getWrappedHandler()
    {
        return wrappedHandler;
    }

    public void setWrappedHandlerType(String handlerType) throws Exception
    {
        if (!ObjectUtils.equals(this.wrappedHandlerType, handlerType))
        {
            cleanupWrappedHandler();
            this.wrappedHandlerType = handlerType;
            if (handlerType==null)
                wrappedHandler = null;
            else 
                wrappedHandler = handlerRegistry.getValueHandler(wrappedHandlerType, attribute);
            
            if (wrappedHandler!=null)
            {
                wrappedHandler.addListener(this);
//                if (data!=null)
//                    wrappedHandler.setData(data);
            }
        }
    }

    public void setData(String data) throws Exception
    {
        if (!ObjectUtils.equals(this.data, data))
        {
            String oldData = this.data;
            this.data = data;
            if (wrappedHandler!=null)
                wrappedHandler.setData(data);
            else
            {
                Object oldValue = converter.convert(attribute.getType(), oldData, null);
                Object newValue = converter.convert(attribute.getType(), this.data, null);
                fireValueChangedEvent(oldValue, newValue);
            }
        }
    }

    public String getData()
    {
        if (wrappedHandler==null)
            return data;
        else
            return wrappedHandler.getData();
    }

    public Object handleData()
    {
        if (wrappedHandler==null && data != null)
        {
            cleanupParentAttribute();
            return converter.convert(attribute.getType(), data, null);
        }
        else
        {
            Object result = wrappedHandler==null? null : wrappedHandler.handleData();
            if (result!=null)
            {
                cleanupParentAttribute();
                return result;
            } 
            else
                return getParentAttribute()==null? null : getParentAttribute().getRealValue();
        }
    }

    public void close()
    {
        cleanupParentAttribute();
        cleanupWrappedHandler();
    }

    public boolean canHandleValue()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isReferenceValuesSupported()
    {
        return wrappedHandler==null 
                || (wrappedHandler!=null && wrappedHandler.isReferenceValuesSupported());
    }

    public boolean isExpressionSupported()
    {
        return wrappedHandler!=null && wrappedHandler.isExpressionSupported();
    }

    public void valueChanged(Object oldValue, Object newValue)
    {
        fireValueChangedEvent(oldValue, newValue);
    }

    private void cleanupParentAttribute()
    {
        if (parentAttribute!=null)
        {
            parentAttribute.getOwner().removeNodeAttributeDependency(attribute.getName(), this);
            parentAttribute = null;
        }
    }
    
    private void cleanupWrappedHandler()
    {
        if (wrappedHandler!=null)
            wrappedHandler.close();
    }

    private NodeAttribute getParentAttribute()
    {
        if (parentAttribute==null)
        {
            parentAttribute = attribute.getOwner().getParentAttribute(attribute.getName());
            if (parentAttribute!=null)
                parentAttribute.getOwner().addNodeAttributeDependency(attribute.getName(), this);
        }
        return parentAttribute;
    }

    public void nodeAttributeNameChanged(NodeAttribute attribute, String oldName, String newName)
    {
    }

    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue)
    {
        fireValueChangedEvent(oldValue, newValue);
    }

    public boolean nodeAttributeRemoved(Node node, NodeAttribute attribute)
    {
        Object oldValue = handleData();
        cleanupParentAttribute();
        Object newValue = handleData();
        if (!ObjectUtils.equals(newValue, oldValue))
            fireValueChangedEvent(oldValue, newValue);
        return false;
    }

    public boolean isExpressionValid()
    {
        return wrappedHandler==null? true : wrappedHandler.isExpressionValid();
    }

    public void validateExpression() throws Exception
    {
        if (wrappedHandler!=null)
            wrappedHandler.validateExpression();
    }

    public void expressionInvalidated(Object oldValue)
    {
        fireExpressionInvalidatedEvent(oldValue);
    }
}

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

import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;
/**
 *
 * @author Mikhail Titov
 */
public class AttributeReferenceValueHandler extends AttributeReferenceHandler
{
    @Service
    private static TypeConverter converter;
    
    private Object attrValue;

    public AttributeReferenceValueHandler(NodeAttribute attribute) throws Exception
    {
        super(attribute);
        try{
            if (node!=null && ObjectUtils.in(node.getStatus(), Status.INITIALIZED, Status.STARTED))
            {
                attrValue = referencedAttribute==null? 
                    null : convertValue(referencedAttribute.getRealValue());
            }
        }catch(Exception e)
        {
            attribute.getOwner().getLogger().warn(
                    "Error setting expression", e);
        }
    }

    @Override
    public void setData(String data) throws Exception
    {
        NodeAttribute oldReferencedAttribute = referencedAttribute;
        super.setData(data);
        
        if (node!=null && ObjectUtils.in(node.getStatus(), Status.INITIALIZED, Status.STARTED))
        {
            attrValue = referencedAttribute==null? 
                null : convertValue(referencedAttribute.getRealValue());
            Object oldAttrValue = oldReferencedAttribute==null? 
                null : convertValue(oldReferencedAttribute.getRealValue());
            if (!ObjectUtils.equals(oldAttrValue, attrValue))
                fireValueChangedEvent(oldAttrValue, attrValue);
        }
    }

    @Override
    public Object handleData()
    {
        return attrValue;
    }

    @Override
    public void nodeStatusChanged(Node sourceNode, Status oldStatus, Status newStatus)
    {
        if (sourceNode.equals(node) && oldStatus==Node.Status.CREATED)
        {
            attrValue = convertValue(referencedAttribute.getRealValue());
            if (attrValue!=null)
                fireValueChangedEvent(null, attrValue);
        }
    }

    @Override
    public boolean nodeAttributeRemoved(Node node, NodeAttribute removedAttribute)
    {
        if (ObjectUtils.equals(referencedAttribute, removedAttribute))
        {
            referencedAttribute = null;
            cleanupNodeReference(this.node, node);
            expressionValid = false;
            this.node = null;
            Object oldAttrValue = attrValue;
            attrValue = null;
            fireExpressionInvalidatedEvent(oldAttrValue);
        }
        return true;
    }

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldRealValue, Object newRealValue)
    {
        if (ObjectUtils.equals(referencedAttribute, attribute))
        {
            Object newAttrValue = convertValue(newRealValue);
            if (!ObjectUtils.equals(attrValue, newAttrValue))
            {
                Object oldAttrValue = attrValue;
                attrValue = newAttrValue;
                fireValueChangedEvent(oldAttrValue, attrValue);
            }
        }
    }
    
    private Object convertValue(Object val)
    {
        return converter.convert(attribute.getType(), val, null);
    }
}

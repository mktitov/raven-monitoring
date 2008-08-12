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

import org.raven.tree.AttributeNotFoundException;
import org.raven.tree.InvalidPathException;
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
public class AttributeReferenceValueHandler extends NodeReferenceValueHandler
{
    @Service
    private static TypeConverter converter;
    
    private String attrData = null;
    private NodeAttribute referencedAttribute = null;
    private Object attrValue;

    public AttributeReferenceValueHandler(NodeAttribute attribute) throws Exception
    {
        super(attribute, false, true);
        try{
            resolveAttribute(attribute.getRawValue(), true);
            initNodeReference(node, pathElements);
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

    public NodeAttribute getReferencedAttribute() 
    {
        return referencedAttribute;
    }
    
    private void resolveAttribute(String data, boolean init) throws InvalidPathException, Exception
    {
        if (ObjectUtils.equals(attrData, data) && expressionValid)
            return;
        if (data!=null)
        {
            int pos = data.lastIndexOf(Node.ATTRIBUTE_SEPARATOR);
            if (pos<0)
                throw new InvalidPathException(String.format(
                        "Invalid path (%s) to the attribute. " +
                        "Attribute separator symbol (%s) not found"
                        , data, Node.ATTRIBUTE_SEPARATOR));
            String pathToNode = data.substring(0, pos);
            String attributeName = data.substring(pos+1);

            if (init)
                super.resolveNode(pathToNode);
            else
                super.setData(pathToNode);
            
            referencedAttribute = node.getNodeAttribute(attributeName);
            
            if (referencedAttribute==null)
                throw new AttributeNotFoundException(String.format(
                        "Invalid path (%s) to the attribute. " +
                        "Node (%s) does not contains attribute (%s)"
                        , data, node.getName(), attributeName));
        }
        attrData = data;
    }

    @Override
    public void setData(String data) throws Exception
    {
        String oldAttrData = attrData;
        NodeAttribute oldReferencedAttribute = referencedAttribute;
        resolveAttribute(data, false);
        attrData = data;
        
        if (!ObjectUtils.equals(oldAttrData, attrData))
            attribute.save();
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
    public String getData()
    {
        return attrData;
    }

    @Override
    public Object handleData()
    {
        return attrValue;
    }

    @Override
    public void validateExpression() throws Exception
    {
        setData(attrData);
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
    public void nodeAttributeNameChanged(NodeAttribute attribute, String oldName, String newName)
    {
        if (ObjectUtils.equals(referencedAttribute, attribute))
        {
            attrData = data + Node.ATTRIBUTE_SEPARATOR + newName;
            attribute.save();
        }
    }

    @Override
    public boolean nodeAttributeRemoved(Node node, NodeAttribute removedAttribute)
    {
        if (ObjectUtils.equals(referencedAttribute, removedAttribute))
        {
            referencedAttribute = null;
            Object oldAttrValue = attrValue;
            attrValue = null;
            cleanupNodeReference(this.node, node);
            expressionValid = false;
            this.node = null;
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

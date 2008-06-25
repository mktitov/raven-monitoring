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

    public AttributeReferenceValueHandler(NodeAttribute attribute)
    {
        super(attribute, false);
        slaveMode=true;
    }

    @Override
    public void setData(String data) throws Exception
    {
        if (ObjectUtils.equals(attrData, data) && expressionValid)
            return;
        NodeAttribute oldReferencedAttribute = referencedAttribute;
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

            super.setData(pathToNode);
            
            referencedAttribute = node.getNodeAttribute(attributeName);
            
            if (referencedAttribute==null)
                throw new AttributeNotFoundException(String.format(
                        "Invalid path (%s) to the attribute. " +
                        "Node (%s) does not contains attribute (%s)"
                        , data, node.getName(), attributeName));
        }
        String oldAttrData = attrData;
        attrData = data;
        if (!ObjectUtils.equals(oldAttrData, attrData))
            attribute.save();
        attrValue = referencedAttribute==null? 
            null : convertValue(referencedAttribute.getRealValue());
        Object oldAttrValue = oldReferencedAttribute==null? 
            null : convertValue(oldReferencedAttribute.getRealValue());
        if (!ObjectUtils.equals(oldAttrValue, attrValue))
            fireValueChangedEvent(oldAttrValue, attrValue);
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
    public void nodeAttributeNameChanged(NodeAttribute attribute, String oldName, String newName)
    {
        if (ObjectUtils.equals(referencedAttribute, attribute))
        {
            attrData = data + Node.ATTRIBUTE_SEPARATOR + newName;
            attribute.save();
        }
    }

    @Override
    public void nodeAttributeRemoved(Node node, NodeAttribute removedAttribute)
    {
        if (ObjectUtils.equals(referencedAttribute, removedAttribute))
        {
            referencedAttribute = null;
            Object oldAttrValue = attrValue;
            attrValue = null;
            cleanupNodeReference(node, null);
            expressionValid = false;
            node = null;
            fireExpressionInvalidatedEvent(oldAttrValue);
        }
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

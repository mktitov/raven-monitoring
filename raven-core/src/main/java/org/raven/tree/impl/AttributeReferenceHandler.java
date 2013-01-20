/*
 *  Copyright 2008 tim.
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

/**
 *
 * @author Mikhail Titov
 */
public class AttributeReferenceHandler extends NodeReferenceValueHandler
{
    private String attrData = null;
    protected NodeAttribute referencedAttribute = null;

    public AttributeReferenceHandler(NodeAttribute attribute) throws Exception
    {
        super(attribute, false, true);
        try{
            resolveAttribute(attribute.getRawValue(), true);
            initNodeReference(node, pathElements);
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
    
    private void resolveAttribute(String data, boolean init) throws Exception
    {
//        if (ObjectUtils.equals(attrData, data) && expressionValid)
//        if (ObjectUtils.equals(attrData, data))
//            return;
        referencedAttribute = null;
        if (data!=null) {
            int pos = data.lastIndexOf(Node.ATTRIBUTE_SEPARATOR);
            if (pos<0) {
                attribute.getOwner().getLogger().error(String.format(
                        "Invalid path (%s) to the attribute. " +
                        "Attribute separator symbol (%s) not found"
                        , data, Node.ATTRIBUTE_SEPARATOR));
                return;                
//                throw new InvalidPathException(String.format(
//                        "Invalid path (%s) to the attribute. " +
//                        "Attribute separator symbol (%s) not found"
//                        , data, Node.ATTRIBUTE_SEPARATOR));
            }
            String pathToNode = data.substring(0, pos);
            String attributeName = data.substring(pos+1);

            if (init)
                super.resolveNode(pathToNode);
            else
                super.setData(pathToNode);
            
            if (node!=null) {
                referencedAttribute = node.getNodeAttribute(attributeName);
                if (referencedAttribute==null)
                    attribute.getOwner().getLogger().error(String.format(
                            "Invalid path (%s) to the attribute. " +
                            "Node (%s) does not contains attribute (%s)"
                            , data, node.getName(), attributeName));
//                    throw new AttributeNotFoundException(String.format(
//                            "Invalid path (%s) to the attribute. " +
//                            "Node (%s) does not contains attribute (%s)"
//                            , data, node.getName(), attributeName));
            }
        }
        attrData = data;
    }

    @Override
    public void setData(String data) throws Exception
    {
        String oldAttrData = attrData;
        resolveAttribute(data, false);
//        attrData = data;
        
        if (!ObjectUtils.equals(oldAttrData, attrData))
            attribute.save();
    }

    @Override
    public String getData()
    {
        return attrData;
    }

    @Override
    public Object handleData()
    {
        return referencedAttribute;
    }

    @Override
    public void validateExpression() throws Exception
    {
        setData(attrData);
    }

    @Override
    public void nodeStatusChanged(Node sourceNode, Status oldStatus, Status newStatus)
    {
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
    public void nodeMoved(Node node)
    {
        if (this.node!=null && ObjectUtils.in(node, this.node, attribute.getOwner()))
        {
            String newPath = pathResolver.isPathAbsolute(data)?
                pathResolver.getAbsolutePath(this.node)
                : pathResolver.getRelativePath(attribute.getOwner(), this.node);
            try {
                setData(newPath.substring(0, newPath.length()-1)
                        +Node.ATTRIBUTE_SEPARATOR+referencedAttribute.getName());
            } catch (Exception ex)
            {
                attribute.getOwner().getLogger().error(String.format(
                        "Error reconstructing path for attribute (%s) after move operation"
                        , attribute.getName())
                    , ex);
            }
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
            fireExpressionInvalidatedEvent(attrData);
        }
        return true;
    }

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldRealValue, Object newRealValue)
    {
    }
}

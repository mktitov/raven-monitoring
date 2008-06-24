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

import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.weda.beans.ObjectUtils;
/**
 *
 * @author Mikhail Titov
 */
public class AttributeReferenceValueHandler extends NodeReferenceValueHandler
{
    private String attrData = null;
    private NodeAttribute referencedAttribute = null;

    public AttributeReferenceValueHandler(NodeAttribute attribute)
    {
        super(attribute, false);
    }

    @Override
    public void setData(String data) throws Exception
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
            String attributeName = pathToNode.substring(pos+1);

            super.setData(pathToNode);
            
            referencedAttribute = node.getNodeAttribute(attributeName);
            
            if (referencedAttribute==null)
                throw new InvalidPathException(String.format(
                        "Invalid path (%s) to the attribute. " +
                        "Node (%s) does not contains attribute (%s)"
                        , data, node.getName(), attributeName));
        }
        String oldAttrData = attrData;
        attrData = data;
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
    
    
}

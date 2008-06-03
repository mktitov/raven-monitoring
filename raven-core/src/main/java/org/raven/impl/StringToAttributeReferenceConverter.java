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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import org.raven.tree.AttributeReference;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.AttributeReferenceImpl;
import org.weda.converter.TypeConverterException;
import org.weda.converter.impl.AbstractConverter;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class StringToAttributeReferenceConverter extends AbstractConverter<String, AttributeReference>
{
    @Service
    private static TypeConverter converter;
    
    public AttributeReference convert(String value, Class realTargetType, String format)
    {
        StringTokenizer tokenizer = new StringTokenizer(value, Node.ATTRIBUTE_SEPARATOR);
        try
        {
            String nodePath = tokenizer.nextToken();
            String attrName = tokenizer.nextToken();
            Node node = converter.convert(Node.class, nodePath, null);
            NodeAttribute attr = node.getNodeAttribute(attrName);
            if (attr==null)
                throw new TypeConverterException(String.format(
                        "Node (%s) does not contain attribute (%s)", nodePath, attrName));
            
            return new AttributeReferenceImpl(attr);
        } 
        catch (NoSuchElementException e)
        {
            throw new TypeConverterException(
                    String.format("Invalid attribute reference path - (%s)", value));
        }
    }

    public Class getSourceType()
    {
        return String.class;
    }

    public Class getTargetType()
    {
        return AttributeReference.class;
    }

}

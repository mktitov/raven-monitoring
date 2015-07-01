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

import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
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

    public AttributeReferenceValueHandler(NodeAttribute attribute) throws Exception
    {
        super(attribute);
    }

    @Override
    public Object handleData() {
        if (referencedAttribute==null && attribute.getRawValue()!=null) try {
            setData(attribute.getRawValue());
        } catch (Exception ex) {
            if (attribute.getOwner().isLogLevelEnabled(LogLevel.ERROR))
                attribute.getOwner().getLogger().error(
                        String.format("Error getting value for attribute (%s)", attribute.getName()), ex);
        }
        return referencedAttribute==null? null : converter.convert(
                attribute.getType(), referencedAttribute.getRealValue(), null);
    }
}

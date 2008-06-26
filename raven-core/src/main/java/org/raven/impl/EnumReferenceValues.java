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

import java.util.ArrayList;
import java.util.List;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.NodeAttribute;
import org.weda.constraints.ReferenceValueCollection;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueImpl;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class EnumReferenceValues implements AttributeReferenceValues
{
    @Service
    private static TypeConverter converter;
    
    public List<String> getReferenceValues(NodeAttribute attr)
    {
        Object[] values = attr.getType().getEnumConstants();
        List<String> result = new ArrayList<String>(values.length);
        for (Object value: values)
            result.add(converter.convert(String.class, value, null));
        
        return result;
    }

    public boolean getReferenceValues(NodeAttribute attr, ReferenceValueCollection referenceValues) 
            throws TooManyReferenceValuesException
    {
        if (!attr.getType().isEnum())
            return false;
        
        for (Object value: attr.getType().getEnumConstants())
        {
            String strValue = converter.convert(String.class, value, null);
            referenceValues.add(new ReferenceValueImpl(strValue, strValue), null);
        }
        return true;
    }
}

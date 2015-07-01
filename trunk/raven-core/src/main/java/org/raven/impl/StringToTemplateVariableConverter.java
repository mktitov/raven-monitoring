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

import org.raven.template.impl.TemplateVariable;
import org.raven.tree.AttributeReference;

/**
 *
 * @author Mikhail Titov
 */
public class StringToTemplateVariableConverter extends StringToAttributeReferenceConverter
{
    @Override
    public TemplateVariable convert(String value, Class realTargetType, String format)
    {
        AttributeReference attrRef = super.convert(value, realTargetType, format);
        return new TemplateVariable(attrRef.getAttribute());
    }

    @Override
    public Class getTargetType()
    {
        return TemplateVariable.class;
    }
}

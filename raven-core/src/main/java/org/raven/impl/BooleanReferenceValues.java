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

import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.NodeAttribute;
import org.weda.constraints.ReferenceValueCollection;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueImpl;

/**
 *
 * @author Mikhail Titov
 */
public class BooleanReferenceValues implements AttributeReferenceValues
{
    public boolean getReferenceValues(NodeAttribute attr, ReferenceValueCollection referenceValues)
            throws TooManyReferenceValuesException
    {
        if (Boolean.class.isAssignableFrom(attr.getType()))
        {
            referenceValues.add(new ReferenceValueImpl(Boolean.TRUE.toString(), "true"), null);
            referenceValues.add(new ReferenceValueImpl(Boolean.FALSE.toString(), "false"), null);
            return true;
        } else
            return false;
    }
}

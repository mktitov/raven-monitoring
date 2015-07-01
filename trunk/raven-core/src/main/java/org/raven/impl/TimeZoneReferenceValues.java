/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.NodeAttribute;
import org.weda.constraints.ReferenceValueCollection;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueImpl;

/**
 *
 * @author Mikhail Titov
 */
public class TimeZoneReferenceValues implements AttributeReferenceValues {

    public boolean getReferenceValues(NodeAttribute attr, ReferenceValueCollection refValues)
                throws TooManyReferenceValuesException 
    {
        if (!TimeZone.class.equals(attr.getType()))
            return false;
        
        List<String> timezones = new ArrayList<String>(StringToTimeZoneConverter.TIMEZONES);
        Collections.sort(timezones);
        for (String timezone: timezones)
            refValues.add(new ReferenceValueImpl(timezone, timezone), null);

        return true;
    }
}

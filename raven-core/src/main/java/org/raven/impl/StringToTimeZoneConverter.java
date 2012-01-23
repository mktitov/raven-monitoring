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

import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import org.weda.converter.TypeConverterException;
import org.weda.converter.impl.AbstractConverter;

/**
 *
 * @author Mikhail Titov
 */
public class StringToTimeZoneConverter extends AbstractConverter<String, TimeZone> 
{
    public final static Set<String> TIMEZONES = new HashSet<String>();
    
    static {
        for (String id: TimeZone.getAvailableIDs())
            if (!id.contains("GMT"))
                TIMEZONES.add(id);
        for (int i=-12; i<=12; ++i) {
            TIMEZONES.add(String.format("GMT%+03d:00", i));
            if (Math.abs(i)!=12)
                TIMEZONES.add(String.format("GMT%+03d:30", i));
        }
    }

    public TimeZone convert(String value, Class realTargetType, String format) {
        if (TIMEZONES.contains(value))
            return TimeZone.getTimeZone(value);
        else
            throw new TypeConverterException(String.format("Invalid time zone id (%s)", value));
    }

    public Class getSourceType() {
        return String.class;
    }

    public Class getTargetType() {
        return TimeZone.class;
    }
}

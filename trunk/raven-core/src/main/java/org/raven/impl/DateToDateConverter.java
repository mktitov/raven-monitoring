/*
 *  Copyright 2009 Mikhail Titov.
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

import java.util.Date;
import org.weda.converter.TypeConverterException;
import org.weda.converter.impl.AbstractConverter;

/**
 *
 * @author Mikhail Titov
 */
public class DateToDateConverter extends AbstractConverter<Date, Date>
{

    public DateToDateConverter()
    {
        super(true);
    }

    public Date convert(Date value, Class realTargetType, String format)
    {
        if (java.sql.Date.class.equals(realTargetType))
            return new java.sql.Date(value.getTime());
        else if (java.sql.Timestamp.class.equals(realTargetType))
            return new java.sql.Timestamp(value.getTime());
        else if (java.sql.Time.class.equals(realTargetType))
            return new java.sql.Time(value.getTime());

        throw new TypeConverterException(
                String.format("Unknown date (%s) type", realTargetType.getName()));
    }

    public Class getSourceType()
    {
        return Date.class;
    }

    public Class getTargetType()
    {
        return Date.class;
    }
}

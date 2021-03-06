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

import org.weda.converter.impl.AbstractConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NumberToNumberConverter extends AbstractConverter<Number, Number>
{
    public NumberToNumberConverter()
    {
        super(true);
    }

    public Number convert(Number value, Class realTargetType, String format)
    {
        if (Integer.class.equals(realTargetType) || Integer.TYPE.equals(realTargetType))
            return value.intValue();
        else if (Short.class.equals(realTargetType) || Short.TYPE.equals(realTargetType))
            return value.shortValue();
        else if (Long.class.equals(realTargetType) || Long.TYPE.equals(realTargetType))
            return value.longValue();
        else if (Double.class.equals(realTargetType) || Double.TYPE.equals(realTargetType))
            return value.doubleValue();
        else if (Float.class.equals(realTargetType) || Float.TYPE.equals(realTargetType))
            return value.floatValue();
        else if (Byte.class.equals(realTargetType) || Byte.TYPE.equals(realTargetType))
            return value.byteValue();
        else
            return value;
    }

    public Class getSourceType()
    {
        return Number.class;
    }

    public Class getTargetType()
    {
        return Number.class;
    }
}

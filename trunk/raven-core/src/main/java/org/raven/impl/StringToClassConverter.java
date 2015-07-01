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

import org.weda.converter.TypeConverterException;
import org.weda.converter.impl.AbstractConverter;

/**
 *
 * @author Mikhail Titov
 */
public class StringToClassConverter extends AbstractConverter<String, Class>
{
    public Class convert(String value, Class realTargetType, String format) 
    {
        try 
        {
            return Class.forName(value);
        } 
        catch (ClassNotFoundException ex) 
        {
            throw new TypeConverterException(String.format(
                    "Error converting (%s) to class", value), ex);
        }
    }

    public Class getSourceType() 
    {
        return String.class;
    }

    public Class getTargetType() 
    {
        return Class.class;
    }

}

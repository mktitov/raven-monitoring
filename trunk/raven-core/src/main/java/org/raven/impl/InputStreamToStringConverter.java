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

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.weda.converter.TypeConverterException;
import org.weda.converter.impl.AbstractConverter;

/**
 *
 * @author Mikhail Titov
 */
public class InputStreamToStringConverter extends AbstractConverter<InputStream, String>
{
    public String convert(InputStream value, Class realTargetType, String encoding)
    {
        try
        {
            return IOUtils.toString(value, encoding);
        }
        catch (IOException ex)
        {
            throw new TypeConverterException(ex);
        }
    }

    public Class getSourceType()
    {
        return InputStream.class;
    }

    public Class getTargetType()
    {
        return String.class;
    }
}

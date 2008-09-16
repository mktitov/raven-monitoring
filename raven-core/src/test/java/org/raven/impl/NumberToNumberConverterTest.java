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

import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NumberToNumberConverterTest extends RavenCoreTestCase
{
    @Test
    public void converter_test() throws Exception
    {
        NumberToNumberConverter converter = new NumberToNumberConverter();

        Class[] numberTypes = 
            {
                Short.class, Short.TYPE, Integer.class, Integer.TYPE, Long.class, Long.TYPE,
                Double.class, Double.TYPE, Float.class, Float.TYPE
            };
        Object[] numbers = {(short)5, (short)5, 5, 5, 5l, 5l, 5., 5., (float)5., (float)5.};
        for (int i=0; i<numberTypes.length; ++i)
        {
            check(converter, numberTypes[i], numbers[i]);
        }
    }

    public void service_test() throws Exception
    {
        TypeConverter converter = registry.getService(TypeConverter.class);
        assertNotNull(converter);

        assertEquals(new Integer(5), converter.convert(Integer.class, 5.0, null));
    }

    private void check(NumberToNumberConverter converter, Class numberClass, Object number)
    {
        assertEquals(number, converter.convert((short)5, numberClass, null));
        assertEquals(number, converter.convert(5, numberClass, null));
        assertEquals(number, converter.convert(5l, numberClass, null));
        assertEquals(number, converter.convert(5., numberClass, null));
        assertEquals(number, converter.convert(new Float(5.), numberClass, null));
    }
}

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

import java.nio.charset.Charset;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class StringToCharsetConverterTest extends RavenCoreTestCase
{
    @Test
    public void converterTest() throws Exception
    {
        StringToCharsetConverter converter = new StringToCharsetConverter();
        Charset charset = converter.convert("UTF-8", null, null);
        assertNotNull(charset);
        assertEquals("UTF-8", charset.name());
    }

    @Test
    public void serviceTest()
    {
        TypeConverter converter = registry.getService(TypeConverter.class);
        assertNotNull(converter);
        Charset charset = converter.convert(Charset.class, "UTF-8", null);
        assertNotNull(charset);
        assertEquals("UTF-8", charset.name());
    }
}
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class InputStreamToStringConverterTest extends RavenCoreTestCase
{
    @Test
    public void defaultEncodingTest() throws Exception
    {
        InputStreamToStringConverter converter = new InputStreamToStringConverter();
        ByteArrayInputStream bs = new ByteArrayInputStream("test".getBytes());
        String val = converter.convert(bs, InputStream.class, null);

        assertEquals("test", val);
    }

    @Test
    public void encodingTest() throws Exception
    {
        InputStreamToStringConverter converter = new InputStreamToStringConverter();
        ByteArrayInputStream bs = new ByteArrayInputStream("test".getBytes("UTF-8"));
        String val = converter.convert(bs, InputStream.class, "UTF-8");

        assertEquals("test", val);
    }

    @Test
    public void serviceTest() throws Exception
    {
        TypeConverter converter = registry.getService(TypeConverter.class);
        ByteArrayInputStream bs = new ByteArrayInputStream("test".getBytes());
        String val = converter.convert(String.class, bs, null);
        
        assertEquals("test", val);
    }
    
}
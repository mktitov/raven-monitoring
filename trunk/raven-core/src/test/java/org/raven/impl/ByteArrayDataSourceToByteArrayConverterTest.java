/*
 *  Copyright 2010 Mikhail Titov.
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
import org.raven.net.impl.ByteArrayDataSource;
import org.raven.test.RavenCoreTestCase;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class ByteArrayDataSourceToByteArrayConverterTest extends RavenCoreTestCase
{
    @Test
    public void converterTest()
    {
        byte[] data = new byte[]{1,2,3};
        ByteArrayDataSource ds = new ByteArrayDataSource(data, null, null);
        ByteArrayDataSourceToByteArrayConverter converter = new ByteArrayDataSourceToByteArrayConverter();
        assertArrayEquals(data, converter.convert(ds, null, null));
    }

    @Test
    public void serviceTest()
    {
        byte[] data = new byte[]{1,2,3};
        ByteArrayDataSource ds = new ByteArrayDataSource(data, null, null);
        TypeConverter converter = registry.getService(TypeConverter.class);
        assertArrayEquals(data, converter.convert(byte[].class, ds, null));
    }
}
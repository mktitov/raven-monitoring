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

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.raven.net.impl.ByteArrayDataSource;
import org.raven.test.RavenCoreTestCase;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class DataSourceToInputStreamConverterTest extends RavenCoreTestCase
{
    @Test
    public void converterTest() throws IOException
    {
        DataSourceToInputStreamConverter converter = new DataSourceToInputStreamConverter();
        byte[] data = {1,2,3};
        ByteArrayDataSource ds = new ByteArrayDataSource(data, null, null);
        InputStream res = converter.convert(ds, null, null);
        assertArrayEquals(data, IOUtils.toByteArray(res));
    }

    @Test
    public void serviceTest()
    {
        TypeConverter converter = registry.getService(TypeConverter.class);
        byte[] data = {1,2,3};
        ByteArrayDataSource ds = new ByteArrayDataSource(data, null, null);
        InputStream res = converter.convert(InputStream.class, ds, null);
    }
}
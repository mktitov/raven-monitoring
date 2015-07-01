/*
 * Copyright 2014 Mikhail Titov.
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

import org.apache.poi.util.IOUtils;
import org.junit.Test;
import org.raven.ds.BinaryFieldType;
import org.raven.test.RavenCoreTestCase;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class ByteArrayToBinaryFieldConverterTest extends RavenCoreTestCase {
    
    @Test
    public void instanceTest() throws Exception {
        ByteArrayToBinaryFieldConverter converter = new ByteArrayToBinaryFieldConverter();
        BinaryFieldType res = converter.convert(new byte[]{1,2,3}, BinaryFieldType.class, null);
        assertNotNull(res);
        byte[] bytes = IOUtils.toByteArray(res.getData());
        assertArrayEquals(new byte[]{1,2,3}, bytes);
    }
    
    @Test
    public void serviceTest() throws Exception {
        TypeConverter converter = registry.getService(TypeConverter.class);
        BinaryFieldType res = converter.convert(BinaryFieldType.class, new byte[]{1,2,3}, null);
        assertNotNull(res);
        byte[] bytes = IOUtils.toByteArray(res.getData());
        assertArrayEquals(new byte[]{1,2,3}, bytes);
        
    }
    
}

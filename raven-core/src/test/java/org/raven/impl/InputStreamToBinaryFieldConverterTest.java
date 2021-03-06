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

import java.io.ByteArrayInputStream;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.ds.BinaryFieldType;
import org.raven.ds.BinaryFieldTypeException;
import org.raven.test.RavenCoreTestCase;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class InputStreamToBinaryFieldConverterTest extends RavenCoreTestCase {
    
    @Test
    public void instanceTest() throws Exception {
        InputStreamToBinaryFieldConverter converter = new InputStreamToBinaryFieldConverter();
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1,2,3});
        assertSame(is, converter.convert(is, BinaryFieldType.class, null).getData());
    }
    
    @Test
    public void serviceTest() throws BinaryFieldTypeException {
        TypeConverter converter = registry.getService(TypeConverter.class);
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1,2,3});
        assertSame(is, converter.convert(BinaryFieldType.class, is, null).getData());
        
    }
}

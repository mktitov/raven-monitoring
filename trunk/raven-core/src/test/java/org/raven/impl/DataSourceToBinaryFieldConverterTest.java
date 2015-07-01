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
import java.io.InputStream;
import javax.activation.DataSource;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import static org.easymock.EasyMock.*;
import org.raven.ds.BinaryFieldType;
import org.weda.services.TypeConverter;
/**
 *
 * @author Mikhail Titov
 */
public class DataSourceToBinaryFieldConverterTest extends RavenCoreTestCase {
    
    @Test
    public void instanceTest() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[]{1,2,3});
        DataSource ds = createMock(DataSource.class);
        expect(ds.getInputStream()).andReturn(is);
        replay(ds);
        
        DataSourceToBinaryFieldConverter converter = new DataSourceToBinaryFieldConverter();
        assertSame(is, converter.convert(ds, BinaryFieldType.class, null).getData());
        
        verify(ds);
    }
    
    @Test
    public void serviceTest() throws Exception{
        TypeConverter converter = registry.getService(TypeConverter.class);
        InputStream is = new ByteArrayInputStream(new byte[]{1,2,3});
        DataSource ds = createMock(DataSource.class);
        expect(ds.getInputStream()).andReturn(is);
        replay(ds);
        
        assertSame(is, converter.convert(BinaryFieldType.class, ds, null).getData());
        verify(ds);
    }
}

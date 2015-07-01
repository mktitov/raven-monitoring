/*
 *  Copyright 2011 Mikhail Titov.
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
import java.io.InputStreamReader;
import java.sql.Clob;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.weda.services.TypeConverter;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class ClobToStringConverterTest extends RavenCoreTestCase{

    String testStr = "test тест";

    @Test
    public void instanceTest() throws Exception {
        Clob clob = createClob();
        replay(clob);
        ClobToStringConverter conv = new ClobToStringConverter();
        assertEquals(testStr, conv.convert(clob, null, null));
        verify(clob);
    }

    @Test
    public void serviceTest() throws Exception {
        Clob clob = createClob();
        replay(clob);

        TypeConverter converter = registry.getService(TypeConverter.class);
        assertEquals(testStr, converter.convert(String.class, clob, null));

        verify(clob);
    }

    private Clob createClob() throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream(testStr.getBytes());
        InputStreamReader reader = new InputStreamReader(is);
        Clob clob = createMock(Clob.class);
        expect(clob.getCharacterStream()).andReturn(reader);
        return clob;
    }
}
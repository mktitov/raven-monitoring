/*
 *  Copyright 2009 Mikhail Titov.
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

import java.sql.Timestamp;
import oracle.sql.TIMESTAMP;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class OracleTimestampToTimestampConverterTest extends RavenCoreTestCase
{
    @Test
    public void instanceTest()
    {
        OracleTimestampToTimestampConverter converter = new OracleTimestampToTimestampConverter();
        Timestamp curTime = new Timestamp(System.currentTimeMillis());
        TIMESTAMP ts = new TIMESTAMP(curTime);

        assertEquals(curTime, converter.convert(ts, null, null));
    }

    @Test
    public void serviceTest()
    {
        TypeConverter converter = registry.getService(TypeConverter.class);
        assertNotNull(converter);
        Timestamp curTime = new Timestamp(System.currentTimeMillis());
        TIMESTAMP ts = new TIMESTAMP(curTime);

        assertEquals(curTime, converter.convert(Timestamp.class, ts, null));
    }
}
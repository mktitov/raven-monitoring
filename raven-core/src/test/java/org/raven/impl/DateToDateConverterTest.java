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

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class DateToDateConverterTest extends RavenCoreTestCase
{
    @Test
    public void instanceTest()
    {
        DateToDateConverter converter = new DateToDateConverter();
        Date date = new Date();

        java.sql.Date sqlDate = new java.sql.Date(date.getTime());
        assertEquals(sqlDate, converter.convert(date, java.sql.Date.class, null));

        Time sqlTime = new Time(date.getTime());
        assertEquals(sqlTime, converter.convert(date, Time.class, null));

        Timestamp sqlTimestamp = new Timestamp(date.getTime());
        assertEquals(sqlTimestamp, converter.convert(date, Timestamp.class, null));
    }

    @Test
    public void serviceTest()
    {
        TypeConverter converter = registry.getService(TypeConverter.class);
        Date date = new Date();
        java.sql.Date sqlDate = new java.sql.Date(date.getTime());
        assertEquals(sqlDate, converter.convert(java.sql.Date.class, date, null));
    }
}
/*
 * Copyright 2012 Mikhail Titov.
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

import java.util.TimeZone;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.weda.services.TypeConverter;
import static org.junit.Assert.*;

/**
 *
 * @author Mikhail Titov
 */
public class StringToTimeZoneConverterTest extends RavenCoreTestCase {
    
    @Test
    public void converterTest() {
        StringToTimeZoneConverter conv = new StringToTimeZoneConverter();
        TimeZone def = TimeZone.getTimeZone("Europe/Moscow");
        assertNotNull(def);
        assertEquals(def, conv.convert(def.getID(), null, null));
        def = TimeZone.getTimeZone("GMT+04:00");
        assertNotNull(def);
        assertEquals(def, conv.convert("GMT+04:00", null, null));
    }    
    
    @Test
    public void serviceTest() {
        TypeConverter conv = registry.getService(TypeConverter.class);
        assertNotNull(conv);
        TimeZone def = TimeZone.getTimeZone("Europe/Moscow");
        assertNotNull(def);
        assertEquals(def, conv.convert(TimeZone.class, "Europe/Moscow", null));
        def = TimeZone.getTimeZone("GMT+04:00");
        assertNotNull(def);
        assertEquals(def, conv.convert(TimeZone.class, "GMT+04:00", null));
    }
}

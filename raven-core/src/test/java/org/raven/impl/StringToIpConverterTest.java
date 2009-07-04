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

import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.net.InvalidIpException;
import org.raven.net.impl.Ip;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class StringToIpConverterTest extends RavenCoreTestCase
{
    @Test
    public void instanceTest() throws InvalidIpException
    {
        StringToIpConverter converter = new StringToIpConverter();
        assertEquals(Ip.parse("10.50.1.1"), converter.convert("10.50.1.1", null, null));
    }

    @Test
    public void serviceTest() throws InvalidIpException
    {
        TypeConverter converter = registry.getService(TypeConverter.class);
        assertEquals(Ip.parse("10.50.1.1"), converter.convert(Ip.class, "10.50.1.1", null));
    }
}
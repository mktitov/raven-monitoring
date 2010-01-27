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

package org.raven.sched.impl;

import org.junit.Test;
import org.raven.sched.ValueParserException;
import org.raven.test.ServiceTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class DayOfWeekValueParserTest extends ServiceTestCase
{
    @Test
    public void parseTest() throws ValueParserException
    {
        assertEquals(new Integer(1), new DayOfWeekValueParser().parseValue("1").getFirstValue());
        assertEquals(new Integer(2), new DayOfWeekValueParser().parseValue("2").getFirstValue());
        assertEquals(new Integer(7), new DayOfWeekValueParser().parseValue("7").getFirstValue());
    }

    @Test(expected=ValueParserException.class)
    public void parseErrorTest() throws ValueParserException
    {
        assertEquals(1, new DayOfWeekValueParser().parseValue("0"));
    }

    @Test(expected=ValueParserException.class)
    public void parseErrorTest2() throws ValueParserException
    {
        assertEquals(1, new DayOfWeekValueParser().parseValue("8"));
    }

    @Test
    public void toStringTest()
    {
        DayOfWeekValueParser parser = new DayOfWeekValueParser();
        assertEquals("понедельник", parser.toString(1));
        assertEquals("вторник", parser.toString(2));
        assertEquals("среда", parser.toString(3));
        assertEquals("четверг", parser.toString(4));
        assertEquals("пятница", parser.toString(5));
        assertEquals("суббота", parser.toString(6));
        assertEquals("воскресенье", parser.toString(7));
    }
}
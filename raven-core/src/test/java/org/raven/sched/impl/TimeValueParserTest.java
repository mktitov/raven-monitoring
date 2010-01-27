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
import org.raven.sched.ValuePair;
import org.raven.sched.ValueParserException;
import org.raven.test.ServiceTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class TimeValueParserTest extends ServiceTestCase
{
    @Test(expected=ValueParserException.class)
    public void invalidTimeFormatTest() throws ValueParserException
    {
        new TimeValueParser().parseValue("1:2:3");
    }

    @Test(expected=ValueParserException.class)
    public void invalidHourTest() throws ValueParserException
    {
        new TimeValueParser().parseValue("a");
    }

    @Test(expected=ValueParserException.class)
    public void invalidHourTest2() throws ValueParserException
    {
        new TimeValueParser().parseValue("a:1");
    }

    @Test(expected=ValueParserException.class)
    public void invalidMinutesTest() throws ValueParserException
    {
        new TimeValueParser().parseValue("1:a");
    }

    @Test
    public void parserHoursTest() throws ValueParserException
    {
        TimeValueParser parser = new TimeValueParser();

        ValuePair values = new TimeValueParser().parseValue("1");
        assertEquals(new Integer(60), values.getFirstValue());
        assertEquals(new Integer(119), values.getLastValue());

        values = new TimeValueParser().parseValue("0");
        assertEquals(new Integer(0), values.getFirstValue());
        assertEquals(new Integer(59), values.getLastValue());
    }

    @Test
    public void parserHoursWithMinutesTest() throws ValueParserException
    {
        TimeValueParser parser = new TimeValueParser();

        ValuePair values = new TimeValueParser().parseValue("1:40");
        assertEquals(new Integer(100), values.getFirstValue());
        assertNull(values.getLastValue());
    }

    @Test
    public void toStringTest()
    {
        TimeValueParser parser = new TimeValueParser();
        assertEquals("00:02", parser.toString(2));
        assertEquals("10:11", parser.toString(611));
    }
}
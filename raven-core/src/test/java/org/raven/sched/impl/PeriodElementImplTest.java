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

import org.junit.Before;
import org.junit.Test;
import org.raven.sched.PeriodElement;
import org.raven.sched.ValueParser;
import org.raven.sched.ValueParserException;
import org.raven.test.ServiceTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class PeriodElementImplTest extends ServiceTestCase
{
    private ValueParserImpl parser;

    @Before
    public void prepare()
    {
        parser = new ValueParserImpl(1, 10);
    }

    @Test(expected=ValueParserException.class)
    public void invalidFormatTest() throws ValueParserException
    {
        new PeriodElementImpl("", parser);
    }

    @Test(expected=ValueParserException.class)
    public void invalidFormatTest2() throws ValueParserException
    {
        new PeriodElementImpl("1-1-2", parser);
    }

    @Test(expected=ValueParserException.class)
    public void invalidFormatTest3() throws ValueParserException
    {
        new PeriodElementImpl("a-1", parser);
    }

    @Test
    public void isInPeriodTest1() throws ValueParserException
    {
        PeriodElement elem = new PeriodElementImpl("1", parser);
        assertTrue(elem.isInPeriod(1));
        assertFalse(elem.isInPeriod(2));
        assertFalse(elem.isInPeriod(0));
    }
    
    @Test
    public void isInPeriodTest2() throws ValueParserException
    {
        PeriodElement elem = new PeriodElementImpl("1-2", parser);
        assertTrue(elem.isInPeriod(1));
        assertTrue(elem.isInPeriod(2));
        assertFalse(elem.isInPeriod(0));
        assertFalse(elem.isInPeriod(3));
    }

    @Test
    public void toStringTest() throws Exception
    {
        assertEquals("1-2", new PeriodElementImpl("1-2", parser).toString());
        assertEquals("1", new PeriodElementImpl("1", parser).toString());
    }

    @Test
    public void rangeValues() throws Exception
    {
        ValueParser parser2 = new TestValueParser();
        PeriodElement elem = new PeriodElementImpl("1-4", parser2);
        assertTrue(elem.isInPeriod(1));
        assertTrue(elem.isInPeriod(2));
        assertTrue(elem.isInPeriod(3));
        assertTrue(elem.isInPeriod(4));
        assertTrue(elem.isInPeriod(5));
        assertFalse(elem.isInPeriod(0));
        assertFalse(elem.isInPeriod(6));

        assertEquals("1-5", elem.toString());
    }
}
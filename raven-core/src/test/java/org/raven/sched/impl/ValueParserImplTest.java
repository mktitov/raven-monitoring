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
public class ValueParserImplTest extends ServiceTestCase
{
    @Test(expected=ValueParserException.class)
    public void invalidNumberTest() throws ValueParserException
    {
        new ValueParserImpl(0, 1).parseValue("a");
    }

    @Test(expected=ValueParserException.class)
    public void minValueTest() throws ValueParserException
    {
        new ValueParserImpl(1, 10).parseValue("0");
    }

    @Test(expected=ValueParserException.class)
    public void maxValueTest() throws ValueParserException
    {
        new ValueParserImpl(1, 10).parseValue("11");
    }

    @Test
    public void parseTest() throws ValueParserException
    {
        new ValueParserImpl(1, 10).parseValue("1");
        new ValueParserImpl(1, 10).parseValue("10");
        new ValueParserImpl(1, 10).parseValue("2");
    }

    @Test
    public void toStringTest()
    {
        assertEquals("1", new ValueParserImpl(0, 1).toString(1));
    }
}
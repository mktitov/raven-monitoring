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
public class PeriodImplTest extends ServiceTestCase
{
    @Test
    public void test() throws ValueParserException
    {
        PeriodImpl period = new PeriodImpl("1,2,4-6", new ValueParserImpl(1, 10));
        assertTrue(period.isInPeriod(1));
        assertTrue(period.isInPeriod(2));
        assertFalse(period.isInPeriod(3));
        assertTrue(period.isInPeriod(4));
        assertTrue(period.isInPeriod(5));
        assertTrue(period.isInPeriod(6));
        assertFalse(period.isInPeriod(7));
    }

    @Test
    public void toStringTest() throws ValueParserException
    {
        PeriodImpl period = new PeriodImpl("1,2,4-6", new ValueParserImpl(1, 10));
        assertEquals("1, 2, 4-6", period.toString());
    }
}
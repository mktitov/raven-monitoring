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

import org.raven.sched.PeriodElement;
import org.raven.sched.ValuePair;
import org.raven.sched.ValueParser;
import org.raven.sched.ValueParserException;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
public class PeriodElementImpl implements PeriodElement
{
    @Message
    private static String invalidPeriodElementFormat;
    private final ValuePair[] pairs;
    private final ValueParser parser;

    public PeriodElementImpl(String element, ValueParser parser) throws ValueParserException
    {
        String[] elems = element.split("\\s*-\\s*");
        if (elems.length>2 || elems.length<1)
            throw new ValueParserException(String.format(invalidPeriodElementFormat, element));
        this.parser = parser;
        pairs = new ValuePair[elems.length];
        for (int i=0; i<elems.length; ++i)
            pairs[i] = parser.parseValue(elems[i]);
    }

    public boolean isInPeriod(int value)
    {
        int v1 = pairs[0].getFirstValue();
        int v2 = getLastValue();

        return value>=v1 && value<=v2;
    }

    @Override
    public String toString()
    {
        int v1 = pairs[0].getFirstValue();
        int v2 = getLastValue();
        return parser.toString(v1)+(v2>v1? "-"+parser.toString(v2) : "");
    }

    private int getLastValue()
    {
        ValuePair lastPair = pairs[pairs.length-1];
        return lastPair.getLastValue()==null? lastPair.getFirstValue() : lastPair.getLastValue();
    }
}

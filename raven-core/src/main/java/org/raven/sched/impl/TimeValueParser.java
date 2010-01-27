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

import org.raven.sched.ValuePair;
import org.raven.sched.ValueParser;
import org.raven.sched.ValueParserException;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
public class TimeValueParser implements ValueParser
{
    @Message
    private static String invalidTimeFormatMessage;
    @Message
    private static String invalidHourMessage;
    @Message
    private static String invalidMinuteMessage;

    public ValuePair parseValue(String value) throws ValueParserException
    {
        String[] elems = value.split(":");
        if (elems.length<1 || elems.length>2)
            throw new ValueParserException(String.format(invalidTimeFormatMessage, value));
        int hour = parseValue(elems[0], 0, 23, invalidHourMessage);
        Integer minutes = elems.length==1? null : parseValue(elems[1], 0, 59, invalidMinuteMessage);

        int firstValue = hour*60 + (minutes==null? 0 : minutes);
        Integer lastValue = minutes!=null? null : firstValue + 59;

        return new ValuePairImpl(firstValue, lastValue);
    }

    public String toString(int value)
    {
        int hours = value/60;
        int minutes = value%60;
        return ""+(hours<10? "0" : "")+hours+":"+(minutes<10? "0" : "")+minutes;
    }

    private int parseValue(String val, int minValue, int maxValue, String errorMessage) throws ValueParserException
    {
        try
        {
            int v = Integer.parseInt(val);
            if (v<minValue || v>maxValue)
                throw new NumberFormatException();
            return v;
        }
        catch (NumberFormatException numberFormatException)
        {
            throw new ValueParserException(String.format(errorMessage, val, minValue, maxValue));
        }
    }
}

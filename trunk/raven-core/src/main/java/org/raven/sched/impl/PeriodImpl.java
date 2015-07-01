package org.raven.sched.impl;

import org.raven.sched.PeriodElement;
import org.raven.sched.ValueParser;
import org.raven.sched.ValueParserException;

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

/**
 *
 * @author Mikhail Titov
 */
public class PeriodImpl implements PeriodElement
{
    private final PeriodElement[] periodElements;

    public PeriodImpl(String period, ValueParser parser) throws ValueParserException
    {
        String[] elems  = period.split("\\s*,\\s*");
        periodElements = new PeriodElement[elems.length];
        for (int i=0; i<elems.length; ++i)
            periodElements[i] = new PeriodElementImpl(elems[i], parser);
    }

    public boolean isInPeriod(int value) 
    {
        for (PeriodElement element: periodElements)
            if (element.isInPeriod(value))
                return true;
        return false;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<periodElements.length; ++i)
        {
            if (i>0) builder.append(", ");
            builder.append(periodElements[i].toString());
        }
        return builder.toString();
    }
}

/*
 *  Copyright 2008 Mikhail Titov.
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

import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.weda.converter.impl.AbstractConverter;

/**
 *
 * @author Mikhail Titov
 */
public class SnmpVariableToNumberConverter extends AbstractConverter<Variable, Number>
{
    public SnmpVariableToNumberConverter()
    {
        super(true);
    }

    public Number convert(Variable var, Class realTargetType, String format)
    {
        Number val = var instanceof OctetString? new Double(var.toString()) : var.toLong();
        if (realTargetType==Long.class)
            return val.longValue();
        else if (realTargetType==Integer.class)
            return val.intValue();
        else if (realTargetType==Short.class)
            return val.shortValue();
        else if (realTargetType==Double.class)
            return val.doubleValue();
        else if (realTargetType==Float.class)
            return val.floatValue();
        else
            return val.byteValue();
    }

    public Class getSourceType()
    {
        return Variable.class;
    }

    public Class getTargetType()
    {
        return Number.class;
    }

}

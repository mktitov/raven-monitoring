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

import java.util.Locale;
import org.weda.converter.TypeConverterException;
import org.weda.converter.impl.AbstractConverter;

/**
 *
 * @author Mikhail Titov
 */
public class StringToLocaleConverter extends AbstractConverter<String, Locale> {

    public Locale convert(String value, Class realTargetType, String format) {
        String[] keys = value.split("_");
        switch (keys.length) {
            case 1: return new Locale(keys[0]);
            case 2: return new Locale(keys[0], keys[1]);
            case 3: return new Locale(keys[0], keys[1], keys[2]);
        }
        throw new TypeConverterException(String.format("Invalid locale (%s)", value));
    }

    public Class getSourceType() {
        return String.class;
    }

    public Class getTargetType() {
        return Locale.class;
    }
}

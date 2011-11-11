/*
 *  Copyright 2011 Mikhail Titov.
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

import java.sql.Clob;
import org.apache.commons.io.IOUtils;
import org.weda.converter.TypeConverterException;
import org.weda.converter.impl.AbstractConverter;

/**
 *
 * @author Mikhail Titov
 */
public class ClobToStringConverter extends AbstractConverter<Clob, String> {

    public String convert(Clob value, Class realTargetType, String format) {
        try {
            return IOUtils.toString(value.getCharacterStream());
        } catch (Exception ex) {
            throw new TypeConverterException("Error converting CLOB to string", ex);
        }
    }

    public Class getSourceType() {
        return Clob.class;
    }

    public Class getTargetType() {
        return String.class;
    }
}

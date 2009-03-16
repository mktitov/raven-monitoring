/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.ds;

import java.io.InputStream;
import java.sql.Timestamp;
import org.raven.net.Ip;

/**
 *
 * @author Mikhail Titov
 */
public enum RecordSchemaFieldType
{
    LONG(Long.class), INTEGER(Integer.class), SHORT(Short.class), BYTE(Byte.class),
    DOUBLE(Double.class), FLOAT(Float.class),
    STRING(String.class),
    TIMESTAMP(Timestamp.class),
    IP(Ip.class),
    BINARY(BinaryFieldType.class);

    private final Class type;

    private RecordSchemaFieldType(Class type)
    {
        this.type = type;
    }

    public Class getType()
    {
        return type;
    }

    public static Object getSqlObject(RecordSchemaFieldType type, Object value)
            throws RecordSchemaFieldTypeException
    {
        try
        {
            switch(type)
            {
                case IP: return value.toString();
                case BINARY: return ((BinaryFieldType)value).getData();
                default: return value;
            }
        }
        catch(BinaryFieldTypeException e)
        {
            throw new RecordSchemaFieldTypeException(e);
        }
    }

    public static Class getSqlType(RecordSchemaFieldType type)
    {
        switch(type)
        {
            case IP: return String.class;
            case BINARY: return InputStream.class;
            default: return type.getType();
        }
    }
}

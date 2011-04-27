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

package org.raven.ds;

import java.sql.Types;
import org.junit.Assert;
import org.junit.Test;
import static org.raven.ds.RecordSchemaFieldType.*;
/**
 *
 * @author Mikhail Titov
 */
public class RecordSchemaFieldTypeTest extends Assert
{
    @Test
    public void getTypeBySqlTypeTest() throws Exception
    {
        assertEquals(LONG, getTypeBySqlType(Types.BIGINT));
        assertEquals(INTEGER, getTypeBySqlType(Types.INTEGER));
        assertEquals(SHORT, getTypeBySqlType(Types.SMALLINT));
        assertEquals(BYTE, getTypeBySqlType(Types.TINYINT));
        assertEquals(DOUBLE, getTypeBySqlType(Types.DOUBLE));
        assertEquals(FLOAT, getTypeBySqlType(Types.FLOAT));
        assertEquals(DATE, getTypeBySqlType(Types.DATE));
        assertEquals(TIMESTAMP, getTypeBySqlType(Types.TIMESTAMP));
        assertEquals(BOOLEAN, getTypeBySqlType(Types.BOOLEAN));
        assertEquals(STRING, getTypeBySqlType(Types.VARCHAR));
        assertEquals(BINARY, getTypeBySqlType(Types.BLOB));
        assertEquals(BINARY, getTypeBySqlType(Types.BINARY));
        assertNull(getTypeBySqlType(-1));
    }
}
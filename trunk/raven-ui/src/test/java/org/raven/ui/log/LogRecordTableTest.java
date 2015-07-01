/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.ui.log;

import org.junit.*;
import static org.junit.Assert.*;
import org.raven.log.LogLevel;
import org.raven.log.NodeLogRecord;

/**
 *
 * @author Mikhail Titov
 */
public class LogRecordTableTest {
    
    @Ignore @Test()
    public void test() {
        NodeLogRecord rec = new NodeLogRecord(1, "path", LogLevel.TRACE, "line1\nCaused by line2\nline3\nline4.EXPR.");
        LogRecordTable table = new LogRecordTable();
        Object[] vals = table.getObjects(rec);
        assertNotNull(vals);
    }
    
}

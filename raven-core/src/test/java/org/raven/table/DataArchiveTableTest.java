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

package org.raven.table;

import java.util.Date;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class DataArchiveTableTest extends RavenCoreTestCase
{
    @Test
    public void sumTest() throws Exception
    {
        DataArchiveTable table = new DataArchiveTable();
        table.addData(new Date(), Double.NaN);
        table.addData(new Date(), null);
        table.addData(new Date(), 1);
        table.addData(new Date(), 2);
        Object sum = table.sum();
        assertNotNull(sum);
        assertTrue(sum instanceof Number);
        assertEquals(3, ((Number)sum).intValue());
    }
}
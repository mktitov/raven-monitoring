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

package org.raven.table;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractTableTest extends Assert 
{
    @Test
    public void columnTagsTest()
    {
        TestTable table = new TestTable();
        assertFalse(table.containsColumnTag(0, "tag1"));
        assertNull(table.getColumnTags(0));

        TestTableTag tag = new TestTableTag("tag1");
        TestTableTag tag2 = new TestTableTag("tag2");
        table.addColumnTag(0, tag);
        assertTrue(table.containsColumnTag(0, "tag1"));
        assertFalse(table.containsColumnTag(1, "tag1"));
        assertNotNull(table.getColumnTags(0));
        assertEquals(1, table.getColumnTags(0).size());
        assertSame(tag, table.getColumnTags(0).get("tag1"));
        table.addColumnTag(0, tag2);
        assertEquals(2, table.getColumnTags(0).size());
        assertSame(tag, table.getColumnTags(0).get("tag1"));
        assertSame(tag2, table.getColumnTags(0).get("tag2"));
    }

    @Test
    public void rowTagsTest()
    {
        TestTable table = new TestTable();
        assertFalse(table.containsRowTag(0, "tag1"));
        assertNull(table.getRowTags(0));

        TestTableTag tag = new TestTableTag("tag1");
        TestTableTag tag2 = new TestTableTag("tag2");
        table.addRowTag(0, tag);
        assertTrue(table.containsRowTag(0, "tag1"));
        assertFalse(table.containsRowTag(1, "tag1"));
        assertNotNull(table.getRowTags(0));
        assertEquals(1, table.getRowTags(0).size());
        assertSame(tag, table.getRowTags(0).get("tag1"));
        table.addRowTag(0, tag2);
        assertEquals(2, table.getRowTags(0).size());
        assertSame(tag, table.getRowTags(0).get("tag1"));
        assertSame(tag2, table.getRowTags(0).get("tag2"));
    }
}
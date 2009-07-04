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

package org.raven.impl;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.RavenUtils;
import org.raven.table.Table;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class CollectionToTableConverterTest extends RavenCoreTestCase
{
    @Test
    public void simpleValueToTableTest()
    {
        CollectionToTableConverter converter = new CollectionToTableConverter();
        List list = Arrays.asList("colname", "colvalue");

        checkConversion(converter, list);
    }

    @Test
    public void arrayToTableTest()
    {
        CollectionToTableConverter converter = new CollectionToTableConverter();
        List list = Arrays.asList(new String[]{"colname"}, new String[]{"colvalue"});
        checkConversion(converter, list);
    }

    @Test
    public void collectionToTableTest()
    {
        CollectionToTableConverter converter = new CollectionToTableConverter();
        List list = Arrays.asList(Arrays.asList("colname"), Arrays.asList("colvalue"));
        checkConversion(converter, list);
    }

    @Test
    public void serviceTest()
    {
        TypeConverter converter = registry.getService(TypeConverter.class);
        assertNotNull(converter);
        List list = Arrays.asList("colname", "colvalue");
        Table table = converter.convert(Table.class, list, null);
        assertNotNull(table);
    }

    private void checkConversion(CollectionToTableConverter converter, List list)
    {
        Table table = converter.convert(list, null, null);
        assertNotNull(table);
        assertArrayEquals(new String[]{"colname"}, table.getColumnNames());
        List<Object[]> res = RavenUtils.tableAsList(table);
        assertEquals(1, res.size());
        assertEquals("colvalue", res.get(0)[0]);
    }
}
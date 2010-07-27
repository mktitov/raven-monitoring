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

package org.raven;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Test;
import org.raven.table.TableImpl;
import org.raven.test.ServiceTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class RavenUtilsTest extends ServiceTestCase
{
    @Test
    public void tableToHtml_nbspTest() throws Exception
    {
        TableImpl table = new TableImpl(new String[]{"nbsp1", "nbsp2"});
        table.addRow(new Object[]{null, ""});

        String html = RavenUtils.tableToHtml(table);
        assertNotNull(html);
        assertEquals("<table><tr><th>nbsp1</th><th>nbsp2</th></tr><tr><td>&nbsp;</td><td>&nbsp;</td></tr></table>", html);
    }
    
    @Test
    public void tableToHtml_numberTest() throws Exception
    {
        TableImpl table = new TableImpl(new String[]{"float", "double", "integer"});
        table.addRow(new Object[]{new Float(12.345), new Double(12.343), new Integer(123)});

        String html = RavenUtils.tableToHtml(table);
        assertNotNull(html);
        assertEquals("<table><tr><th>float</th><th>double</th><th>integer</th></tr><tr><td>12.35</td><td>12.34</td><td>123</td></tr></table>", html);
    }

    @Test
    public void tableToHtml_datesTest() throws Exception
    {
        TableImpl table = new TableImpl(new String[]{"java.sql.Date", "java.sql.Time", "java.util.Date"});
        Date date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").parse("01.01.2010 01:02:03");
        table.addRow(new Object[]{new java.sql.Date(date.getTime()), new java.sql.Time(date.getTime()), date});

        String html = RavenUtils.tableToHtml(table);
        assertNotNull(html);
        assertEquals("<table><tr><th>java.sql.Date</th><th>java.sql.Time</th><"
                + "th>java.util.Date</th></tr><tr><td>01.01.2010</td>"
                + "<td>01:02:03</td><td>01.01.2010 01:02:03</td></tr></table>", html);
    }
}
/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.ds.impl;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class TextToLinesNodeTest extends RavenCoreTestCase
{
    private PushDataSource ds;
    private TextToLinesNode splitter;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        splitter = new TextToLinesNode();
        splitter.setName("splitter");
        tree.getRootNode().addAndSaveChildren(splitter);
        splitter.setDataSource(ds);
        splitter.setEncoding(Charset.forName("utf-8"));
        assertTrue(splitter.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(splitter);
        assertTrue(collector.start());
    }

    @Test
    public void stringSourceTest()
    {
        ds.pushData("Строка1\nline2");

        assertEquals(2, collector.getDataListSize());
        assertEquals("Строка1", collector.getDataList().get(0));
        assertEquals("line2", collector.getDataList().get(1));
    }

    @Test
    public void inputStreamSourceTest() throws UnsupportedEncodingException
    {
        ByteArrayInputStream is = new ByteArrayInputStream("Строка1\nline2".getBytes("utf-8"));
        ds.pushData(is);

        assertEquals(2, collector.getDataListSize());
        assertEquals("Строка1", collector.getDataList().get(0));
        assertEquals("line2", collector.getDataList().get(1));
    }
}
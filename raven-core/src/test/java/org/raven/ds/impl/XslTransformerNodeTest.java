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

package org.raven.ds.impl;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.raven.test.PushDataSource;
import org.raven.ds.DataConsumer;
import org.raven.test.DataCollector;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class XslTransformerNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        XslTransformerNode transformer = new XslTransformerNode();
        transformer.setName("transformer");
        tree.getRootNode().addAndSaveChildren(transformer);
        transformer.setDataSource(ds);
        transformer.getStylesheet().setDataStream(
                new FileInputStream("src/test/conf/xml_style.xsl"));
        assertTrue(transformer.start());

        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(transformer);
        assertTrue(collector.start());

        ds.pushData(new FileInputStream("src/test/conf/xml_file.xml"));
        List data = collector.getDataList();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof byte[]);
        assertEquals("World", new String((byte[])data.get(0)));
    }
}
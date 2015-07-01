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
package org.raven.ds.impl;

import java.io.ByteArrayInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.DataFile;
import org.raven.tree.impl.objects.NodeWithDataFileAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class WriteToFileAttributeDataPipeTest extends RavenCoreTestCase {
    private PushDataSource ds;
    private WriteToFileAttributeDataPipe writer;
    private NodeWithDataFileAttribute node;
    private DataCollector collector;
    
    @Before
    public void prepare() {
        ds = new PushDataSource();
        ds.setName("data source");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        node = new NodeWithDataFileAttribute();
        node.setName("node with file attr");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());
        
        writer = new WriteToFileAttributeDataPipe();
        writer.setName("file attribute setter pipe");
        tree.getRootNode().addAndSaveChildren(writer);
        writer.setDataSource(ds);
        writer.setNode(node);
        writer.setFileAttribute("dataFile");
        writer.setFileName("test_file");
        writer.setMimeType("test/mime");
        assertTrue(writer.start());
        
        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(writer);
        assertTrue(collector.start());
    }
    
    @Test
    public void test() throws Exception {
        byte[] buf = new byte[]{1,2,3};
        ByteArrayInputStream stream = new ByteArrayInputStream(buf);
        ds.pushData(stream);
        ds.pushData(null);
        
        DataFile file = node.getDataFile();
        assertEquals("test_file", file.getFilename());
        assertEquals(new Long(3), file.getFileSize());
        assertEquals("test/mime", file.getMimeType());
        assertArrayEquals(buf, IOUtils.toByteArray(file.getDataStream()));
        
        assertEquals(2, collector.getDataListSize());
        assertSame(stream, collector.getDataList().get(0));
        assertNull(collector.getDataList().get(1));
    }
    
}

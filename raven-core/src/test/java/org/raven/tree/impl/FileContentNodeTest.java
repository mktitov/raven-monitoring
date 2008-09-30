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

package org.raven.tree.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.RavenUtils;
import org.raven.ds.impl.AbstractDataConsumer.ResetDataPolicy;
import org.raven.table.Table;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.objects.TestDataConsumer;

/**
 *
 * @author Mikhail Titov
 */
public class FileContentNodeTest extends RavenCoreTestCase
{
    private File filesDir, file1, file2;
    private FileContentNode fileContent;
    private TestDataConsumer consumer;
    

    @Before
    public void prepareFiles() throws Exception
    {
        filesDir = new File("target/file-content-files");
        try{
            FileUtils.deleteDirectory(filesDir);
        }catch(IOException e)
        {
        }
        FileUtils.forceMkdir(filesDir);
        file1 = new File(filesDir.getAbsolutePath()+"/file1.txt");
        FileUtils.writeStringToFile(file1, "col1-1 col1-2\ncol2-1 col2-2");
    }

    @Before
    public void prepareNodes()
    {
        fileContent = new FileContentNode();
        fileContent.setName("fileContent");
        tree.getRootNode().addChildren(fileContent);
        fileContent.save();
        fileContent.init();
        fileContent.start();

        assertEquals(Status.STARTED, fileContent.getStatus());

        consumer = new TestDataConsumer();
        consumer.setName("consumer");
        tree.getRootNode().addChildren(consumer);
        consumer.save();
        consumer.init();
        consumer.setDataSource(fileContent);
        consumer.setResetDataPolicy(ResetDataPolicy.DONT_RESET_DATA);
    }

    @Test
    public void oneFileReadTest() throws Exception
    {
        consumer.getNodeAttribute(FileContentNode.URL_ATTRIBUTE).setValue(file1.getAbsolutePath());
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        Object data = consumer.refereshData(null);
        assertNotNull(data);
        assertTrue(data instanceof Table);
        List<Object[]> rows = RavenUtils.tableAsList((Table)data);
        assertEquals(2, rows.size());
        assertEquals("col1-1", rows.get(0)[0]);
        assertEquals("col1-2", rows.get(0)[1]);
        assertEquals("col2-1", rows.get(1)[0]);
        assertEquals("col2-2", rows.get(1)[1]);

        assertTrue(file1.exists());
    }

    @Test
    public void oneFileReadAsOneRowTest() throws Exception
    {
        consumer.getNodeAttribute(FileContentNode.URL_ATTRIBUTE).setValue(file1.getAbsolutePath());
        consumer.getNodeAttribute(FileContentNode.ROWDELIMITER_ATTRIBUTE).setValue(null);
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        Object data = consumer.refereshData(null);
        assertNotNull(data);
        assertTrue(data instanceof Table);
        List<Object[]> rows = RavenUtils.tableAsList((Table)data);
        assertEquals(1, rows.size());
        assertEquals("col1-1", rows.get(0)[0]);
        assertEquals("col1-2", rows.get(0)[1]);
        assertEquals("col2-1", rows.get(0)[2]);
        assertEquals("col2-2", rows.get(0)[3]);
        
        assertTrue(file1.exists());
    }
    
    @Test
    public void addFileNameToFirstColumnTest() throws Exception
    {
        consumer.getNodeAttribute(FileContentNode.URL_ATTRIBUTE).setValue(file1.getAbsolutePath());
        consumer.getNodeAttribute(
                FileContentNode.ADDFILENAMETOFIRSTCOLUMN_ATTRIBUTE).setValue("true");
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        Object data = consumer.refereshData(null);
        assertNotNull(data);
        assertTrue(data instanceof Table);
        List<Object[]> rows = RavenUtils.tableAsList((Table)data);
        assertEquals(2, rows.size());
        assertEquals("file1.txt", rows.get(0)[0]);
        assertEquals("col1-1", rows.get(0)[1]);
        assertEquals("col1-2", rows.get(0)[2]);
        assertEquals("file1.txt", rows.get(1)[0]);
        assertEquals("col2-1", rows.get(1)[1]);
        assertEquals("col2-2", rows.get(1)[2]);

        assertTrue(file1.exists());
    }

    @Test
    public void removeFileAfterProcessingTest() throws Exception
    {
        consumer.getNodeAttribute(FileContentNode.URL_ATTRIBUTE).setValue(file1.getAbsolutePath());
        consumer.getNodeAttribute(
                FileContentNode.REMOVEFILEAFTERPROCESSING_ATTRIBUTE).setValue("true");
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        Object data = consumer.refereshData(null);
        assertNotNull(data);
        assertTrue(data instanceof Table);
        List<Object[]> rows = RavenUtils.tableAsList((Table)data);
        assertEquals(2, rows.size());
        assertEquals("col1-1", rows.get(0)[0]);
        assertEquals("col1-2", rows.get(0)[1]);
        assertEquals("col2-1", rows.get(1)[0]);
        assertEquals("col2-2", rows.get(1)[1]);

        assertFalse(file1.exists());
    }
    
}

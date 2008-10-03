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
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.auth.StaticUserAuthenticator;
import org.apache.commons.vfs.impl.DefaultFileSystemConfigBuilder;
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

        file2 = new File(filesDir.getAbsolutePath()+"/file2.txt");
        FileUtils.writeStringToFile(file2, "col1-1 col1-2");
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

//    @Test
    public void test() throws Exception
    {
        StaticUserAuthenticator auth = new StaticUserAuthenticator("PRICER3", "statreader", "oerfhm");
        FileSystemOptions opts = new FileSystemOptions();
        DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
        FileObject fo = VFS.getManager().resolveFile("smb://10.50.2.37/statBackup/test", opts);
        assertEquals(FileType.FOLDER, fo.getType());
    }

    @Test
    public void oneFileReadTest() throws Exception
    {
//        consumer.getNodeAttribute(FileContentNode.URL_ATTRIBUTE).setValue(file1.getAbsolutePath());
        consumer.getNodeAttribute(FileContentNode.URL_ATTRIBUTE).setValue(
                "smb://statreader:oerfhm@10.50.2.37/statBackup/test");
//        consumer.getNodeAttribute(FileContentNode.URL_ATTRIBUTE).setValue(
//                "smb://tim:F071f07tim@10.50.1.85/dvd");
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

//    @Test
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
    
//    @Test
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

//    @Test
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

//    @Test
    public void readManyFilesTest() throws Exception
    {
        consumer.getNodeAttribute(
                FileContentNode.URL_ATTRIBUTE).setValue(filesDir.getAbsolutePath());
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        consumer.refereshData(null);
        List dataList = consumer.getDataList();
        assertEquals(2, dataList.size());
        for (int i=0; i<dataList.size(); ++i)
        {
            Object data = dataList.get(i);
            assertNotNull(data);
            assertTrue(data instanceof Table);
            List<Object[]> rows = RavenUtils.tableAsList((Table)data);
            if (i==0)
                assertEquals(2, rows.size());
            else
                assertEquals(1, rows.size());
            assertEquals("col1-1", rows.get(0)[0]);
            assertEquals("col1-2", rows.get(0)[1]);
            if (i==0)
            {
                assertEquals("col2-1", rows.get(1)[0]);
                assertEquals("col2-2", rows.get(1)[1]);
            }
        }
        assertTrue(file1.exists());
        assertTrue(file2.exists());
    }
    
//    @Test
    public void readManyFilesWithFileMaskTest() throws Exception
    {
        consumer.getNodeAttribute(
                FileContentNode.URL_ATTRIBUTE).setValue(filesDir.getAbsolutePath());
        consumer.getNodeAttribute(FileContentNode.FILEMASK_ATTRIBUTE).setValue("^file2.*");
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        consumer.refereshData(null);
        List dataList = consumer.getDataList();
        assertEquals(1, dataList.size());
        for (int i=0; i<dataList.size(); ++i)
        {
            Object data = dataList.get(i);
            assertNotNull(data);
            assertTrue(data instanceof Table);
            List<Object[]> rows = RavenUtils.tableAsList((Table)data);
            assertEquals(1, rows.size());
            assertEquals("col1-1", rows.get(0)[0]);
            assertEquals("col1-2", rows.get(0)[1]);
        }

        assertTrue(file1.exists());
        assertTrue(file2.exists());

    }
    
//    @Test
    public void readManyFilesWithFileMask_removeAfterProcessingTest() throws Exception
    {
        consumer.getNodeAttribute(
                FileContentNode.URL_ATTRIBUTE).setValue(filesDir.getAbsolutePath());
        consumer.getNodeAttribute(FileContentNode.FILEMASK_ATTRIBUTE).setValue("^file2.*");
        consumer.getNodeAttribute(
                FileContentNode.REMOVEFILEAFTERPROCESSING_ATTRIBUTE).setValue("true");
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        consumer.refereshData(null);
        List dataList = consumer.getDataList();
        assertEquals(1, dataList.size());

        assertTrue(file1.exists());
        assertFalse(file2.exists());
    }
}

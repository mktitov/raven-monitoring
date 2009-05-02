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

package org.raven.net.impl;

import org.raven.net.impl.FileReaderNode;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.ds.impl.AbstractDataConsumer.ResetDataPolicy;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.objects.TestDataConsumer;

/**
 *
 * @author Mikhail Titov
 */
public class FileReaderNodeTest extends RavenCoreTestCase
{
    private File filesDir, file1, file2;
    private FileReaderNode fileReader;
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
        fileReader = new FileReaderNode();
        fileReader.setName("fileContent");
        tree.getRootNode().addChildren(fileReader);
        fileReader.save();
        fileReader.init();
        fileReader.start();

        assertEquals(Status.STARTED, fileReader.getStatus());

        consumer = new TestDataConsumer();
        consumer.setName("consumer");
        tree.getRootNode().addChildren(consumer);
        consumer.save();
        consumer.init();
        consumer.setDataSource(fileReader);
        consumer.setResetDataPolicy(ResetDataPolicy.DONT_RESET_DATA);
    }

    @Test
    public void oneFileReadTest() throws Exception
    {
        consumer.getNodeAttribute(FileReaderNode.URL_ATTRIBUTE).setValue(file1.getAbsolutePath());
//        consumer.getNodeAttribute(FileContentNode.URL_ATTRIBUTE).setValue(
//                "smb://timtest:Atest_12345678@10.50.1.85/test/");
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        consumer.refereshData(null);
        List dataList = consumer.getDataList();
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        Object data = dataList.get(0);

        assertEquals("col1-1 col1-2\ncol2-1 col2-2", data);

        assertTrue(file1.exists());
    }

    @Test
    public void manyFilesReadTest() throws Exception
    {
        consumer.getNodeAttribute(FileReaderNode.URL_ATTRIBUTE).setValue(
                filesDir.getAbsolutePath());
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        consumer.refereshData(null);
        List dataList = consumer.getDataList();
        assertNotNull(dataList);
        assertEquals(2, dataList.size());

        assertEquals("col1-1 col1-2\ncol2-1 col2-2", dataList.get(0));
        assertEquals("col1-1 col1-2", dataList.get(1));

        assertTrue(file1.exists());
    }

    @Test
    public void manyFilesReadWithFileMaskTest() throws Exception
    {
        consumer.getNodeAttribute(FileReaderNode.URL_ATTRIBUTE).setValue(
                filesDir.getAbsolutePath());
        consumer.getNodeAttribute(FileReaderNode.FILEMASK_ATTRIBUTE).setValue("^file1.*");
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        consumer.refereshData(null);
        List dataList = consumer.getDataList();
        assertNotNull(dataList);
        assertEquals(1, dataList.size());

        assertEquals("col1-1 col1-2\ncol2-1 col2-2", dataList.get(0));

        assertTrue(file1.exists());
    }

    @Test
    public void removeFileAfterProcessingTest() throws Exception
    {
        consumer.getNodeAttribute(FileReaderNode.URL_ATTRIBUTE).setValue(file1.getAbsolutePath());
        consumer.getNodeAttribute(
                FileReaderNode.REMOVEFILEAFTERPROCESSING_ATTRIBUTE).setValue("true");
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        consumer.refereshData(null);
        List dataList = consumer.getDataList();
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        Object data = dataList.get(0);

        assertEquals("col1-1 col1-2\ncol2-1 col2-2", data);

        assertFalse(file1.exists());
        
    }
}
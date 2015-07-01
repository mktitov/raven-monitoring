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

import org.raven.net.impl.SmbFileReader;
import java.io.File;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.impl.AbstractDataConsumer.ResetDataPolicy;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.objects.TestDataConsumer;

/**
 *
 * @author Mikhail Titov
 */
public class SmbFileReaderTest extends RavenCoreTestCase
{
    private File filesDir, file1, file2;
    private SmbFileReader fileReader;
    private TestDataConsumer consumer;

    @Before
    public void prepareFiles() throws Exception
    {
        filesDir = new File("c:/tmp/test");
        File[] files = filesDir.listFiles();
        if (files!=null)
            for (File file: files)
                file.delete();

        FileUtils.forceMkdir(filesDir);
        file1 = new File(filesDir.getAbsolutePath()+"/file1.txt");
        FileUtils.writeStringToFile(file1, "file1");

        file2 = new File(filesDir.getAbsolutePath()+"/file2.txt");
        FileUtils.writeStringToFile(file2, "file2");
    }
    
    @Before
    public void prepareNodes()
    {
        fileReader = new SmbFileReader();
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
    
    @Ignore
    @Test
    public void readOneFileTest() throws Exception
    {
        consumer.getNodeAttribute(SmbFileReader.URL_ATTRIBUTE).setValue(
                "smb://timtest:Atest_12345678@10.50.1.85/test/"+file1.getName());
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        consumer.refereshData(null);
        List dataList = consumer.getDataList();
        assertEquals(1, dataList.size());
        assertEquals("file1", dataList.get(0));

        assertTrue(file1.exists());
        assertTrue(file2.exists());
    }

    @Ignore
    @Test
    public void readAllFilesFromDir() throws Exception
    {
        consumer.getNodeAttribute(SmbFileReader.URL_ATTRIBUTE).setValue(
                "smb://timtest:Atest_12345678@10.50.1.85/test/");
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        consumer.refereshData(null);
        List dataList = consumer.getDataList();
        assertEquals(2, dataList.size());
        assertTrue(dataList.contains("file1"));
        assertTrue(dataList.contains("file2"));
        
        assertTrue(file1.exists());
        assertTrue(file2.exists());
    }

    @Ignore
    @Test
    public void smbFileMaskTest() throws Exception
    {
        consumer.getNodeAttribute(SmbFileReader.URL_ATTRIBUTE).setValue(
                "smb://timtest:Atest_12345678@10.50.1.85/test/");
        consumer.getNodeAttribute(SmbFileReader.SMBFILEMASK_ATTRIBUTE).setValue("file2*");
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        consumer.refereshData(null);
        List dataList = consumer.getDataList();
        assertEquals(1, dataList.size());
        assertEquals("file2", dataList.get(0));
        
        assertTrue(file1.exists());
        assertTrue(file2.exists());
    }

    @Ignore
    @Test
    public void regexpFileMaskTest() throws Exception
    {
        consumer.getNodeAttribute(SmbFileReader.URL_ATTRIBUTE).setValue(
                "smb://timtest:Atest_12345678@10.50.1.85/test/");
        consumer.getNodeAttribute(SmbFileReader.REGEXP_FILEMASK_ATTRIBUTE).setValue("^file1.*");
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        consumer.refereshData(null);
        List dataList = consumer.getDataList();
        assertEquals(1, dataList.size());
        assertEquals("file1", dataList.get(0));
        
        assertTrue(file1.exists());
        assertTrue(file2.exists());
    }

    @Ignore
    @Test
    public void smbFileMaskWithregexpFileMaskTest() throws Exception
    {
        File file3 = new File(filesDir.getAbsolutePath()+"/file3");
        FileUtils.writeStringToFile(file3, "file3");
        consumer.getNodeAttribute(SmbFileReader.URL_ATTRIBUTE).setValue(
                "smb://timtest:Atest_12345678@10.50.1.85/test/");
        consumer.getNodeAttribute(SmbFileReader.REGEXP_FILEMASK_ATTRIBUTE).setValue("^file[13].*");
        consumer.getNodeAttribute(SmbFileReader.SMBFILEMASK_ATTRIBUTE).setValue("*.txt");
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        consumer.refereshData(null);
        List dataList = consumer.getDataList();
        assertEquals(1, dataList.size());
        assertEquals("file1", dataList.get(0));
        
        assertTrue(file1.exists());
        assertTrue(file2.exists());
        assertTrue(file3.exists());
    }

    @Ignore
    @Test
    public void removeAfterProcessTest() throws Exception
    {
         consumer.getNodeAttribute(SmbFileReader.URL_ATTRIBUTE).setValue(
                "smb://timtest:Atest_12345678@10.50.1.85/test/");
         consumer.getNodeAttribute(
                 SmbFileReader.REMOVEFILEAFTERPROCESSING_ATTRIBUTE).setValue("true");
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        consumer.refereshData(null);
        List dataList = consumer.getDataList();
        assertEquals(2, dataList.size());
        assertTrue(dataList.contains("file1"));
        assertTrue(dataList.contains("file2"));
        
        assertFalse(file1.exists());
        assertFalse(file2.exists());
    }
}
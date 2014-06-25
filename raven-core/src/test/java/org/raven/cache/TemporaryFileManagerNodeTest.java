/*
 *  Copyright 2011 Mikhail Titov.
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

package org.raven.cache;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import javax.activation.DataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.TestScheduler;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class TemporaryFileManagerNodeTest extends RavenCoreTestCase
{
    private TestScheduler scheduler;
    private TemporaryFileManagerNode manager;

    @Before
    public void prepare()
    {
        scheduler = new TestScheduler();
        scheduler.setName("scheduler");
        tree.getRootNode().addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());

        manager = new TemporaryFileManagerNode();
        manager.setName("manager");
        tree.getRootNode().addAndSaveChildren(manager);
        manager.setDirectory("target/");
        manager.setScheduler(scheduler);
        assertTrue(manager.start());
    }

    @Test
    public void saveGetReleaseTest() throws Exception
    {
        byte[] arr = {1,2,3,4,5};
        ByteArrayInputStream in = new ByteArrayInputStream(arr);
        DataSource ds = manager.saveFile(manager, "test", in, null, true);
        checkDataSource(ds, arr);

        ds = manager.getDataSource("test");
        checkDataSource(ds, arr);

        assertNull(manager.getDataSource("test2"));

        File file = new File(ds.getName());
        assertTrue(file.exists());
        manager.releaseDataSource("test");
        assertTrue(file.exists());
        assertNull(manager.getDataSource("test"));
        
        manager.executeScheduledJob(null);
        assertFalse(file.exists());
    }

    @Test
    public void timelifeTest() throws Exception
    {
        manager.setTimelife(5);
        byte[] arr = {1,2,3,4,5};
        ByteArrayInputStream in = new ByteArrayInputStream(arr);
        DataSource ds1 = manager.saveFile(manager, "test", in, null, true);

        TimeUnit.SECONDS.sleep(3);
        in.reset();
        DataSource ds2 = manager.saveFile(manager, "test2", in, null, true);
        assertArrayEquals(arr, IOUtils.toByteArray(ds2.getInputStream()));
        InputStream is = ds2.getInputStream();

        TimeUnit.SECONDS.sleep(3);
        manager.executeScheduledJob(null);

        assertNotNull(manager.getDataSource("test2"));
        assertArrayEquals(arr, IOUtils.toByteArray(is));
        
        assertNull(manager.getDataSource("test"));
    }

    @Test
    public void stopTest() throws Exception
    {
        byte[] arr = {1,2,3,4,5};
        ByteArrayInputStream in = new ByteArrayInputStream(arr);
        DataSource ds1 = manager.saveFile(manager, "test", in, null, true);

        File file = new File(ds1.getName());
        assertTrue(file.exists());
        manager.stop();
        assertFalse(file.exists());
    }

    @Test
    public void rewriteTest() throws Exception
    {
        byte[] arr = {1,2,3,4,5};
        byte[] arr2 = {5, 4, 3, 2, 1};
        ByteArrayInputStream in = new ByteArrayInputStream(arr);
        ByteArrayInputStream in2 = new ByteArrayInputStream(arr2);

        DataSource ds1 = manager.saveFile(manager, "test", in, null, true);
        File file = new File(ds1.getName());
        assertTrue(file.exists());
        checkDataSource(manager.saveFile(manager, "test", in2, null, false), arr);
        checkDataSource(manager.saveFile(manager, "test", in2, null, true), arr2);

        manager.executeScheduledJob(null);
        assertFalse(file.exists());
    }

    @Test
    public void forceCreateDirTest() throws Exception
    {
        File dir = new File("target/temp_files");
        if (dir.exists())
            FileUtils.forceDelete(dir);
        assertFalse(dir.exists());
        manager.stop();
        manager.setDirectory(dir.getAbsolutePath());
        manager.setForceCreateDirectory(Boolean.TRUE);
        assertTrue(manager.start());

        assertTrue(dir.exists());
        assertEquals(0, dir.list().length);

        byte[] arr = {1,2,3,4,5};
        ByteArrayInputStream in = new ByteArrayInputStream(arr);
        DataSource ds1 = manager.saveFile(manager, "test", in, null, true);

        assertEquals(1, dir.list().length);
    }
    
    @Test
    public void createFileTest() throws Exception {
        File file = manager.createFile(manager, "test_file", "test/test");
        assertNotNull(file);
        byte[] buf = new byte[]{1,2,3,4};
        FileUtils.writeByteArrayToFile(file, buf);
        DataSource source = manager.getDataSource("test_file");
        assertNotNull(source);
        checkDataSource(source, buf);
    }

    private void checkDataSource(DataSource source, byte[] expected) throws IOException
    {
        assertNotNull(source);
        assertNotNull(source.getName());
        File file =new File(source.getName());
        assertTrue(file.exists());
        byte[] arr = IOUtils.toByteArray(source.getInputStream());
        assertArrayEquals(expected, arr);
    }
}
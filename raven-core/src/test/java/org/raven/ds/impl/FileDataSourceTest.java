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

import java.io.InputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.raven.test.DataCollector;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class FileDataSourceTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        assertTrue(executor.start());

        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setResetDataPolicy(AbstractDataConsumer.ResetDataPolicy.DONT_RESET_DATA);

        FileDataSource fds = new FileDataSource();
        fds.setName("fds");
        tree.getRootNode().addAndSaveChildren(fds);
        fds.setExecutorService(executor);
        assertTrue(fds.start());

        collector.setDataSource(fds);
        assertTrue(collector.start());

        InputStream is = IOUtils.toInputStream("test");
        fds.getFile().setDataStream(is);
        Thread.sleep(1000);
        List dataList = collector.getDataList();
        assertEquals(2, dataList.size());
        assertNull(dataList.get(0));
        assertTrue(dataList.get(1) instanceof InputStream);
        assertEquals("test", IOUtils.toString((InputStream)dataList.get(1)));

        dataList.clear();
        Object data = collector.refereshData(null);
        assertNotNull(data);
        assertTrue(data instanceof InputStream);
        assertEquals("test", IOUtils.toString((InputStream)data));
    }
}
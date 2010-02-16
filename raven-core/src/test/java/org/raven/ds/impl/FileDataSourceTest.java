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
import org.raven.RavenUtils;
import org.raven.test.DataCollector;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.table.Table;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;

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
        collector.setPauseBeforeRecieve(500);
        collector.setResetDataPolicy(AbstractDataConsumer.ResetDataPolicy.DONT_RESET_DATA);

        FileDataSource fds = new FileDataSource();
        fds.setName("fds");
        tree.getRootNode().addAndSaveChildren(fds);
        fds.setExecutorService(executor);
        assertTrue(fds.start());

        collector.setDataSource(fds);
        assertTrue(collector.start());

        assertEquals(1, fds.getViewableObjects(null).size());
        InputStream is = IOUtils.toInputStream("test");
        fds.getFile().setDataStream(is);
        fds.getFile().setMimeType("text/plain");

        Thread.sleep(200);
        List<ViewableObject> vos = fds.getViewableObjects(null);
        assertNotNull(vos);
        assertEquals(2, vos.size());
        ViewableObject vo;
        vo = vos.get(0);
        Object data = vo.getData();
        assertTrue(data instanceof InputStream);
        assertEquals("test", IOUtils.toString((InputStream)data));
        assertEquals("text/plain", vo.getMimeType());

        vo = vos.get(1);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vo.getMimeType());
        Object voData = vo.getData();
        assertTrue(voData instanceof Table);
        List<Object[]> rows = RavenUtils.tableAsList((Table)voData);
        assertEquals(1, rows.size());
        assertEquals(4l, rows.get(0)[1]);
        assertEquals(0l, rows.get(0)[2]);
        assertEquals(0l, rows.get(0)[3]);

        Thread.sleep(500);
        List dataList = collector.getDataList();
        assertEquals(1, dataList.size());
//        assertNull(dataList.get(0));
        assertTrue(dataList.get(0) instanceof InputStream);
        assertEquals("test", IOUtils.toString((InputStream)dataList.get(0)));
        assertEquals(1, fds.getViewableObjects(null).size());

        dataList.clear();
        data = collector.refereshData(null);
        Thread.sleep(200);
        assertEquals(1, fds.getViewableObjects(null).size());
        Thread.sleep(500);

        assertNotNull(data);
        assertTrue(data instanceof InputStream);
        assertEquals("test", IOUtils.toString((InputStream)data));
    }
}
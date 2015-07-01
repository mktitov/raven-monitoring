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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenUtils;
import org.raven.ds.DataContext;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.table.Table;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
/**
 *
 * @author Mikhail Titov
 */
public class AbstractAsyncDataPipeTest extends RavenCoreTestCase
{
    private ExecutorServiceNode executor;
    private AtomicInteger counter;
    private TestAsyncDataPipe dataPipe;
    private PushDataSource dataSource;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        counter = new AtomicInteger(0);

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(4);
        assertTrue(executor.start());

        dataSource = new PushDataSource();
        dataSource.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(dataSource);
        assertTrue(dataSource.start());

        dataPipe = new TestAsyncDataPipe();
        dataPipe.setName("async data pipe");
        tree.getRootNode().addAndSaveChildren(dataPipe);
        dataPipe.setDataSource(dataSource);
        dataPipe.setExecutor(executor);
        assertTrue(dataPipe.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(dataPipe);
        assertTrue(collector.start());
    }

    @Test
    public void parallelHandlingTest() throws InterruptedException
    {
        long time = System.currentTimeMillis();
        dataSource.pushData("1");
        Thread.sleep(50);
        dataSource.pushData("2");
        assertTrue(System.currentTimeMillis()-time<100);

        Thread.sleep(1100);

        List dataList = collector.getDataList();
        assertNotNull(dataList);
        assertArrayEquals(new Object[]{"1", "2"}, dataList.toArray());
    }

    @Test
    public void nullHandlingTest() throws Exception
    {
        dataSource.pushData("1");
        Thread.sleep(50);
        dataSource.pushData("2");
        dataSource.pushData(null);
        Thread.sleep(1100);

        List dataList = collector.getDataList();
        assertNotNull(dataList);
        assertArrayEquals(new Object[]{"1", "2", null}, dataList.toArray());
    }

    @Test
    public void waitForHandlerTest() throws Exception
    {
        dataPipe.setMaxHandlersCount(1);

        long time = System.currentTimeMillis();
        dataSource.pushData("1");
        dataSource.pushData("2");
        assertTrue(System.currentTimeMillis()-time>1000);

        Thread.sleep(1100);

        List dataList = collector.getDataList();
        assertNotNull(dataList);
        assertArrayEquals(new Object[]{"1", "2"}, dataList.toArray());
    }
    
    @Test
    public void waitForHandlerTimeoutTest() throws Exception
    {
        dataPipe.setMaxHandlersCount(1);
        dataPipe.setWaitForHandlerTimeout(5);

        long time = System.currentTimeMillis();
        DataContext context = new DataContextImpl();
        
        IMocksControl mocks = DataContextImplTest.configureCallbacks(1, 0, context);
        mocks.replay();
        
        dataSource.pushData("1", context);
        assertFalse(context.hasErrors());
        dataSource.pushData("2", context);
        assertTrue(context.hasErrors());

        Thread.sleep(1200);

        List dataList = collector.getDataList();
        assertNotNull(dataList);
        assertArrayEquals(new Object[]{"1"}, dataList.toArray());
        
        mocks.verify();
    }

    @Test
    public void getViewableObjectsTest() throws Exception
    {
        long time = System.currentTimeMillis();
        dataSource.pushData("1");

        List<ViewableObject> vos = dataPipe.getViewableObjects(null);
        assertNotNull(vos);
        assertEquals(2, vos.size());
        assertEquals(Viewable.RAVEN_TEXT_MIMETYPE, vos.get(0).getMimeType());
        assertTrue(vos.get(0).getData() instanceof String);

        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vos.get(1).getMimeType());
        assertTrue(vos.get(1).getData() instanceof Table);

        Table table = (Table) vos.get(1).getData();
        List<Object[]> rows = RavenUtils.tableAsList(table);
        assertNotNull(rows);
        assertEquals(1, rows.size());

        Thread.sleep(1100);

    }

    @Test
    public void handleDataInSameThreadTest() throws Exception {
        dataPipe.setHandleDataInSeparateThread(Boolean.FALSE);
        dataSource.pushData("1");
        assertArrayEquals(new Object[]{"1"}, collector.getDataList().toArray());
    }
    
    @Test
    public void releaseHandlersTest() throws Exception {
        
        assertTrue(dataPipe.getHandlers().isEmpty());
        
        dataPipe.setHandleDataInSeparateThread(Boolean.FALSE);
        dataPipe.setHandlerIdleTime(1);
        dataSource.pushData("1");
        assertArrayEquals(new Object[]{"1"}, collector.getDataList().toArray());
        
        assertEquals(1, dataPipe.getHandlers().size());
        TimeUnit.MILLISECONDS.sleep(AbstractAsyncDataPipe.RELEASE_HANDLERS_INTERVAL+1000);
        assertTrue(dataPipe.getHandlers().isEmpty());
    }
}
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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.raven.ds.DataHandler;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractAsyncDataPipeTest extends RavenCoreTestCase
{
    private ExecutorServiceNode executor;
    private AtomicInteger counter;
    private AsyncDataPipe dataPipe;
    private PushDataSource dataSource;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        counter = new AtomicInteger(0);

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(2);
        assertTrue(executor.start());

        dataSource = new PushDataSource();
        dataSource.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(dataSource);
        assertTrue(dataSource.start());

        dataPipe = new AsyncDataPipe();
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
}
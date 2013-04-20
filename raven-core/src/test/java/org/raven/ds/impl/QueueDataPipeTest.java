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

import org.junit.Before;
import org.junit.Test;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.DataCollector;
import org.raven.test.InThreadExecutorService;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class QueueDataPipeTest extends RavenCoreTestCase {
    private ExecutorServiceNode executor;
    private PushDataSource ds;
    private DataCollector collector;
    private QueueDataPipe queue;
    
    @Before
    public void prepare() {
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(4);
        executor.setMaximumQueueSize(4);
        assertTrue(executor.start());
        
        ds = new PushDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        queue = new QueueDataPipe();
        queue.setName("queuePipe");
        tree.getRootNode().addAndSaveChildren(queue);
        queue.setDataSource(ds);
        queue.setExecutor(executor);
        assertTrue(queue.start());
        
        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(queue);
        assertTrue(collector.start());
    }
    
    @Test
    public void activeTest() throws Exception {
        for (int i=0; i<512; ++i)
            ds.pushData(i);
        Thread.sleep(1000);
        assertEquals(512, collector.getDataListSize());
    }
    
    @Test
    public void activeTest2() throws Exception {
        for (int i=0; i<512; ++i) {
            ds.pushData(i);
            Thread.sleep(1);
        }
        Thread.sleep(1000);
        assertEquals(512, collector.getDataListSize());
    }
    
    @Test
    public void activeTest3() throws Exception {
        for (int i=0; i<512; ++i) {
            ds.pushData(i);
            Thread.sleep(i%100==0?1100 : 1);
        }
        Thread.sleep(7000);
        assertEquals(512, collector.getDataListSize());
    }
    
    @Test
    public void activeOnFullTest() throws Exception {
        queue.stop();
        queue.setQueueType(QueueDataPipe.QueueType.ACTIVE_ON_FULL);
        queue.setExecutor(createInThreadExecutor());
        queue.setQueueSize(2);
        assertTrue(queue.start());
        
        ds.pushData(1);
        ds.pushData(2);
        assertEquals(0, collector.getDataListSize());
        ds.pushData(3);
        assertEquals(3, collector.getDataListSize());
        assertArrayEquals(new Object[]{1,2,null}, collector.getDataList().toArray());
        collector.getDataList().clear();
        ds.pushData(4);
        ds.pushData(5);
        assertArrayEquals(new Object[]{3,4,null}, collector.getDataList().toArray());
    }
    
    @Test
    public void passiveTest() throws Exception {
        queue.stop();
        queue.setQueueType(QueueDataPipe.QueueType.PASSIVE);
        queue.setExecutor(createInThreadExecutor());
        assertTrue(queue.start());

        ds.pushData(1);
        ds.pushData(2);
        assertEquals(0, collector.getDataListSize());
        collector.refereshData(null);
        assertEquals(3, collector.getDataListSize());
        assertArrayEquals(new Object[]{1,2,null}, collector.getDataList().toArray());
    }
    
    @Test
    public void passiveThresholdTest() throws Exception {
        queue.stop();
        queue.setQueueType(QueueDataPipe.QueueType.PASSIVE);
        queue.setExecutor(createInThreadExecutor());
        queue.setDataCountThreshold(2);
        assertTrue(queue.start());

        ds.pushData(1);
        collector.refereshData(null);
        assertEquals(0, collector.getDataListSize());
        ds.pushData(2);
        collector.refereshData(null);
        assertEquals(3, collector.getDataListSize());
        assertArrayEquals(new Object[]{1,2,null}, collector.getDataList().toArray());
    }
    
    @Test
    public void dataLifetimeTest() throws Exception {
        queue.stop();
        queue.setDataLifetime(1);
        queue.setQueueType(QueueDataPipe.QueueType.PASSIVE);
        queue.start();
        
        ds.pushData(1);
        Thread.sleep(100);
        collector.refereshData(null);
        Thread.sleep(100);
        assertEquals(2, collector.getDataListSize());
        collector.getDataList().clear();
        
        ds.pushData(1);
        Thread.sleep(500);
        ds.pushData(2);
        Thread.sleep(600);
        collector.refereshData(null);
        Thread.sleep(100);
        assertEquals(2, collector.getDataListSize());
        assertArrayEquals(new Object[]{2, null}, collector.getDataList().toArray());
    }
    
    public ExecutorService createInThreadExecutor() {
        InThreadExecutorService executor = new InThreadExecutorService();
        executor.setName("in thread executor");
        testsNode.addAndSaveChildren(executor);
        assertTrue(executor.start());
        return executor;
    }
}

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
    private QueueDataPipe queuePipe;
    
    @Before
    public void prepare() {
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(2);
        executor.setMaximumQueueSize(2);
        assertTrue(executor.start());
        
        ds = new PushDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        queuePipe = new QueueDataPipe();
        queuePipe.setName("queuePipe");
        tree.getRootNode().addAndSaveChildren(queuePipe);
        queuePipe.setDataSource(ds);
        queuePipe.setExecutor(executor);
        assertTrue(queuePipe.start());
        
        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(queuePipe);
        assertTrue(collector.start());
    }
    
    @Test
    public void test() throws Exception {
        for (int i=0; i<512; ++i)
            ds.pushData(i);
        Thread.sleep(1000);
        assertEquals(512, collector.getDataListSize());
    }
    
    @Test
    public void test2() throws Exception {
        for (int i=0; i<512; ++i) {
            ds.pushData(i);
            Thread.sleep(1);
        }
        Thread.sleep(1000);
        assertEquals(512, collector.getDataListSize());
    }
    
    @Test
    public void test3() throws Exception {
        for (int i=0; i<512; ++i) {
            ds.pushData(i);
            Thread.sleep(i%100==0?1100 : 1);
        }
        Thread.sleep(7000);
        assertEquals(512, collector.getDataListSize());
    }
}

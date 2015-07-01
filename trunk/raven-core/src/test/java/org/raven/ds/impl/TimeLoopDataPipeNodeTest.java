/*
 * Copyright 2014 Mikhail Titov.
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

import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class TimeLoopDataPipeNodeTest extends RavenCoreTestCase {
    
    @Test
    public void test() throws InterruptedException {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        assertTrue(executor.start());
        
        PushDataSource ds = new PushDataSource();
        ds.setName("data source");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        TimeLoopDataPipeNode timeLoop = new TimeLoopDataPipeNode();
        timeLoop.setName("time loop");
        testsNode.addAndSaveChildren(timeLoop);
        timeLoop.setExecutor(executor);
        timeLoop.setDelayTime(1l);
        timeLoop.setDelayTimeUnit(TimeUnit.SECONDS);
        timeLoop.setDataSource(ds);
        assertTrue(timeLoop.start());
        
        DataCollector collector = new DataCollector();
        collector.setName("collector");
        testsNode.addAndSaveChildren(collector);
        collector.setDataSource(timeLoop);
        assertTrue(collector.start());
        
        //test
        ds.pushData("test");
        Thread.sleep(900l);
        assertEquals(0, collector.getDataListSize());
        Thread.sleep(110);
        assertEquals(1, collector.getDataListSize());
        assertEquals("test", collector.getDataList().get(0));
    }
}

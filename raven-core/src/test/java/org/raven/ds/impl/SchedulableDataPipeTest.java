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

import org.junit.Before;
import org.junit.Test;
import org.raven.TestScheduler;
import org.raven.test.DataCollector;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Milhail Titov
 */
public class SchedulableDataPipeTest extends RavenCoreTestCase
{
    private SchedulableDataPipe schedulablePipe;
    private PushOnDemandDataSource ds;
    private DataCollector collector;
    private TestScheduler scheduler;

    @Before
    public void prepare()
    {
        ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        scheduler = new TestScheduler();
        scheduler.setName("scheduler");
        tree.getRootNode().addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());

        schedulablePipe = new SchedulableDataPipe();
        schedulablePipe.setName("schedule");
        tree.getRootNode().addAndSaveChildren(schedulablePipe);
        schedulablePipe.setDataSource(ds);
        schedulablePipe.setScheduler(scheduler);
        assertTrue(schedulablePipe.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(schedulablePipe);
        assertTrue(collector.start());
    }

    @Test
    public void singleExecutionTest()
    {
        ds.addDataPortion(1);
        schedulablePipe.executeScheduledJob(null);
        assertEquals(1, collector.getDataListSize());
        assertEquals(1, collector.getDataList().get(0));
    }

    @Test
    public void asyncExecutionTest() throws Exception
    {
        ds.addDataPortion(1);
        schedulablePipe.setUseExpression(Boolean.TRUE);
        schedulablePipe.setExpression("Thread.sleep(200); data");
        schedulablePipe.setAllowAsyncExecution(Boolean.TRUE);
        new Thread(new Runnable() {
            public void run() {
                schedulablePipe.executeScheduledJob(null);
            }
        }).start();
        Thread.sleep(100);
        new Thread(new Runnable() {
            public void run() {
                schedulablePipe.executeScheduledJob(null);
            }
        }).start();
        Thread.sleep(300);
        
        assertEquals(2, collector.getDataListSize());
        assertEquals(1, collector.getDataList().get(0));
        assertEquals(1, collector.getDataList().get(1));
    }

    @Test
    public void nonAsyncExecutionTest() throws Exception
    {
        ds.addDataPortion(1);
        schedulablePipe.setUseExpression(Boolean.TRUE);
        schedulablePipe.setExpression("Thread.sleep(200); data");
        schedulablePipe.setAllowAsyncExecution(Boolean.FALSE);
        new Thread(new Runnable() {
            public void run() {
                schedulablePipe.executeScheduledJob(null);
            }
        }).start();
        Thread.sleep(100);
        new Thread(new Runnable() {
            public void run() {
                schedulablePipe.executeScheduledJob(null);
            }
        }).start();
        Thread.sleep(300);

        assertEquals(1, collector.getDataListSize());
        assertEquals(1, collector.getDataList().get(0));
    }
}
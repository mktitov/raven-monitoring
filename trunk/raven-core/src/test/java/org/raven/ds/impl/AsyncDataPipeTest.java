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
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class AsyncDataPipeTest extends RavenCoreTestCase
{
    private AsyncDataPipe pipe;
    private PushDataSource ds;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        ExecutorService executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        assertTrue(executor.start());

        ds = new PushDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        pipe = new AsyncDataPipe();
        pipe.setName("pipe");
        tree.getRootNode().addAndSaveChildren(pipe);
        pipe.setExecutor(executor);
        pipe.setHandleDataInSeparateThread(false);
        pipe.setDataSource(ds);
        assertTrue(pipe.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(pipe);
        assertTrue(collector.start());
    }

    @Test
    public void test()
    {
        ds.pushData("test");

        assertEquals(1, collector.getDataListSize());
        assertEquals("test", collector.getDataList().get(0));
    }

    @Test
    public void testHandlerExpression()
    {
        pipe.setUseHandlerExpression(Boolean.TRUE);
        pipe.setHandlerExpression("data+'2'");
        
        ds.pushData("test");

        assertEquals(1, collector.getDataListSize());
        assertEquals("test2", collector.getDataList().get(0));
    }

    @Test
    public void testSkipData()
    {
        pipe.setUseHandlerExpression(Boolean.TRUE);
        pipe.setHandlerExpression("SKIP_DATA");

        ds.pushData("test");

        assertEquals(0, collector.getDataListSize());
    }
}
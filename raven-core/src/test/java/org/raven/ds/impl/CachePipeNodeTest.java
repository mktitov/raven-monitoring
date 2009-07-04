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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.raven.DataCollector;
import org.raven.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.NodeAttribute;
import org.weda.internal.CacheScope;

/**
 *
 * @author Mikhail Titov
 */
public class CachePipeNodeTest extends RavenCoreTestCase
{
    private PushOnDemandDataSource ds;
    private CachePipeNode cachePipe;
    private DataCollector collector1;
    private NodeAttribute activeConsumerAttr;

    @Before
    public void prepare() throws Exception
    {
        ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        ds.addDataPortion("test");

        cachePipe = new CachePipeNode();
        cachePipe.setName("cachePipe");
        tree.getRootNode().addAndSaveChildren(cachePipe);
        cachePipe.setDataSource(ds);
        cachePipe.setCacheScope(CacheScope.GLOBAL);
        cachePipe.setExpirationTime(2000l);
        cachePipe.setWaitTimeout(1000l);
        assertTrue(cachePipe.start());

        collector1 = new DataCollector();
        collector1.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector1);
        collector1.setDataSource(cachePipe);
        activeConsumerAttr = collector1.getNodeAttribute(CachePipeNode.CONSUMER_TYPE_ATTR);
        assertNotNull(activeConsumerAttr);
        activeConsumerAttr.setValue("false");
        assertTrue(collector1.start());
        
    }

    @Test
    public void waitTimeoutTest() throws Exception
    {
        long startTime = System.currentTimeMillis();
        collector1.refereshData(null);
        long endTime = System.currentTimeMillis();
        assertTrue(endTime-startTime>=1000l);
        assertEquals(0, collector1.getDataList().size());
    }

    @Test
    public void activeConsumerTest() throws Exception
    {
        activeConsumerAttr.setValue("true");
        collector1.refereshData(null);
        assertEquals(1, collector1.getDataList().size());
        assertEquals("test", collector1.getDataList().get(0));
    }

    @Test
    public void activeAndPassiveConsumersTest() throws Exception
    {
        final DataCollector collector2 = new DataCollector();
        collector2.setName("collector2");
        tree.getRootNode().addAndSaveChildren(collector2);
        collector2.setDataSource(cachePipe);
        assertTrue(collector2.start());

        final DataCollector collector3 = new DataCollector();
        collector3.setName("collector3");
        tree.getRootNode().addAndSaveChildren(collector3);
        collector3.setDataSource(cachePipe);
        assertTrue(collector3.start());

        activeConsumerAttr.setValue("true");

        CollectorExecutor executor1 = new CollectorExecutor(collector1);
        CollectorExecutor executor2 = new CollectorExecutor(collector2);
        CollectorExecutor executor3 = new CollectorExecutor(collector3);

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.execute(executor2);
        TimeUnit.MILLISECONDS.sleep(500l);
        executorService.execute(executor1);
        executorService.execute(executor3);

        executorService.shutdown();
        assertTrue(executorService.awaitTermination(200l, TimeUnit.MILLISECONDS));

        checkCollector(collector1);
//        checkCollector(collector2);
        checkCollector(collector3);
    }

    private void checkCollector(DataCollector collector)
    {
        assertEquals(1, collector.getDataList().size());
        assertEquals("test", collector.getDataList().get(0));
    }

    private class CollectorExecutor implements Runnable
    {
        private final DataCollector collector;

        public CollectorExecutor(DataCollector collector)
        {
            this.collector = collector;
        }

        public void run()
        {
            collector.refereshData(null);
        }
    }
}
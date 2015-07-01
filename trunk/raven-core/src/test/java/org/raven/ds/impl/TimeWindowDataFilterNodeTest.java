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

import org.junit.*;
import org.raven.sched.impl.TimeWindowNode;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class TimeWindowDataFilterNodeTest extends RavenCoreTestCase {
    
    private TimeWindowDataFilterNode filter;
    private PushOnDemandDataSource ds1;
    private PushDataSource ds2;
    private DataCollector collector;
    
    @Before
    public void prepare() {
        ds1 = new PushOnDemandDataSource();
        ds1.setName("ds1");
        tree.getRootNode().addAndSaveChildren(ds1);
        assertTrue(ds1.start());
        
        ds2 = new PushDataSource();
        ds2.setName("ds2");
        tree.getRootNode().addAndSaveChildren(ds2);
        assertTrue(ds2.start());
        
        filter = new TimeWindowDataFilterNode();
        filter.setName("filter");
        tree.getRootNode().addAndSaveChildren(filter);
        
        TimeWindowNode window = new TimeWindowNode();
        window.setName("window");
        filter.addAndSaveChildren(window);
        window.setTimePeriods("00:00-23:59");
        assertTrue(window.start());
        
        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(filter);
        assertTrue(collector.start());
    }
    
    @Test
    public void test() {
        filter.setDataSource(ds1);
        assertTrue(filter.start());
        
        ds1.addDataPortion("test");
        collector.refereshData(null);
        assertEquals(1, collector.getDataListSize());
        assertEquals("test", collector.getDataList().get(0));
        
        collector.getDataList().clear();
        filter.setInvertResult(Boolean.TRUE);
        ds1.addDataPortion("test2");
        collector.refereshData(null);
        assertEquals(0, collector.getDataListSize());
    }
    
    @Test
    public void pushFilterTest() {
        filter.setDataSource(ds2);
        filter.setInvertResult(Boolean.TRUE);
        filter.setFilterOnPush(false);
        assertTrue(filter.start());
        
        ds2.pushData("test");
        assertEquals(1, collector.getDataListSize());
        assertEquals("test", collector.getDataList().get(0));
    }
    
    @Test
    public void pullFilterTest() {
        filter.setDataSource(ds1);
        filter.setInvertResult(Boolean.TRUE);
        filter.setFilterOnPush(false);
        assertTrue(filter.start());
        
        ds1.addDataPortion("test");
        collector.refereshData(null);
        assertEquals(0, collector.getDataListSize());
        
        filter.setFilterOnPull(false);
        collector.getDataList().clear();
        collector.refereshData(null);
        assertEquals(1, collector.getDataListSize());
        assertEquals("test", collector.getDataList().get(0));
        
    }
    
}

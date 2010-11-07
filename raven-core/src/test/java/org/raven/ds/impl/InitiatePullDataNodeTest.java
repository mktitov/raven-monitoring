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
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class InitiatePullDataNodeTest extends RavenCoreTestCase
{
    private PushDataSource ds1;
    private PushOnDemandDataSource ds2;
    private InitiatePullDataNode pipe;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        ds1 = new PushDataSource();
        ds1.setName("ds1");
        tree.getRootNode().addAndSaveChildren(ds1);
        assertTrue(ds1.start());

        ds2 = new PushOnDemandDataSource();
        ds2.setName("ds2");
        tree.getRootNode().addAndSaveChildren(ds2);
        assertTrue(ds2.start());

        pipe = new InitiatePullDataNode();
        pipe.setName("pipe");
        tree.getRootNode().addAndSaveChildren(pipe);
        pipe.setDataSource(ds1);
        pipe.setPullDataFrom(ds2);
        assertTrue(pipe.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(pipe);
        assertTrue(collector.start());
    }

    @Test
    public void passBothTest()
    {
        ds2.addDataPortion("Hello");
        ds1.pushData("World");

        assertEquals(2, collector.getDataListSize());
        assertEquals("Hello", collector.getDataList().get(0));
        assertEquals("World", collector.getDataList().get(1));
    }

    @Test
    public void passDataSourceTest()
    {
        pipe.setDataMixPolicy(InitiatePullDataNode.DataMixPolicy.PATH_DATASOURCE);
        ds2.addDataPortion("Hello");
        ds1.pushData("World");

        assertEquals(1, collector.getDataListSize());
        assertEquals("World", collector.getDataList().get(0));
    }

    @Test
    public void passNewDataSourceTest()
    {
        pipe.setDataMixPolicy(InitiatePullDataNode.DataMixPolicy.PASS_NEW_DATASOURCE);
        ds2.addDataPortion("Hello");
        ds1.pushData("World");

        assertEquals(1, collector.getDataListSize());
        assertEquals("Hello", collector.getDataList().get(0));
    }
}
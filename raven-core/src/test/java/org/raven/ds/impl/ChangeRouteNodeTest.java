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
import static org.junit.Assert.*;
import org.raven.log.LogLevel;
import org.raven.test.DataCollector;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class ChangeRouteNodeTest extends RavenCoreTestCase
{
    private ChangeRouteNode pipe;
    private DataCollector c1, c2;
    private PushOnDemandDataSource ds;

    @Before
    public void prepare()
    {
        ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.setLogLevel(LogLevel.DEBUG);
        assertTrue(ds.start());

        pipe = new ChangeRouteNode();
        pipe.setName("pipe");
        tree.getRootNode().addAndSaveChildren(pipe);
        pipe.setDataSource(ds);

        c1 = new DataCollector();
        c1.setName("c1");
        tree.getRootNode().addAndSaveChildren(c1);
        c1.setResetDataPolicy(AbstractDataConsumer.ResetDataPolicy.DONT_RESET_DATA);
        c1.setDataSource(pipe);
        assertTrue(c1.start());

        c2 = new DataCollector();
        c2.setName("c2");
        tree.getRootNode().addAndSaveChildren(c2);
        c2.setResetDataPolicy(AbstractDataConsumer.ResetDataPolicy.DONT_RESET_DATA);
        c2.setDataSource(pipe);
        assertTrue(c2.start());

        pipe.setRouteDataTo(c2);
        assertTrue(pipe.start());
    }

    @Test
    public void test()
    {
        pipe.setDataSource(ds);
        pipe.setRouteDataTo(c2);
        assertTrue(pipe.start());

        ds.addDataPortion("1");

        assertNull(c1.refereshData(null));
        assertEquals(0, c1.getDataListSize());
        assertEquals(1, c2.getDataList().size());
        assertEquals("1", c2.getDataList().get(0));
    }
}
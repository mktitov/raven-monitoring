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

import org.junit.Test;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class SafeDataConsumerTest extends RavenCoreTestCase
{
    @Test
    public void refreshDataTest()
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        SafeDataConsumer consumer = new SafeDataConsumer();
        consumer.setName("consumer");
        tree.getRootNode().addAndSaveChildren(consumer);
        consumer.setDataSource(ds);
        assertTrue(consumer.start());

        ds.addDataPortion("test");
        assertEquals("test", consumer.refereshData(null));
    }

    @Test
    public void refreshDataWithExpressionTest()
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        SafeDataConsumer consumer = new SafeDataConsumer();
        consumer.setName("consumer");
        tree.getRootNode().addAndSaveChildren(consumer);
        consumer.setDataSource(ds);
        consumer.setUseExpression(true);
        consumer.setExpression("data+1");
        assertTrue(consumer.start());

        ds.addDataPortion(1);
        assertEquals(2, consumer.refereshData(null));
    }
}
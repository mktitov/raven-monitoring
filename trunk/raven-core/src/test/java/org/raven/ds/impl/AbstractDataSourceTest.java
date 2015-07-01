/*
 *  Copyright 2008 Mikhail Titov.
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

import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.impl.objects.TestDataConsumer;
import org.raven.ds.impl.objects.TestDataSource;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractDataSourceTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        TestDataSource dataSource = new TestDataSource();
        dataSource.setName("ds");
        tree.getRootNode().addChildren(dataSource);
        
        tree.saveNode(dataSource);
        
        dataSource.init();

        NodeAttribute attr = dataSource.getNodeAttribute("taskCount");
        assertNotNull(attr);
        assertTrue(attr.isReadonly());
        
        TestDataConsumer dataConsumer = new TestDataConsumer();
        dataConsumer.setName("consumer");
        tree.getRootNode().addChildren(dataConsumer);
        
        tree.saveNode(dataConsumer);
        
        dataConsumer.init();
        
        dataConsumer.getNodeAttribute("dataSource").setValue(dataSource.getPath());
        dataConsumer.getNodeAttribute("interval").setValue("2");
        dataConsumer.getNodeAttribute("intervalUnit").setValue(TimeUnit.SECONDS.toString());
        
        dataConsumer.start();
        
        dataSource.start();
        //
        TimeUnit.SECONDS.sleep(3);
        dataSource.stop();
        
        assertEquals(2, dataConsumer.getExecutionCount());
        
        TimeUnit.SECONDS.sleep(2);
        assertEquals(2, dataConsumer.getExecutionCount());

        //
        dataSource.start();
        TimeUnit.SECONDS.sleep(3);
        dataSource.stop();
        
        assertEquals(4, dataConsumer.getExecutionCount());
        
        //
        dataConsumer.shutdown();
        dataSource.start();
        TimeUnit.SECONDS.sleep(3);
        dataSource.stop();
        
        assertEquals(4, dataConsumer.getExecutionCount());
    }
}

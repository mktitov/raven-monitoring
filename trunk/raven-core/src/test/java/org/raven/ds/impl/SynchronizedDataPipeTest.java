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
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class SynchronizedDataPipeTest extends RavenCoreTestCase {
    
    @Test
    public void test() throws InterruptedException {
        final PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        SynchronizedDataPipe pipe = new SynchronizedDataPipe();
//        SafeDataPipeNode pipe = new SafeDataPipeNode();
        pipe.setName("pipe");
        tree.getRootNode().addAndSaveChildren(pipe);
        pipe.setDataSource(ds);
        pipe.setExpression("context['thread']=Thread.currentThread(); Thread.sleep(1)");
        pipe.setUseExpression(Boolean.TRUE);
        assertTrue(pipe.start());
        
        SafeDataPipeNode pipe2 = new SafeDataPipeNode();
        pipe2.setName("pipe2");
        tree.getRootNode().addAndSaveChildren(pipe2);
        pipe2.setDataSource(pipe);
        pipe2.setUseExpression(Boolean.TRUE);
        pipe2.setExpression("context['thread']==Thread.currentThread()? 1:0");
        assertTrue(pipe2.start());
        
        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(pipe2);
        assertTrue(collector.start());
        
        final DataContextImpl context = new DataContextImpl();
        for (int i=0; i<100; i++) 
            new Thread(new Runnable() {
                public void run() {
                    ds.pushData("test", context);
                }
            }).start();
        
        Thread.sleep(1000);
        assertEquals(100, collector.getDataList().size());
        int sum = 0;
        for (Object o: collector.getDataList())
            sum += (Integer)o;
        assertEquals(100, sum);
    }
}

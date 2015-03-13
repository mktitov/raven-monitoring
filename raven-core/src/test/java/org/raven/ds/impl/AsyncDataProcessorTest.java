/*
 * Copyright 2015 Mikhail Titov.
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

import org.junit.Before;
import org.junit.Test;
import org.raven.test.InThreadExecutorService;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.DataProcessor;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
/**
 *
 * @author Mikhail Titov
 */
public class AsyncDataProcessorTest extends RavenCoreTestCase {
    
    
    @Before
    public void prepare() {
    }
    
    @Test
    public void singlePacketTest() throws Exception {
        DataProcessor realProcessor = createMock(DataProcessor.class);
        expect(realProcessor.processData("test")).andReturn(Boolean.TRUE);
        replay(realProcessor);
        
        InThreadExecutorService executor = new InThreadExecutorService();
        AsyncDataProcessor processor = new AsyncDataProcessor(executor, realProcessor, executor);
        processor.processData("test");
        
        verify(realProcessor);
    }
    
    @Test(timeout = 500l)
    public void capacityTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor realProcessor = createMock(DataProcessor.class);
        expect(realProcessor.processData(checkDataPacket("test", 100))).andReturn(Boolean.TRUE);
        expect(realProcessor.processData(checkDataPacket("test3",0))).andReturn(Boolean.TRUE);
        replay(realProcessor);        
        
        AsyncDataProcessor processor = new AsyncDataProcessor(executor, realProcessor, executor, 1);
        assertTrue(processor.processData("test"));
        assertFalse(processor.processData("test2"));
        Thread.sleep(110);
        assertTrue(processor.processData("test3"));
        
        Thread.sleep(200l);
        
        verify(realProcessor);
        
    }
    
    @Test(timeout = 4000l)
    public void loadTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor realProcessor = createMock(DataProcessor.class);
        expect(realProcessor.processData(checkDataPacket("test", 1))).andReturn(Boolean.TRUE).times(1000);
        replay(realProcessor);
        
        
        AsyncDataProcessor processor = new AsyncDataProcessor(executor, realProcessor, executor);
        Thread source1 = new Thread(new Source(processor, 1, 500));
        Thread source2 = new Thread(new Source(processor, 2, 500));
        source1.start();
        source2.start();
        
        Thread.sleep(2000);
        verify(realProcessor);
    }
    
    private class Source implements Runnable {
        private final DataProcessor processor;
        private final long pause;
        private final int count;

        public Source(DataProcessor processor, long pause, int count) {
            this.processor = processor;
            this.pause = pause;
            this.count = count;
        }
        
        public void run() {
            try {
                for (int i=0; i<count; ++i) {
                    processor.processData("test");
                    Thread.sleep(pause);
                }
            } catch (Exception e) {
                
            }
        }        
    }
    
    public static String checkDataPacket(final String dataPacket, final long pause) {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                assertEquals(dataPacket, argument);
                try {
                    if (pause>0)
                        Thread.sleep(pause);
                } catch (InterruptedException ex) {
                }
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
    
    private ExecutorService createExecutor() {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setMaximumQueueSize(100);
        assertTrue(executor.start());
        
        return executor;
    }
}

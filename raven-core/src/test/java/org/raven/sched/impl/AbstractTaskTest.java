/*
 * Copyright 2014 Mikhail Titov.
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

package org.raven.sched.impl;

import java.util.concurrent.atomic.AtomicBoolean;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import static org.easymock.EasyMock.*;
import org.raven.sched.CancelationProcessor;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractTaskTest extends Assert {

    @Test
    public void executionTest() {
        final AtomicBoolean executed = new AtomicBoolean();
        AbstractTask task = new AbstractTask(null, null) {
            @Override public void doRun() throws Exception {
                executed.set(true);
            }
        };
        assertFalse(task.isExecuted());
        task.run();
        assertTrue(executed.get());
        assertTrue(task.isExecuted());
    }
    
    @Test
    public void cancelTest() {
        final AtomicBoolean executed = new AtomicBoolean();
        AbstractTask task = new AbstractTask(null, null) {
            @Override public void doRun() throws Exception {
                executed.set(true);
            }
        };
        assertFalse(task.isExecuted());
        assertFalse(task.isCanceled());
        task.cancel();
        task.run();
        assertTrue(task.isCanceled());
        assertFalse(task.isExecuted());
        assertFalse(executed.get());
    }
    
    @Test
    public void cancelWithProcessorTest() {
        CancelationProcessor processor = createMock(CancelationProcessor.class);
        processor.cancel();
        
        replay(processor);
        
        final AtomicBoolean executed = new AtomicBoolean();
        AbstractTask task = new AbstractTask(null, null) {
            @Override public void doRun() throws Exception {
                executed.set(true);
            }
        };
        task.setCancelationProcessor(processor);
        assertFalse(task.isExecuted());
        assertFalse(task.isCanceled());
        task.cancel();
        task.run();
        assertTrue(task.isCanceled());
        assertFalse(task.isExecuted());
        assertFalse(executed.get());
        
        verify(processor);
    }
}

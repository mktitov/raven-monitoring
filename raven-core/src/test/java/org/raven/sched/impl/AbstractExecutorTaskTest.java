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
package org.raven.sched.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.sched.ExecutorService;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractExecutorTaskTest extends RavenCoreTestCase {
    private ExecutorServiceNode executor;
    
    @Before
    public void prepare() {
        executor = new ExecutorServiceNode();
        executor.setName("Executor");
        testsNode.addAndSaveChildren(executor);
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        executor.setCorePoolSize(8);
        assertTrue(executor.start());
    }
    
    @Test
    public void test() throws Exception {
        Task task = new Task();
        for (int i=0; i<5; ++i) {
            final CountDownLatch latch = new CountDownLatch(2);
            task.setLatch(latch);
            executor.execute(task);
            assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        }
    }
    
    @Test
    public void reexecuteTask() throws Exception {
        Task task = new Task(true);
        final CountDownLatch latch = new CountDownLatch(100);
        task.setLatch(latch);
        executor.execute(task);
        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
    }
    
    private class Task extends AbstractExecutorTask {
        private  CountDownLatch latch;
        private final boolean reexecute;

        public Task() {
            this(false);
        }

        public Task(boolean reexecute) {
            this.reexecute = reexecute;
        }

        public void setLatch(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public ForkJoinTask getForJoinTask() {
            latch.countDown();
            ForkJoinTask res =  super.getForJoinTask();
            System.out.println("Getting FJT: "+res);
            return res;
        }

        @Override
        public void doRun() throws Exception {
            System.out.println("\nExecuting!!\n");
            latch.countDown();
            if (latch.getCount()!=0 && reexecute)
                executor.execute(this);
        }

        @Override
        public Node getTaskNode() {
            return testsNode;
        }

        @Override
        public String getStatusMessage() {
            return "";
        }
        
    }
}

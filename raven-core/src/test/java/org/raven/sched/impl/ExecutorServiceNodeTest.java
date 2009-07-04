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

package org.raven.sched.impl;

import java.io.IOException;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class ExecutorServiceNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws ExecutorServiceException, InterruptedException, IOException
    {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(1);
        executor.setMaximumPoolSize(2);
        executor.setMaximumQueueSize(1);
        executor.setLogLevel(LogLevel.DEBUG);
        assertTrue(executor.start());

        executor.execute(new TestTask(executor, 500));
        executor.execute(new TestTask(executor, 500));
        executor.execute(new TestTask(executor, 500));
        executor.execute(new TestTask(executor, 0));


        Thread.sleep(100);
        assertEquals(new Integer(2), executor.getExecutingTaskCount());

        Thread.sleep(1000);

        assertEquals(new Integer(0), executor.getExecutingTaskCount());
        assertEquals(3, executor.getExecutedTasks().getOperationsCount());
        assertEquals(1l, executor.getRejectedTasks().get());
    }

    private class TestTask implements Task
    {
        private final Node initiator;
        private final long sleepInterval;

        public TestTask(Node initiator, long sleepInterval) {
            this.initiator = initiator;
            this.sleepInterval = sleepInterval;
        }

        public Node getTaskNode() {
            return initiator;
        }

        public String getStatusMessage() {
            return "status message";
        }

        public void run() {
            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException ex) {
            }
        }
    }
}
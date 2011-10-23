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
import java.util.List;
import org.junit.Test;
import org.raven.RavenUtils;
import org.raven.test.RavenCoreTestCase;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.table.Table;
import org.raven.tree.Node;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class ExecutorServiceNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws ExecutorServiceException, InterruptedException, IOException, Exception
    {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(2);
        executor.setMaximumPoolSize(3);
        executor.setMaximumQueueSize(1);
        executor.setLogLevel(LogLevel.DEBUG);
        assertTrue(executor.start());

        executor.execute(new TestTask(executor, 500));
        assertTrue(executor.executeQuietly(new TestTask(executor, 500)));
        executor.execute(new TestTask(executor, 500));
        try {
            executor.execute(new TestTask(executor, 0));
            fail();
        } catch (ExecutorServiceException executorServiceException) {
        }

        Thread.sleep(100);
        assertEquals(new Integer(2+1), executor.getExecutingTaskCount()); //+1 is the delayed tasks executor
        List<ViewableObject> vos = executor.getViewableObjects(null);
        assertNotNull(vos);
        assertEquals(1, vos.size());
        Object data = vos.get(0).getData();
        assertTrue(data instanceof Table);
        List<Object[]> rows = RavenUtils.tableAsList((Table)data);
        assertNotNull(rows);
        assertEquals(2+1, rows.size()); //+1 is the delayed tasks executor
        assertEquals(executor.getPath(), rows.get(1)[1]);
        assertEquals("status message", rows.get(1)[2]);
        assertEquals(Thread.State.TIMED_WAITING.toString(), rows.get(1)[5]);
        assertTrue(rows.get(1)[6] instanceof ViewableObject);
        ViewableObject vo = (ViewableObject) rows.get(1)[6];
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vo.getMimeType());
        assertTrue(vo.getData() instanceof Table);
        Table trace = (Table)vo.getData();
        rows = RavenUtils.tableAsList(trace);
        assertTrue(rows.size()>0);
        System.out.println("\nTRACE: \n");
        for (Object[] row: rows)
            System.out.println(row[0]);
        

        Thread.sleep(1000);

        assertEquals(new Integer(0+1), executor.getExecutingTaskCount()); //+1 is the delayed tasks executor
        assertEquals(4, executor.getExecutedTasks().getOperationsCount());
        assertEquals(1l, executor.getRejectedTasks().get());
    }

    @Test
    public void delayedTasksTest() throws InterruptedException {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(2);
        executor.setMaximumPoolSize(2);
        executor.setMaximumQueueSize(1);
        executor.setLogLevel(LogLevel.DEBUG);
        assertTrue(executor.start());

        TestTask task1 = new TestTask(executor, 0);
        TestTask task2 = new TestTask(executor, 0);
        assertTrue(executor.executeQuietly(100, task2));
        assertTrue(executor.executeQuietly(50, task1));
        Thread.sleep(150);
        assertTrue(task1.executed);
        assertTrue(task2.executed);
        assertTrue(task1.time < task2.time);
    }

    private class TestTask implements Task {
        private final Node initiator;
        private final long sleepInterval;
        private boolean executed;
        private long time;

        public TestTask(Node initiator, long sleepInterval) {
            this.initiator = initiator;
            this.sleepInterval = sleepInterval;
        }

        public Node getTaskNode() {
            return initiator;
        }

        public boolean isExecuted() {
            return executed;
        }

        public String getStatusMessage() {
            return "status message";
        }

        public void run() {
            try {
                Thread.sleep(sleepInterval);
                executed = true;
                time = System.currentTimeMillis();
            } catch (InterruptedException ex) {
            }
        }
    }
}
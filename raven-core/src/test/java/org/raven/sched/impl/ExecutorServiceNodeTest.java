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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenUtils;
import org.raven.test.RavenCoreTestCase;
import org.raven.log.LogLevel;
import org.raven.sched.CancelableTask;
import org.raven.sched.CancelationProcessor;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.sched.TaskRestartPolicy;
import org.raven.table.Table;
import org.raven.tree.Node;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class ExecutorServiceNodeTest extends RavenCoreTestCase {
    
    private ExecutorServiceNode executor;
    private CountDownLatch executedTasks;

    @Before
    public void prepare() {
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
//        executor.setCacheTaskWrappers(Boolean.TRUE);
    }
    
    @Test
    public void performanceTest() throws InterruptedException {
        executor.setCorePoolSize(16);
        executor.setMaximumPoolSize(17);
        executor.setMaximumQueueSize(1000);
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        assertTrue(executor.start());
        for (int i=0; i<5; ++i)
            runTest(i);
    }
    
    private void runTest(int number) throws InterruptedException {
        System.out.println("\n\nRUNING TEST NUMBER: "+number);
        int messagesCount = 1000000;
        executedTasks = new CountDownLatch(messagesCount);
        long ts = System.nanoTime();
        for (int i=0; i<messagesCount; ++i)
            while (!executor.executeQuietly(new PerformanceTask2())) 
                Thread.sleep(1);
        long submitTs = System.nanoTime();
        executedTasks.await(20, TimeUnit.SECONDS);
        long executedCount = messagesCount-executedTasks.getCount();
        long finishTs = System.nanoTime();
        System.out.println("Executed tasks count: "+executedCount);
        System.out.println("Submit time ns: "+(submitTs-ts));
        System.out.println("Submit time ms: "+TimeUnit.NANOSECONDS.toMillis(submitTs-ts));
        System.out.println("Test time ns: "+(finishTs-ts));
        System.out.println("Test time ms: "+TimeUnit.NANOSECONDS.toMillis(finishTs-ts));
        double messagesPerMs = (double)executedCount/TimeUnit.NANOSECONDS.toMillis(finishTs-ts);
        System.out.println("Messages per ms: "+messagesPerMs);
        System.out.println();
    }
    
    @Test
    public void threadFactoryTest() throws InterruptedException {
        executor.setCorePoolSize(3);
        executor.setMaximumPoolSize(5);
        executor.setMaximumQueueSize(1);
        executor.setKeepAliveTime(100l);
        executor.setTimeUnit(TimeUnit.MILLISECONDS);
        assertTrue(executor.start());
        for (int i=0; i<5; ++i)
            executor.executeQuietly(new Task() {
                @Override
                public Node getTaskNode() {
                    return testsNode;
                }
                @Override
                public String getStatusMessage() {
                    return "test";
                }
                @Override
                public void run() {
                    try {
                        System.out.println(">>>Executing");
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ExecutorServiceNodeTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        System.out.println("RUNNING threads count: "+executor.threadFactory.getRunningThreadsCount());
        System.out.println("CURRENT POOL SIZE: "+executor.getCurrentPoolSize());
        Thread.sleep(1000);
        System.out.println("RUNNING threads count: "+executor.threadFactory.getRunningThreadsCount());
        System.out.println("CURRENT POOL SIZE: "+executor.getCurrentPoolSize());
        System.out.println("Test finished");
    }
    
    @Test
    public void test() throws ExecutorServiceException, InterruptedException, IOException, Exception
    {
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
        System.out.println("RUNNING threads count: "+executor.threadFactory.getRunningThreadsCount());

        Thread.sleep(100);
        assertEquals(new Integer(2+1), executor.getExecutingTaskCount()); //+1 is the delayed tasks executor
        List<ViewableObject> vos = executor.getViewableObjects(null);
        assertNotNull(vos);
        assertEquals(4, vos.size());
        Object data = vos.get(3).getData();
        assertTrue(data instanceof Table);
        List<Object[]> rows = RavenUtils.tableAsList((Table)data);
        assertNotNull(rows);
        System.out.println("\nTRACE: \n");
//        for (Object[] row: rows)
//            System.out.println(row[5]);
        assertEquals(2+1, rows.size()); //+1 is the delayed tasks executor
        assertEquals(executor.getPath(), rows.get(1)[1]);
        assertEquals("status message", rows.get(1)[2]);
        assertEquals(Thread.State.TIMED_WAITING.toString(), rows.get(1)[6]);
        assertTrue(rows.get(1)[7] instanceof ViewableObject);
        ViewableObject vo = (ViewableObject) rows.get(1)[7];
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vo.getMimeType());
        assertTrue(vo.getData() instanceof Table);
        Table trace = (Table)vo.getData();
        rows = RavenUtils.tableAsList(trace);
        assertTrue(rows.size()>0);
        

        Thread.sleep(1000);

        assertEquals(new Integer(0+1), executor.getExecutingTaskCount()); //+1 is the delayed tasks executor
        assertEquals(3, executor.getExecutedTasks().getOperationsCount());
        assertEquals(1l, executor.getRejectedTasks().get());
    }

    @Test
    public void test_nullMaximumQueueSize() throws Exception {
        executor.setCorePoolSize(2);
        executor.setMaximumPoolSize(3);
        executor.setMaximumQueueSize(null);
        executor.setLogLevel(LogLevel.DEBUG);
        assertTrue(executor.start());

        executor.execute(new TestTask(executor, 500));
        assertTrue(executor.executeQuietly(new TestTask(executor, 500)));
        try {
            executor.execute(new TestTask(executor, 0));
            fail();
        } catch (ExecutorServiceException ex) {
        }

        Thread.sleep(100);
        assertEquals(new Integer(2+1), executor.getExecutingTaskCount()); //+1 is the delayed tasks executor
        List<ViewableObject> vos = executor.getViewableObjects(null);

        Thread.sleep(1000);

        assertEquals(new Integer(0+1), executor.getExecutingTaskCount()); //+1 is the delayed tasks executor
        assertEquals(2, executor.getExecutedTasks().getOperationsCount());
        assertEquals(1l, executor.getRejectedTasks().get());
    }

    @Test
    public void delayedTasksTest() throws InterruptedException {
        executor.setCorePoolSize(2);
        executor.setMaximumPoolSize(2);
        executor.setMaximumQueueSize(1);
        executor.setLogLevel(LogLevel.DEBUG);
        assertTrue(executor.start());

        TestTask task1 = new TestTask(executor, 0);
        TestTask task2 = new TestTask(executor, 0);
        TestTask task3 = new TestTask(executor, 0);
        assertTrue(executor.executeQuietly(100, task2));
        assertTrue(executor.executeQuietly(50, task1));
        assertTrue(executor.executeQuietly(90, task3));
        Thread.sleep(150);
        assertTrue(task1.executed);
        assertTrue(task2.executed);
        assertTrue(task3.executed);
        assertTrue(task1.time < task3.time);
        assertTrue(task3.time < task2.time);
    }
    
    @Test
    public void cancelDelayedTaskTest() throws InterruptedException {
        executor.setCorePoolSize(2);
        executor.setMaximumPoolSize(2);
        executor.setMaximumQueueSize(1);
        executor.setLogLevel(LogLevel.DEBUG);
        assertTrue(executor.start());
        
        TestCancelableTask task1 = new TestCancelableTask(executor);
        TestCancelableTask task2 = new TestCancelableTask(executor);
        assertTrue(executor.executeQuietly(100, task1));
        assertTrue(executor.executeQuietly(100, task2));
        task1.cancel();
        Thread.currentThread().sleep(200l);
        assertTrue(task2.executed);
        assertFalse(task2.canceled);
        assertFalse(task1.executed);
        assertTrue(task1.canceled);
    }

    @Test
    public  void managedTaskReexecutePolicyTest() throws Exception {
        executor.setCheckManagedTasksInterval(500l);
        assertTrue(executor.start());

        ManagedTaskNode task = new ManagedTaskNode();
        task.setName("task");
        tree.getRootNode().addAndSaveChildren(task);
        task.setExecutor(executor);
        task.setTaskRestartPolicy(TaskRestartPolicy.REEXECUTE_TASK);
        assertTrue(task.start());

        Thread.sleep(600);
        assertEquals(1, task.getExecutionCount());
    }

    @Test
    public  void managedTaskRestartPolicyTest() throws Exception {
        executor.setCheckManagedTasksInterval(500l);
        assertTrue(executor.start());

        ManagedTaskNode task = new ManagedTaskNode();
        task.setName("task");
        tree.getRootNode().addAndSaveChildren(task);
        task.setExecutor(executor);
        task.setTaskRestartPolicy(TaskRestartPolicy.RESTART_NODE);
        assertTrue(task.start());

        Thread.sleep(600);
        assertEquals(0, task.getExecutionCount());
        assertEquals(2, task.getRestartCount());
    }

    @Test
    public  void singleManagedTaskExecutionTest() throws Exception {
        executor.setCheckManagedTasksInterval(500l);
        assertTrue(executor.start());

        ManagedTaskNode task = new ManagedTaskNode();
        task.setName("task");
        tree.getRootNode().addAndSaveChildren(task);
        task.setExecutor(executor);
        task.setTaskRestartPolicy(TaskRestartPolicy.REEXECUTE_TASK);
        task.setSleepInterval(1001);
        assertTrue(task.start());

        Thread.sleep(1400);
        assertEquals(1, task.getExecutionCount());

        Thread.sleep(200);
        assertEquals(2, task.getExecutionCount());
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
    
    private class TestCancelableTask implements CancelableTask {
        private final Node initiator;
        private volatile CancelationProcessor cancelationProcessor;
        private volatile boolean executed = false;
        private volatile boolean canceled = false;

        public TestCancelableTask(Node initiator) {
            this.initiator = initiator;
        }

        public void setCancelationProcessor(CancelationProcessor cancelationProcessor) {
            this.cancelationProcessor = cancelationProcessor;
        }

        public void cancel() {
            cancelationProcessor.cancel();
            canceled = true;
        }

        public Node getTaskNode() {
            return initiator;
        }

        public String getStatusMessage() {
            return "test task";
        }

        public void run() {
            executed = true;
        }
        
    }
    
    private class PerformanceTask2 extends AbstractExecutorTask {

        @Override
        public void doRun() throws Exception {
            executedTasks.countDown();
        }

        @Override
        public Node getTaskNode() {
            return testsNode;
        }

        @Override
        public String getStatusMessage() {
            return "Testing performance";
        }
        
    }
    
    private class PerformanceTask implements Task {

        public Node getTaskNode() {
            return testsNode;
        }

        public String getStatusMessage() {
            return "Testing performance";
        }

        public void run() {
            executedTasks.countDown();
        }
    }
    
}
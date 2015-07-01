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
import java.util.concurrent.TimeUnit;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.raven.sched.Executor;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class ExecutorServiceBalancerNodeTest extends RavenCoreTestCase {
    private ExecutorServiceBalancerNode balancer;
    private CountDownLatch executedTasks;
    
    @Before
    public void prepare() {
        balancer = new ExecutorServiceBalancerNode();
        balancer.setName("executor balancer");
        testsNode.addAndSaveChildren(balancer);
    }
    
    @Test(expected = ExecutorServiceException.class)
    public void executeOnStoppedWithoutFailover(
            @Mocked final Task task
    ) throws ExecutorServiceException 
    {
        try {
            balancer.execute(task);
        } finally {
            new Verifications() {{
                task.run(); times = 0;
            }};
        }
    }
    
    @Test(expected = ExecutorServiceException.class)
    public void executeOnStoppedWithFailover(
            @Mocked final Task task
    ) throws ExecutorServiceException 
    {
        try {
            balancer.setUseSystemExecutorAsFailover(true);
            balancer.execute(task);
        } finally {
            new Verifications() {{
                task.run(); times = 0;
            }};
        }
    }
    
//    @Test()
//    public void executeOnStoppedWithFailover(
//            @Mocked final Task task
//    ) throws ExecutorServiceException 
//    {
//        balancer.setUseSystemExecutorAsFailover(true);
//        balancer.execute(task);
//        
//        new Verifications() {{
//            task.run(); times = 1;
//        }};
//    }
//    
    @Test(expected = ExecutorServiceException.class)
    public void executeOnSartedWithoutFailover(
            @Mocked final Task task
    ) throws ExecutorServiceException 
    {
        assertTrue(balancer.start());
        try {
            balancer.execute(task);
        } finally {
            new Verifications() {{
                task.run(); times = 0;
            }};
        }
    }
    
    @Test()
    public void executeOnStartedWithFailover(
            @Mocked final Task task
    ) throws ExecutorServiceException 
    {
        balancer.setUseSystemExecutorAsFailover(true);
        assertTrue(balancer.start());
        balancer.execute(task);
        
        new Verifications() {{
            task.run(); times = 1;
        }};
    }
    
    @Test(expected=ExecutorServiceException.class)
    public void executeWithStoppedExecutor(
            @Mocked final Task task
    ) throws Exception {
        addExecutor("e1", null, false);
        assertNotNull(balancer.getNode("e1"));
        assertFalse(balancer.getNode("e1").isStarted());
        
        assertTrue(balancer.start());
        balancer.execute(task);
        
    }
    
    @Test()
    public void executeWithStartedExecutor(
            @Mocked final Task task,
            @Mocked final Executor executor
    ) throws Exception {
        
        addExecutor("e1", executor, true);
        
        assertTrue(balancer.start());
        balancer.execute(task);
        new Verifications() {{
            executor.execute(task); times = 1;
        }};
    }
    
    @Test()
    public void balancerTest(
            @Mocked final Task task,
            @Mocked final Executor executor,
            @Mocked final Executor executor1
    ) throws Exception {
        
        addExecutor("e1", executor, true);
        addExecutor("e2", executor1, true);
        
        assertTrue(balancer.start());
        balancer.execute(task);
        balancer.execute(task);
        balancer.execute(task);
        new VerificationsInOrder() {{
            executor.execute(task); times = 1;
            executor1.execute(task); times = 1;
            executor.execute(task); times = 1;
        }};
    }
    
    @Test()
    public void executeWithFailedExecutor(
            @Mocked final Task task,
            @Mocked final Executor executor,
            @Mocked final Executor executor1
    ) throws Exception {
        
        new Expectations() {{
            executor.execute(task); times = 1; result = new ExecutorServiceException("test");
        }};
        
        addExecutor("e1", executor, true);
        addExecutor("e2", executor1, true);
        
        assertTrue(balancer.start());
        balancer.execute(task);
        new VerificationsInOrder() {{
            executor.execute(task); times = 1;
            executor1.execute(task); times = 1;
        }};
    }
    
    @Test(expected = ExecutorServiceException.class)
    public void maxExecutionTries(
            @Mocked final Task task,
            @Mocked final Executor executor,
            @Mocked final Executor executor1
    ) throws Exception {
        
        new Expectations() {{
            executor.execute(task); times = 1; result = new ExecutorServiceException("test");
            executor1.execute(task); times = 1; result = new ExecutorServiceException("test");
        }};
        
        addExecutor("e1", executor, true);
        addExecutor("e2", executor1, true);
        
        assertTrue(balancer.start());
        try {
            balancer.execute(task);
        } finally {
            new VerificationsInOrder() {{
                executor.execute(task); times = 1;
                executor1.execute(task); times = 1;
            }};
        }
    }
    
    @Test(expected = ExecutorServiceException.class)
    public void stopDynamicExecutorTest(
            @Mocked final Task task,
            @Mocked final Executor executor
    ) throws Exception {
                
        TestExecutor e1 = addExecutor("e1", executor, true);
        
        assertTrue(balancer.start());
        try {
            balancer.execute(task);
            e1.stop();
            balancer.execute(task);
        } finally {
            new Verifications() {{
                executor.execute(task); times = 1;
            }};
        }
    }
    
    @Test()
    public void startDynamicExecutorTest(
            @Mocked final Task task,
            @Mocked final Executor executor,
            @Mocked final Executor executor1
    ) throws Exception {
                
        TestExecutor e1 = addExecutor("e1", executor, false);
        TestExecutor e2 = addExecutor("e2", executor1, true);
        assertTrue(balancer.start());
        balancer.execute(task);
        e1.start();
        e2.stop();
        balancer.execute(task);
        new VerificationsInOrder() {{
            executor1.execute(task); times = 1;
            executor.execute(task); times = 1;
        }};
    }
    
    @Test()
    public void addDynamicExecutorTest(
            @Mocked final Task task,
            @Mocked final Executor executor,
            @Mocked final Executor executor1
    ) throws Exception {
        TestExecutor e1 = addExecutor("e1", executor, true);
        assertTrue(balancer.start());
        balancer.execute(task);
        addExecutor("e2", executor1, true);
        balancer.execute(task);
        new VerificationsInOrder() {{
            executor.execute(task); times = 1;
            executor1.execute(task); times = 1;
        }};
    }
    
    @Test()
    public void moveExecutorToBalancerTest(
            @Mocked final Task task,
            @Mocked final Executor executor,
            @Mocked final Executor executor1
    ) throws Exception {
        TestExecutor e1 = addExecutor("e1", executor, true);
        TestExecutor e2 = addExecutor(testsNode, "e2", executor1, true);
        assertTrue(balancer.start());
        balancer.execute(task);
        tree.move(e2, balancer, null);
        balancer.execute(task);
        new VerificationsInOrder() {{
            executor.execute(task); times = 1;
            executor1.execute(task); times = 1;
        }};
    }
    
    @Test(expected=ExecutorServiceException.class)
    public void moveExecutorFromBalancerTest(
            @Mocked final Task task,
            @Mocked final Executor executor
    ) throws Exception {
        TestExecutor e1 = addExecutor("e1", executor, true);
        assertTrue(balancer.start());
        try {
            balancer.execute(task);
            tree.move(e1, testsNode, null);
            balancer.execute(task);
        } finally {
            new VerificationsInOrder() {{
                executor.execute(task); times = 1;
            }};
        }
    }
    
    @Test
    public void migrateDelayedTasksFromStoppedExecutor(
            @Mocked final Task task,
            @Mocked final Executor executor1
    ) throws Exception 
    {
        //creating normal executor
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        balancer.addAndSaveChildren(executor);
        executor.setCorePoolSize(16);
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        assertTrue(executor.start());
        assertTrue(balancer.start());
        
        addExecutor("executor1", executor1, true);
        balancer.execute(60000, task);
        new Verifications(){{
            executor1.execute(anyLong, (Task)any); times=0;
        }};
        executor.stop();
        new Verifications(){{
            long d;
            executor1.execute(d = withCapture(), task);
            assertTrue(d>59000);
        }};
        
    }
    
    @Test
    public void performanceTest() throws InterruptedException {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        balancer.addAndSaveChildren(executor);
        executor.setCorePoolSize(16);
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        assertTrue(executor.start());
        assertTrue(balancer.start());
        for (int i=0; i<5; ++i)
            runTest(i);
    }
    
    private void runTest(int number) throws InterruptedException {
        System.out.println("\n\nRUNING TEST NUMBER: "+number);
        int messagesCount = 1000000;
        executedTasks = new CountDownLatch(messagesCount);
        long ts = System.nanoTime();
        for (int i=0; i<messagesCount; ++i)
            while (!balancer.executeQuietly(new PerformanceTask2())) 
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
    
    
    private TestExecutor addExecutor(String name, Executor stub, boolean start) {
        return addExecutor(balancer, name, stub, start);
    }
    
    private TestExecutor addExecutor(Node owner, String name, Executor stub, boolean start) {
        TestExecutor executor = new TestExecutor();
        executor.setName(name);
        owner.addAndSaveChildren(executor);
        executor.setExecutor(stub);
        if (start)
            assertTrue(executor.start());
        return executor;
    }
    
    private ExecutorServiceBalancerNode createBalancer() {
        balancer = new ExecutorServiceBalancerNode();
        balancer.setName("executor balancer2");
        testsNode.addAndSaveChildren(balancer);
        return balancer;
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
    
    
}

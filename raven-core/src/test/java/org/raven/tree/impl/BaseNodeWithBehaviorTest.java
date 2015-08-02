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

package org.raven.tree.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.DataProcessorFacadeConfig;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.NodeWithBehavior;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class BaseNodeWithBehaviorTest extends RavenCoreTestCase {
    private ExecutorServiceNode executor;
    private TestNodeWithBehavior node;
    private LoggerHelper logger;
    
    @Before
    public void prepare() {
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        executor.setCorePoolSize(8);
        assertTrue(executor.start());
                
        node = new TestNodeWithBehavior();
        node.setName("behavior node");
        testsNode.addAndSaveChildren(node);
        node.setExecutor(executor);
        assertTrue(node.start());
        
        logger = new LoggerHelper(testsNode, null);
    }
    
    @Test
    public void behaviorUnavailableTest(
            final @Mocked DataProcessor dp
    ) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        new Expectations() {{
            dp.processData(BaseNodeWithBehavior.BEHAVIOR_UNAVAILABLE); result = new Delegate() {
                public Object processData(Object mess) {
                    latch.countDown();
                    return DataProcessor.VOID;
                }
            };
        }};
        DataProcessorFacade facade = new DataProcessorFacadeConfig("dp", node, dp, executor, logger).build();
        node.requestBehavior(facade);
        latch.await(1, TimeUnit.SECONDS);
    }
    
    @Test
    public void behaviorTest(
            final @Mocked DataProcessorFacade behaviour,
            final @Mocked DataProcessor dp
    ) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        new Expectations() {{
            dp.processData(withArgThat(new BaseMatcher<NodeWithBehavior.Behavior>() {
                @Override public boolean matches(Object item) {
                    return (item instanceof NodeWithBehavior.Behavior && ((NodeWithBehavior.Behavior)item).getBehavior()==behaviour);
                }
                @Override public void describeTo(Description description) {
                }
            })); 
            result = new Delegate() {
                public Object processData(NodeWithBehavior.Behavior mess) {
                    latch.countDown();
                    return DataProcessor.VOID;
                }
            };
        }};
        DataProcessorFacade facade = new DataProcessorFacadeConfig("dp", node, dp, executor, logger).build();
        node.setBehavior(behaviour);
        node.requestBehavior(facade);
        latch.await(1, TimeUnit.SECONDS);
    }    
}

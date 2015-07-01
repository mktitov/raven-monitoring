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

import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.raven.sched.Task;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class InternalExecutorTest {
    private InternalExecutor executor;
    
    @Before
    public void prepare() {
        executor = new InternalExecutor("Internal executor", 4, LoggerFactory.getLogger(InternalExecutor.class));
    }
    
    @Test
    public void successExecute(
            @Mocked final Task task
    ) throws Exception
    {
        executor.execute(task);
        Thread.sleep(100);
        new Verifications() {{
            task.run();
        }};
    }
    
    @Test
    public void successExecuteQuietly(
            @Mocked final Task task
    ) throws Exception
    {
        executor.executeQuietly(task);
        Thread.sleep(100);
        new Verifications() {{
            task.run();
        }};
    }
    
    @Test
    public void successExecuteDelayed(
            @Mocked final Task task
    ) throws Exception
    {
        executor.execute(100, task);
        checkDelayedExecution(task);
    }

    @Test
    public void successExecuteDelayedQuietly(
            @Mocked final Task task
    ) throws Exception
    {
        executor.executeQuietly(100, task);
        checkDelayedExecution(task);
    }

    private void checkDelayedExecution(final Task task) throws InterruptedException {
        Thread.sleep(90);
        new Verifications() {{
            task.run(); times=0;
        }};
        Thread.sleep(110);
        new Verifications() {{
            task.run(); times=1;
        }};
    }
}

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
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.raven.sched.SystemExecutorService;
import org.raven.sched.Task;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class SystemExecutorServiceImplTest extends RavenCoreTestCase {
    
    @Test
    public void test(
            @Mocked final Task task
    )throws Exception
    {
        SystemExecutorService executorService = registry.getService(SystemExecutorService.class);
        assertNotNull(executorService);
        assertNotNull(executorService.getExecutor());
        executorService.getExecutor().executeQuietly(task);
        Thread.sleep(100);
        new Verifications(){{
            task.run(); times=1;
        }};
    }
    
}

/*
 * Copyright 2016 Mikhail Titov.
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
package org.raven.net.http.server.impl;

import mockit.Delegate;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import org.raven.net.http.server.HttpSession;
import org.raven.sched.Executor;
import org.raven.sched.Task;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class SessionManagerImplTest extends Assert {
    
    @Test public void crudTest(
            @Mocked final Executor executor,
            @Mocked final Node owner
    ) {
        SessionManagerImpl manager = new SessionManagerImpl(executor, owner, 5000);
        HttpSession session = manager.createSession();        
        assertNotNull(session);
        HttpSession sameSession = manager.getSession(session.getId());
        assertSame(session, sameSession);
        manager.invalidateSession(session.getId());
        assertNull(manager.getSession(session.getId()));        
    }
    
    @Test public void invalidateSession (
            @Mocked final Executor executor,
            @Mocked final Node owner
    ) {
        SessionManagerImpl manager = new SessionManagerImpl(executor, owner, 5000);
        HttpSession session = manager.createSession();        
        assertSame(session, manager.getSession(session.getId()));
        session.invalidate();
        assertNull(manager.getSession(session.getId()));
    }
    
    @Test public void sessionTimeout(
            @Mocked final Executor executor,
            @Mocked final Node owner
    ) throws Exception {
        new StrictExpectations() {{
            executor.executeQuietly(1000l, (Task) any); result = new Delegate() {
                public boolean executeQuietly(long delay, Task task) {
                    task.run();
                    return true;
                }
            };
            executor.executeQuietly(1000l, (Task) any); result = new Delegate() {
                public boolean executeQuietly(long delay, Task task) {
                    return true;
                }
            };
        }};
        SessionManagerImpl manager = new SessionManagerImpl(executor, owner, 500);
        HttpSession session = manager.createSession();
        assertNotNull(manager.getSession(session.getId()));
        Thread.sleep(600);
        manager.start();
        assertNull(manager.getSession(session.getId()));
//        Thread.sleep(millis);
    }
}

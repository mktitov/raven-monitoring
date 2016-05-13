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
import mockit.Expectations;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.raven.audit.Action;
import org.raven.audit.AuditRecord;
import org.raven.audit.Auditor;
import org.raven.auth.UserContext;
import org.raven.auth.UserContextService;
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
            @Mocked final Node owner,
            @Mocked final Auditor auditor
    ) {
        SessionManagerImpl manager = new SessionManagerImpl(executor, owner, 5000, auditor);
        HttpSession session = manager.createSession();        
        assertNotNull(session);
        HttpSession sameSession = manager.getSession(session.getId());
        assertSame(session, sameSession);
        manager.invalidateSession(session.getId());
        assertNull(manager.getSession(session.getId()));        
    }
    
    @Test public void invalidateSession (
            @Mocked final Executor executor,
            @Mocked final Node owner,
            @Mocked final Auditor auditor
    ) {
        SessionManagerImpl manager = new SessionManagerImpl(executor, owner, 5000, auditor);
        HttpSession session = manager.createSession();        
        assertSame(session, manager.getSession(session.getId()));
        session.invalidate();
        assertNull(manager.getSession(session.getId()));
    }
    
    @Test public void sessionTimeout(
            @Mocked final Executor executor,
            @Mocked final Node owner,
            @Mocked final Auditor auditor
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
        SessionManagerImpl manager = new SessionManagerImpl(executor, owner, 500, auditor);
        HttpSession session = manager.createSession();
        assertNotNull(manager.getSession(session.getId()));
        Thread.sleep(600);
        manager.start();
        assertNull(manager.getSession(session.getId()));
    }
    
    @Test public void sessionCloseAuditTest(
            @Mocked final Executor executor,
            @Mocked final Node owner,
            @Mocked final Auditor auditor,
            @Mocked final UserContext userContext,
            @Mocked final Node serviceNode
    ) {
        new Expectations(){{
            serviceNode.getPath(); result = "/service"; 
            serviceNode.getId(); result = 1;
            userContext.getLogin(); result = "testUser";
            userContext.getHost(); result = "1.1.1.1";
        }};
        SessionManagerImpl manager = new SessionManagerImpl(executor, owner, 5000, auditor);
        HttpSession session = manager.createSession();        
        session.setUserContext(userContext);
        session.setAttribute(UserContextService.SERVICE_NODE_SESSION_ATTR, serviceNode);
        session.invalidate();
        
        new Verifications() {{
            AuditRecord rec;
            auditor.write(rec=withCapture());
            assertNotNull(rec);
            assertEquals(new Integer(1), rec.getNodeId());
            assertEquals("/service", rec.getNodePath());
            assertEquals("testUser", rec.getLogin());
            assertEquals("1.1.1.1", rec.getRemoteIp());
            assertNull(rec.getMessage());
            assertEquals(Action.SESSION_STOP, rec.getAction());
        }};
    }
}

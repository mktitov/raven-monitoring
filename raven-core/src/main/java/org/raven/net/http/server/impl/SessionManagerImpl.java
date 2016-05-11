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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.raven.net.http.server.HttpSession;
import org.raven.net.http.server.SessionManager;
import org.raven.sched.Executor;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class SessionManagerImpl implements SessionManager {
    public final static long SESSIONS_CHECK_INTERVAL = 1000L;
    private final Executor executor;
    private final Node owner;
    private final long sessionTimeout; //in ms
    
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private volatile CheckSessionsTask checkSessionsTask;

    public SessionManagerImpl(Executor executor, Node owner, long sessionTimeout) {
        this.executor = executor;
        this.owner = owner;
        this.sessionTimeout = sessionTimeout;
    }

    @Override
    public void start() {
        stopped.set(false);
        checkSessionsTask = new CheckSessionsTask();
        executeCheckSessionsTask();
    }

    @Override
    public void stop() {
        if (stopped.compareAndSet(false, true)) {
            checkSessionsTask.cancel();
            checkSessionsTask = null;
            sessions.clear();
        }
    }

    @Override
    public HttpSession getSession(String sessionId) {
        final Session session = sessions.get(sessionId);
        if (session!=null)
            session.updateLastAccessTime();
        return session;
    }

    @Override
    public HttpSession createSession() {
        Session prevSession, session;
        do {
            session = new Session(UUID.randomUUID().toString());
            prevSession = sessions.putIfAbsent(session.getId(), session);
        } while(prevSession!=null);
        return session;
    }

    @Override
    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }

    @Override
    public Collection<? extends HttpSession> getSessions() {
        return sessions.isEmpty()? Collections.EMPTY_LIST : new ArrayList<>(sessions.values());
    }
    
    private void executeCheckSessionsTask() {
        executor.executeQuietly(SESSIONS_CHECK_INTERVAL, checkSessionsTask);        
    }
    
    private void checkSessions() {
        final long curTime = System.currentTimeMillis();
        for (Iterator<Session> it = sessions.values().iterator(); it.hasNext();)
            if (it.next().lastAccessTime+sessionTimeout < curTime)
                it.remove();
    }
    
    private class CheckSessionsTask extends AbstractTask {

        public CheckSessionsTask() {
            super(owner, "Http sessions invalidate task");
        }

        @Override
        public void doRun() throws Exception {
            if (!stopped.get() && this==checkSessionsTask) {
                checkSessions();
                executeCheckSessionsTask();
            }
        }        
    }
    
    private class Session extends AbstractHttpSession {
        private volatile long lastAccessTime;

        public Session(String id) {
            super(id);
        }

        @Override
        public long getLastAccessTime() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void invalidate() {
            invalidateSession(getId());
        }        
        
        private void updateLastAccessTime() {
            lastAccessTime = System.currentTimeMillis();
        }
    }    
}

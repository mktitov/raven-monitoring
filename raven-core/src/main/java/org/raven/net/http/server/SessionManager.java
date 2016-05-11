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
package org.raven.net.http.server;

import java.util.Collection;

/**
 *
 * @author Mikhail Titov
 */
public interface SessionManager {
    public void start();
    public void stop();
    public HttpSession getSession(String sessionId);
    public HttpSession createSession();
    public void invalidateSession(String sessionId);
    public Collection<? extends HttpSession> getSessions();
}

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

import java.util.Map;
import org.raven.auth.UserContext;
import org.raven.net.http.server.HttpSession;

/**
 *
 * @author Mikhail Titov
 */
public class HttpSessionImpl implements HttpSession {
    private final String id;
    private final long creationTime;
    private volatile long lastAccessTime;
    private volatile UserContext userContext;
    private volatile Map<String, Object> attributes;

    public HttpSessionImpl(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
    }

    @Override
    public long getCreationTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastAccessTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAttribute(String name, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getAttribute(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UserContext getUserContext() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setUserContext(UserContext userContext) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}

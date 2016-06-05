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
import java.util.concurrent.ConcurrentHashMap;
import org.raven.auth.UserContext;
import org.raven.net.http.server.HttpSession;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractHttpSession implements HttpSession {
    private final String id;
    private final long creationTime;
    private volatile UserContext userContext;
    private volatile Map<String, Object> attributes;

    public AbstractHttpSession(String id) {
        this.id = id;
        this.creationTime = System.currentTimeMillis();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value==null)
            return;
        getOrCreateAttributes().put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return attributes==null? null : getOrCreateAttributes().get(name);
    }

    @Override
    public Map<String, Object> getAttrs() {
        return getOrCreateAttributes();
    }

    @Override
    public UserContext getUserContext() {
        return userContext;
    }

    @Override
    public void setUserContext(UserContext userContext) {
        this.userContext = userContext;
    }
    
    private Map<String, Object> getOrCreateAttributes() {
        if (attributes==null) {
            synchronized(this) {
                if (attributes==null)
                    attributes = new ConcurrentHashMap<>();
            }
        }
        return attributes;
    }
}

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

import org.raven.auth.UserContext;

/**
 *
 * @author Mikhail Titov
 */
public interface HttpSession {
    /**
     * Returns the session id
     */
    public String getId();
    /**
     * Return the session creation time
     */
    public long getCreationTime();
    /**
     * Returns the last access time
     */
    public long getLastAccessTime();
    /**
     * Sets value for the specified attribute
     * @param name the attribute name
     * @param value the attribute value
     */
    public void setAttribute(String name, Object value);
    /**
     * Returns the value of the specified attribute
     * @param name the name of the attribute
     */
    public Object getAttribute(String name);
    /**
     * Returns the context of the user attached to the session
     */
    public UserContext getUserContext();
    /**
     * Attach user context to the session
     */
    public void setUserContext(UserContext userContext);
}

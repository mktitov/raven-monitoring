/*
 *  Copyright 2010 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.server.app.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Mikhail Titov
 */
public interface RavenServletContext
{
    /**
     * Attaches the request and response to the current thread
     */
    public void setContext(HttpServletRequest request, HttpServletResponse response);
    /**
     * Detaches the request and response from current thread
     */
    public void removeContext();
    /**
     * Returns request for current thread or null if request does not attached to this thread
     */
    public HttpServletRequest getRequest();
    /**
     * Returns request for current thread or null if request does not attached to this thread
     */
    public HttpServletResponse getResponse();
    /**
     * Returns the session for this request or null if session not created.
     */
    public HttpSession getSession();
}

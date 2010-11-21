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

package org.raven.server.app.service.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.raven.server.app.service.RavenServletContext;

/**
 *
 * @author Mikhail Titov
 */
public class RavenServletContextImpl implements RavenServletContext
{
    private final ThreadLocal<Context> contexts = new ThreadLocal<Context>();

    public void setContext(HttpServletRequest request, HttpServletResponse response)
    {
        contexts.set(new Context(request, response));
    }

    public void removeContext()
    {
        contexts.remove();
    }

    public HttpServletRequest getRequest() 
    {
        Context context = contexts.get();
        return context==null? null : context.request;
    }

    public HttpServletResponse getResponse()
    {
        Context context = contexts.get();
        return context==null? null : context.response;
    }

    public HttpSession getSession()
    {
        HttpServletRequest request = getRequest();
        return request==null? null : request.getSession(false);
    }

    private static class Context
    {
        private final HttpServletRequest request;
        private final HttpServletResponse response;

        public Context(HttpServletRequest request, HttpServletResponse response)
        {
            this.request = request;
            this.response = response;
        }
    }
}

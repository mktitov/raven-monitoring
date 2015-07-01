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

import javax.servlet.http.HttpSession;
import org.raven.auth.UserContext;
import org.raven.auth.UserContextService;
import org.raven.server.app.App;
import org.raven.server.app.service.RavenServletContext;

/**
 *
 * @author Mikhail Titov
 */
public class UserContextServiceImpl implements UserContextService
{
    private final RavenServletContext servletContext;

    public UserContextServiceImpl(RavenServletContext servletContext)
    {
        this.servletContext = servletContext;
    }
    
    public UserContext getUserContext()
    {
        HttpSession session = servletContext.getSession();
        return session==null? null : (UserContext)session.getAttribute(App.USER_CONTEXT_SESSION_ATTR);
    }
    
}

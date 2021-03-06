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

package org.raven.ui.services;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import org.raven.auth.UserContext;
import org.raven.auth.UserContextService;
import org.raven.ui.filter.LoginFilter;

/**
 *
 * @author Mikhail Titov
 */
public class UserContextServiceImpl implements UserContextService
{
    public UserContext getUserContext() 
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext==null)
            return null;
        HttpSession session = (HttpSession)facesContext.getExternalContext().getSession(false);
        return (UserContext) (session == null ? null : session.getAttribute(LoginFilter.USER_CONTEXT_ATTR));
    }
}

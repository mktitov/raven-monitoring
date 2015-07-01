/*
 *  Copyright 2009 Mikhail Titov.
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
import org.weda.internal.Cache;
import org.weda.internal.CacheEntity;

/**
 *
 * @author Mikhail Titov
 */
public class SessionCache implements Cache
{
    @SuppressWarnings("unchecked")
	public <T> CacheEntity<T> get(String id)
    {
        HttpSession session = getSession(false);
        if (session==null)
            return null;
        else
            return (CacheEntity<T>) session.getAttribute(id);
    }

    @SuppressWarnings("unchecked")
	public void put(String id, CacheEntity entity)
    {
        getSession(true).setAttribute(id, entity);
    }

    public void remove(String id)
    {
        HttpSession session = getSession(false);
        if (session!=null)
            session.removeAttribute(id);
    }

    public void removeAll()
    {
    }

    private HttpSession getSession(boolean create)
    {
        return
            (HttpSession)FacesContext.getCurrentInstance().getExternalContext().getSession(create);
    }
}

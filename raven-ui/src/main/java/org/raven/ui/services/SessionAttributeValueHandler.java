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
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.AbstractAttributeValueHandler;

/**
 *
 * @author Mikhail Titov
 */
public class SessionAttributeValueHandler extends AbstractAttributeValueHandler
{
    private String sessionAttributeName;

    public SessionAttributeValueHandler(NodeAttribute attribute)
    {
        super(attribute);
    }

    public void setData(String value) throws Exception
    {
        sessionAttributeName = value;
    }

    public String getData()
    {
        return sessionAttributeName;
    }

    public Object handleData()
    {
        HttpSession session =
                (HttpSession)FacesContext.getCurrentInstance().getExternalContext()
                .getSession(false);
        if (session==null)
            return null;

        return session.getAttribute(sessionAttributeName);
    }

    public void close()
    {
    }

    public boolean isReferenceValuesSupported() 
    {
        return true;
    }

    public boolean isExpressionSupported() 
    {
        return true;
    }

    public boolean isExpressionValid()
    {
        return true;
    }

    public void validateExpression() throws Exception
    {
    }
}

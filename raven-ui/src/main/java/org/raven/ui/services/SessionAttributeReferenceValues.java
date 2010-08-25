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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.NodeAttribute;
import org.weda.constraints.ReferenceValueCollection;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueImpl;

/**
 *
 * @author Mikhail Titov
 */
public class SessionAttributeReferenceValues implements AttributeReferenceValues
{
    @SuppressWarnings("unchecked")
	public boolean getReferenceValues(NodeAttribute attr, ReferenceValueCollection referenceValues)
            throws TooManyReferenceValuesException 
    {
        if (!SessionAttributeValueHandlerFactory.TYPE.equals(attr.getValueHandlerType()))
            return false;

        HttpSession session =
                (HttpSession)FacesContext.getCurrentInstance().getExternalContext()
                .getSession(false);
        if (session!=null)
        {
            Enumeration<String> attrNames = session.getAttributeNames();
            if (attrNames!=null)
            {
                List<String> sortedNames = new ArrayList<String>();
                while (attrNames.hasMoreElements())
                    sortedNames.add(attrNames.nextElement());
                if (!sortedNames.isEmpty())
                {
                    Collections.sort(sortedNames);
                    for (String attrName: sortedNames)
                        referenceValues.add(new ReferenceValueImpl(attrName, attrName), null);
                }
            }
        }
        return true;
    }
}

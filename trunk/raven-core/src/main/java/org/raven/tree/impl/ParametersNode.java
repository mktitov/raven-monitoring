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

package org.raven.tree.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class ParametersNode extends BaseNode
{
    public final static String NAME="Parameters";

    public ParametersNode()
    {
        super(NAME);
    }

    /**
     * Returns the parameters values or <code>null</code> if parameters node does not have
     * parameters.
     * @return
     */
    public Map<String, Object> getParameterValues()
    {
        Collection<Node> childs = getChildrens();
        if (childs==null || childs.isEmpty())
            return null;
        else
        {
            Map<String, Object> parameterValues = null;
            for (Node child: childs)
                if (child.getStatus()==Status.STARTED && child instanceof ParameterNode)
                {
                    if (parameterValues==null)
                        parameterValues = new HashMap<String, Object>();
                    parameterValues.put(child.getName(), ((ParameterNode)child).getValue());
                }
            return parameterValues;
        }
    }
}

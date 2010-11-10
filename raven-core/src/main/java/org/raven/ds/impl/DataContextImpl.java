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

package org.raven.ds.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.raven.auth.UserContext;
import org.raven.auth.UserContextService;
import org.raven.ds.DataContext;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class DataContextImpl implements DataContext
{
    @Service
    private static UserContextService userContextService;

    private final UserContext userContext;
    private final Map parameters;
    private final Map<String, List<String>> errors;
    private final Map<String, NodeAttribute> sessionAttributes;

    public DataContextImpl()
    {
        this.userContext = userContextService.getUserContext();
        this.parameters = new ConcurrentHashMap();
        this.errors = new LinkedHashMap<String, List<String>>();
        this.sessionAttributes = new HashMap<String, NodeAttribute>();
    }

    public DataContextImpl(Collection<NodeAttribute> sessionAttributes)
    {
        this();
        addSessionAttributes(sessionAttributes);
    }

    public DataContextImpl(Map<String, NodeAttribute> sessionAttributes)
    {
        this();
        if (sessionAttributes!=null && !sessionAttributes.isEmpty())
            addSessionAttributes(sessionAttributes.values());
    }

    public Map getParameters()
    {
        return parameters;
    }

    public Object getAt(String parameterName) 
    {
        return parameters.get(parameterName);
    }

    public void putAt(String parameterName, Object value)
    {
        parameters.put(parameterName, value);
    }

    public void putNodeParameter(Node node, String parameterName, Object value)
    {
        parameters.put(getNodeParameterId(node, parameterName), value);
    }

    public Object getNodeParameter(Node node, String parameterName)
    {
        return parameters.get(getNodeParameterId(node, parameterName));
    }

    public Object removeNodeParameter(Node node, String parameterName)
    {
        return parameters.remove(getNodeParameterId(node, parameterName));
    }

    private String getNodeParameterId(Node node, String parameterName)
    {
        return ""+node.getId()+"_"+parameterName;
    }

    public void addError(String path, String error)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<String> getErrors()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Map<String, NodeAttribute> getSessionAttributes() 
    {
        return sessionAttributes;
    }

    public void addSessionAttribute(NodeAttribute attr) 
    {
        sessionAttributes.put(attr.getName(), attr);
    }

    public final void addSessionAttributes(Collection<NodeAttribute> attrs)
    {
        addSessionAttributes(attrs, true);
    }

    public void addSessionAttributes(Collection<NodeAttribute> attrs, boolean replace)
    {
        if (attrs!=null && !attrs.isEmpty())
            for (NodeAttribute attr: attrs)
                if ( replace || !sessionAttributes.containsKey(attr.getName()) )
                    this.sessionAttributes.put(attr.getName(), attr);
    }

    public UserContext getUserContext() 
    {
        return userContext;
    }
}

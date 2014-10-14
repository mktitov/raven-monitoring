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

import groovy.lang.Closure;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.raven.auth.UserContext;
import org.raven.auth.UserContextService;
import org.raven.ds.DataContext;
import org.raven.ds.DataError;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class DataContextImpl implements DataContext
{
    public static final int MAX_ERROR_STACK_SIZE = 50;
    
    @Service
    private static UserContextService userContextService;

    private final UserContext userContext;
    private final Map parameters;
    private final Queue<DataError> errors;
    private final Map<String, NodeAttribute> sessionAttributes;
    private LinkedList<Closure> callbacks;
    private LinkedList<Closure> eachDataSendCallbacks;

    public DataContextImpl()
    {
        this.userContext = userContextService.getUserContext();
        this.parameters = new ConcurrentHashMap();
        this.errors = new LinkedBlockingQueue<DataError>(MAX_ERROR_STACK_SIZE);
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
        if (value==null)
            parameters.remove(parameterName);
        else
            parameters.put(parameterName, value);
    }
    
    public void putNodeParameter(Node node, String parameterName, Object value)
    {
        if (value==null)
            removeNodeParameter(node, parameterName);
        else
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

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void addError(Node node, Throwable error) {
        errors.offer(new DataErrorImpl(node, error));
    }

    public void addError(Node node, String errorMessage) {
        addError(node, new Exception(errorMessage));
    }

    public Queue<DataError> getErrors() {
        return errors;
    }
    
    public DataError getFirstError() {
        return errors.peek();
    }

    public Map<String, NodeAttribute> getSessionAttributes() {
        return sessionAttributes;
    }

    public void addSessionAttribute(NodeAttribute attr) {
        sessionAttributes.put(attr.getName(), attr);
    }

    public NodeAttribute addSessionAttribute(Node owner, String name, String value) throws Exception {
        NodeAttributeImpl attr = new NodeAttributeImpl(name, String.class, value, null);
        attr.setOwner(owner);
        attr.setId(-1);
        attr.init();
        addSessionAttribute(attr);
        return attr;
    }

    public final void addSessionAttributes(Collection<NodeAttribute> attrs) {
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

    public synchronized DataContext addCallback(Closure callback) {
        return addCallbackOnEnd(callback);
    }

    public synchronized DataContext addCallbackOnEnd(Closure callback) {
        if (callbacks==null) 
            callbacks = new LinkedList<Closure>();
        callbacks.addFirst(callback);
        return this;
    }

    public synchronized DataContext addCallbackOnEach(Closure callback) {
        if (eachDataSendCallbacks==null) 
            eachDataSendCallbacks = new LinkedList<Closure>();
        eachDataSendCallbacks.addFirst(callback);
        return this;
    }

//    public void executeCallbacks(Node initiator) {
//        _executeCallbacks(initiator, callbacks);
//    }

    public void executeCallbacksOnEnd(Node initiator, Object data) {
        _executeCallbacks(initiator, data, callbacks);
    }
    
    public void executeCallbacksOnEach(Node initiator, Object data) {
        _executeCallbacks(initiator, data, eachDataSendCallbacks);
    }
    
    private void _executeCallbacks(Node initiator, Object data, Collection<Closure> callbacks) {
        Collection<Closure> _callbacks;
        synchronized (this) {
            if (callbacks==null || callbacks.isEmpty())
                return;
            _callbacks = new ArrayList(callbacks);
        }
        for (Closure closure: _callbacks) {
            try {
                if (closure.getMaximumNumberOfParameters()==0)
                    closure.call();
                else if (closure.getMaximumNumberOfParameters()==1)
                    closure.call(initiator);
                else if (closure.getMaximumNumberOfParameters()==2)
                    closure.call(initiator, data);
                else throw new Exception("Invalid number of parameters in closure. Expected 0 or 1 but was - "+closure.getMaximumNumberOfParameters());
            } catch (Throwable e) {
                if (initiator.isLogLevelEnabled(LogLevel.ERROR))
                    initiator.getLogger().error("Data send callback execution error ", e);
            }
        }
    }

}

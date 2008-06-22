/*
 *  Copyright 2008 Mikhail Titov.
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

import java.util.ArrayList;
import java.util.List;
import org.raven.tree.AttributeValueHandler;
import org.raven.tree.AttributeValueHandlerListener;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractAttributeValueHandler implements AttributeValueHandler
{
    protected final NodeAttribute attribute;
    private List<AttributeValueHandlerListener> listeners;

    public AbstractAttributeValueHandler(NodeAttribute attribute)
    {
        this.attribute = attribute;
    }

    public void addListener(AttributeValueHandlerListener listener)
    {
        if (listeners==null)
            listeners = new ArrayList<AttributeValueHandlerListener>(2);
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(AttributeValueHandlerListener listener)
    {
        listeners.remove(listener);
    }
    
    protected void fireValueChangedEvent(Object oldValue, Object newValue)
    {
        if (listeners!=null)
            for (AttributeValueHandlerListener listener: listeners)
                listener.valueChanged(oldValue, newValue);
    }
    
    protected void fireExpressionInvalidatedEvent(Object oldValue)
    {
        if (listeners!=null)
            for (AttributeValueHandlerListener listener: listeners)
                listener.expressionInvalidated(oldValue);
    }
}

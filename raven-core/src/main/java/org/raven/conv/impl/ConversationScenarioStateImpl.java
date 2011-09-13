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

package org.raven.conv.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioCycleDetectedException;
import org.raven.conv.ConversationScenarioPoint;
import org.raven.conv.ConversationScenarioState;

/**
 *
 * @author Mikhail Titov
 */
public class ConversationScenarioStateImpl implements ConversationScenarioState
{
    private final Bindings bindings = new SimpleBindings();
    private final Map<String, BindingScope> scopes = new HashMap<String, BindingScope>();
    private final Map<String, Object> defaultValues = new HashMap<String, Object>();
    private ConversationScenarioPoint nextConversationPoint;
    private boolean immediateTransition = false;
    private AtomicBoolean dtmfProcessingFlag = new AtomicBoolean(true);

    public Bindings getBindings()
    {
        return bindings;
    }

    public void setBinding(String name, Object value, BindingScope scope)
    {
        bindings.put(name, value);
        scopes.put(name, scope);
    }

    public void setBindingDefaultValue(String name, Object defaultValue)
    {
        defaultValues.put(name, defaultValue);
    }

    public void setImmediateTransition(boolean immediateTransition)
    {
        this.immediateTransition = immediateTransition;
    }

    private void resetBindings(BindingScope scope)
    {
        for (Iterator<Map.Entry<String, Object>> it = bindings.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry<String, Object> binding = it.next();
            BindingScope bindingScope = scopes.get(binding.getKey());
            if (   scope.equals(bindingScope)
                || (scope==BindingScope.CONVERSATION && bindingScope==null))
            {
                Object defaultValue = defaultValues.get(binding.getKey());
                if (defaultValue==null)
                    it.remove();
                else
                    binding.setValue(defaultValue);
            }
        }
    }

    public boolean hasImmediateTransition()
    {
        return immediateTransition;
    }

    public ConversationScenarioPoint getNextConversationPoint()
    {
        return nextConversationPoint;
    }

    public void setNextConversationPoint(ConversationScenarioPoint nextConversationPoint)
            throws ConversationScenarioCycleDetectedException
    {
        if (this.nextConversationPoint!=nextConversationPoint)
        {
            this.nextConversationPoint = nextConversationPoint;
            resetBindings(BindingScope.POINT);
        }
    }

    public void resetRequestBindings()
    {
        resetBindings(BindingScope.REQUEST);
    }

    public void enableDtmfProcessing() {
        dtmfProcessingFlag.compareAndSet(false, true);
    }

    public void disableDtmfProcessing() {
        dtmfProcessingFlag.compareAndSet(true, false);
    }

    public boolean isDtmfProcessingDisabled() {
        return !dtmfProcessingFlag.get();
    }
}

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

package org.raven.conv;

import javax.script.Bindings;

/**
 *
 * @author Mikhail Titov
 */
public interface ConversationScenarioState
{
    /**
     * Returns the bindings of the conversation. Method never returns null
     */
    public Bindings getBindings();
    /**
     * Sets the value and scope of the binding
     * @param name the binding name
     * @param value the value of the binding
     * @param scope the scope of the binding
     */
    public void setBinding(String name, Object value, BindingScope scope);
    /**
     * Sets the binding default value
     * @param name the binding name
     * @param defaultValue default value of the binding
     */
    public void setBindingDefaultValue(String name, Object defaultValue);
    /**
     * If method returns <b>true</b> the top level engine must immediate 
     * {@link Conversation#makeConversation(org.raven.conv.ConversationState)  continue
     * the conversation} after actions will be processed
     */
    public boolean hasImmediateTransition();
    /**
     * Sets the <code>immediateTransition</code> flag.
     * @see #hasImmediateTransition() 
     */
    public void setImmediateTransition(boolean immediateTransition);
    /**
     * Returns the next conversation point. The point from which the conversation continues.
     */
    public ConversationScenarioPoint getNextConversationPoint();
    /**
     * Sets the next conversation point. From this point conversation will continues.
     * @throws ConversationCycleDetectedException if detected cycle when used a sequence of the
     *      immediate transions
     */
    public void setNextConversationPoint(ConversationScenarioPoint nextConversationPoint)
            throws ConversationScenarioCycleDetectedException;
    /**
     * Resets bindings of the {@link BindingScope#REQUEST} scope. If
     * {@link #setBindingDefaultValue(java.lang.String, java.lang.Object)  default value} exists for
     * the binding then then the binding will reset to this value else binding will be removed.
     */
    public void resetRequestBindings();
}

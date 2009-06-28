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
public interface ConversationState
{
    /**
     * Returns the bindings of the conversation. Method never returns null
     */
    public Bindings getBindings();
    /**
     * If method returns <b>true</b> the top level engine must immediate 
     * {@link Conversation#makeConversation(org.raven.conv.ConversationState)  continue
     * the conversation} after actions will be processed
     */
    public boolean hasImmediateTransition();
    /**
     * Returns the next conversation point. The point from which the conversation continues.
     */
    public ConversationPoint getNextConversationPoint();
    /**
     * Sets the next conversation point. From this point conversation will continues.
     * @throws ConversationCycleDetectedException if detected cycle when used a sequence of the
     *      immediate transions
     */
    public void setNextConversationPoint(ConversationPoint nextConversationPoint)
            throws ConversationCycleDetectedException;
}

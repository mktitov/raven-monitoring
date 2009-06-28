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

import java.util.Collection;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface Conversation
{
    public final static String NEXT_CONVERSATION_POINT_PARAM = "NEXT_CONVERSATION_POINT";
    public final static String IMMEDIATE_TRANSITIONS_PARAM = "IMMEDIATE_TRANSITIONS";
    public final static String IMMEDIATE_TRANSITION_PARAM = "IMMEDIATE_TRANSITION";

    /**
     * Starts or continues onversation
     * @param state the state of the conversation. The parameter must not be null and must created
     *      by {@link #createConversationState()} method. In bound of the one conversation the
     *      parameter must point to the same object.
     * @return the collection of nodes (actions) that must be executed by using the conversation
     *      engine.
     * @throws org.raven.conv.ConversationException
     */
    public Collection<Node> makeConversation(ConversationState state)
            throws ConversationException;
    /**
     * Creates the conversation state.
     */
    public ConversationState createConversationState() throws ConversationException;
}

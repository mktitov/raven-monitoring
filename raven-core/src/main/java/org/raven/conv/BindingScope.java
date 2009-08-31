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

/**
 * The scopes of the conversation bindings.
 * 
 * @author Mikhail Titov
 */
public enum BindingScope
{
    /**
     * The bindings of this scope are resets after
     * {@link ConversationScenario#makeConversation(org.raven.conv.ConversationScenarioState) }
     */
    REQUEST,
    /**
     * The bindings of this scope are resets after transition to the next conversation point
     */
    POINT,
    /**
     * The bindgins of this scope has conversation life time.
     */
    CONVERSATION
}

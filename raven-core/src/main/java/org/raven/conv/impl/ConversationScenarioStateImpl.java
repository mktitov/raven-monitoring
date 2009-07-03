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

import java.util.HashSet;
import java.util.Set;
import javax.script.Bindings;
import javax.script.SimpleBindings;
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
    private ConversationScenarioPoint nextConversationPoint;
    private final Set<ConversationScenarioPoint> immediateTransitions = new HashSet<ConversationScenarioPoint>();

    public Bindings getBindings()
    {
        return bindings;
    }

    public boolean hasImmediateTransition()
    {
        return nextConversationPoint==null? false : nextConversationPoint.getImmediateTransition();
    }

    public ConversationScenarioPoint getNextConversationPoint()
    {
        return nextConversationPoint;
    }

    public void setNextConversationPoint(ConversationScenarioPoint nextConversationPoint)
            throws ConversationScenarioCycleDetectedException
    {
        if (!nextConversationPoint.getImmediateTransition())
            immediateTransitions.clear();
        else
        {
            if (immediateTransitions.contains(nextConversationPoint))
                throw new ConversationScenarioCycleDetectedException(String.format(
                        "Loop detected. The sequence of immediate transitions returned back " +
                        "to the  (%s) conversation point"
                        , nextConversationPoint.getPath()));
            immediateTransitions.add(nextConversationPoint);
        }
        this.nextConversationPoint = nextConversationPoint;
    }
}

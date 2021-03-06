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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioCycleDetectedException;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.IfNode;
import org.raven.conv.ConversationScenarioPoint;
import org.raven.conv.ConversationScenario;
import org.raven.conv.ConversationScenarioException;
import org.raven.conv.ConversationScenarioState;
import org.raven.tree.Node;
import org.raven.tree.impl.InvisibleNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(
    parentNode=InvisibleNode.class,
    childNodes={IfNode.class, ConversationScenarioPointNode.class, GotoNode.class},
    importChildTypesFromParent=true)
public class ConversationScenarioNode 
        extends ConversationScenarioPointNode
        implements ConversationScenario
{
    protected BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    public Collection<Node> makeConversation(ConversationScenarioState state)
            throws ConversationScenarioException
    {
        ConversationScenarioPoint nextPoint = state.getConversationPoint();
        state.setImmediateTransition(false);
        bindingSupport.putAll(state.getBindings());
        try
        {
            Collection<Node> childs = nextPoint.getEffectiveNodes();
            Long repetitionCount =  (Long) state.getBindings().get(REPEITION_COUNT_PARAM);
            state.getBindings().put(REPEITION_COUNT_PARAM, repetitionCount+1);
            if (childs==null || childs.isEmpty())
                return Collections.EMPTY_LIST;
            Collection<Node> actions = new ArrayList<Node>(childs.size());
            boolean conversationPointSeted = false;
            for (Node action: childs)
                if (action.getStatus().equals(Status.STARTED))
                {
                    if (!conversationPointSeted && action instanceof ConversationScenarioPoint)
                    {
                        state.setNextConversationPoint((ConversationScenarioPoint) action);
                        state.setImmediateTransition(true);
                        conversationPointSeted = true;
                        actions.add(action);
                    }
                    else if (!conversationPointSeted && action instanceof GotoNode)
                    {
                        state.setNextConversationPoint(((GotoNode)action).getConversationPoint());
                        state.setImmediateTransition(true);
                        conversationPointSeted = true;
                        actions.add(action);
                    }
                    else
                        actions.add(action);
                }

            return actions;
        }
        finally
        {
            state.resetRequestBindings();
            bindingSupport.reset();
        }
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public ConversationScenarioState createConversationState()
            throws ConversationScenarioCycleDetectedException
    {
        ConversationScenarioState state =  new ConversationScenarioStateImpl();
        state.setBindingDefaultValue(REPEITION_COUNT_PARAM, 0l);
        state.setBinding(REPEITION_COUNT_PARAM, 0l, BindingScope.POINT);
        state.setNextConversationPoint(this);
        state.switchToNextConversationPoint();
        return state;
    }
}

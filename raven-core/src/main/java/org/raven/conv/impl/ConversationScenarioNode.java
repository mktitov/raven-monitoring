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
    childNodes={IfNode.class, ConversationScenarioPointNode.class},
    importChildTypesFromParent=true)
public class ConversationScenarioNode extends ConversationScenarioPointNode implements ConversationScenario
{
    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    public Collection<Node> makeConversation(ConversationScenarioState state) throws ConversationScenarioException
    {
        ConversationScenarioPoint nextPoint = state.getNextConversationPoint();
        if (nextPoint.getNextPoint()!=null)
            nextPoint = nextPoint.getNextPoint();

        bindingSupport.putAll(state.getBindings());
        try
        {
            Collection<Node> childs = nextPoint.getEffectiveChildrens();
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
                        conversationPointSeted = true;
                    }
                    else
                        actions.add(action);
                }

            return actions;
        }
        finally
        {
            bindingSupport.reset();
        }
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public ConversationScenarioState createConversationState() throws ConversationScenarioCycleDetectedException
    {
        ConversationScenarioState state =  new ConversationScenarioStateImpl();
        state.setNextConversationPoint(this);
        return state;
    }
}

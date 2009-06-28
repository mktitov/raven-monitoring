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

import java.util.Collection;
import java.util.Map;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.IfNode;
import org.raven.conv.ConversationPoint;
import org.raven.conv.Conversation;
import org.raven.conv.ConversationException;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(
    parentNode=InvisibleNode.class,
    childNodes={IfNode.class, ConversationPointNode.class},
    importChildTypesFromParent=true)
public class ConversationNode extends BaseNode implements Conversation
{
    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    public Collection<Node> makeConversation(Map<String, Object> params) throws ConversationException
    {
        Node currentPoint = this;
        ConversationPoint nextPoint = (ConversationPoint) params.get(NEXT_CONVERSATION_POINT_PARAM);
        if (nextPoint!=null)
        {
            currentPoint = nextPoint.getNextPoint();
            if (currentPoint==null)
                currentPoint = nextPoint;
        }

        for (Map.Entry<String, Object> paramEntry: params.entrySet())
            bindingSupport.put(paramEntry.getKey(), paramEntry.getValue());
        try
        {
            Collection<Node> actions = currentPoint.getEffectiveChildrens();
            if (actions!=null && !actions.isEmpty())
                for (Node action: actions)
                    if (action instanceof ConversationPoint)
                        params.put(NEXT_CONVERSATION_POINT_PARAM, action);

            if (!params.containsKey(NEXT_CONVERSATION_POINT_PARAM))
                params.put(NEXT_CONVERSATION_POINT_PARAM, currentPoint);

                
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

}

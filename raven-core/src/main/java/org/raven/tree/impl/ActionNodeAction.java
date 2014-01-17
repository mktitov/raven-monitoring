/*
 *  Copyright 2010 Mikhail Titov.
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

import java.util.Collection;
import java.util.Map;
import org.raven.ds.DataContext;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.tree.ActionViewableObject;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;

/**
 *
 * @author Mikhail Titov
 */
public class ActionNodeAction implements ActionViewableObject
{
    protected final AbstractActionNode actionNode;
    protected final DataContext context;
    protected final Map<String, Object> additionalBindings;
    protected final BindingSupportImpl bindingSupport;
    protected final Map<String, NodeAttribute> actionAttributes;

    public ActionNodeAction(
            AbstractActionNode actionNode
            , DataContext context
            , Map<String, Object> additionalBindings
            , Map<String, NodeAttribute> actionAttributes)
    {
        this.context = context;
        this.additionalBindings = additionalBindings;
        this.actionNode = actionNode;
        this.bindingSupport = actionNode.bindingSupport;
        this.actionAttributes = actionAttributes;
    }

    public String getMimeType()
    {
        return Viewable.RAVEN_ACTION_MIMETYPE;
    }

    public Object getData()
    {
        bindingSupport.put(AbstractActionNode.REFRESH_ATTRIBUTES_BINDING, context.getSessionAttributes());
        bindingSupport.put(AbstractActionNode.DATA_CONTEXT_BINDING, context);
        bindingSupport.put(AbstractActionNode.ACTION_ATTRIBUTES_BINDING, actionAttributes);
        try{
            actionNode.addToBindingSupport(additionalBindings);
            return actionNode.getAttr(AbstractActionNode.ACTION_EXPRESSION_ATTR).getValue();
        }finally {
            bindingSupport.reset();
        }
    }

    public boolean cacheData()
    {
        return false;
    }

    public int getWidth()
    {
        return 0;
    }

    public int getHeight()
    {
        return 0;
    }

    public String getConfirmationMessage()
    {
        bindingSupport.put(AbstractActionNode.REFRESH_ATTRIBUTES_BINDING, context.getSessionAttributes());
        bindingSupport.put(AbstractActionNode.DATA_CONTEXT_BINDING, context);
        try{
            actionNode.addToBindingSupport(additionalBindings);
            return actionNode.getAttr(AbstractActionNode.CONFIRMATION_MESSAGE_ATTR).getValue();
        }finally {
            bindingSupport.reset();
        }
    }

    protected void configureBindings(BindingSupportImpl bindingSupport){}

    @Override
    public String toString()
    {
        bindingSupport.put(AbstractActionNode.REFRESH_ATTRIBUTES_BINDING, context.getSessionAttributes());
        bindingSupport.put(AbstractActionNode.DATA_CONTEXT_BINDING, context);
        try{
            actionNode.addToBindingSupport(additionalBindings);
            return actionNode.getAttr(AbstractActionNode.ENABLED_ACTION_TEXT_ATTR).getValue();
        }finally {
            bindingSupport.reset();
        }
    }

    public Collection<NodeAttribute> getActionAttributes()
    {
        return actionAttributes==null || actionAttributes.isEmpty()? null : actionAttributes.values();
    }

    public boolean isRefreshViewAfterAction()
    {
        bindingSupport.put(AbstractActionNode.REFRESH_ATTRIBUTES_BINDING, context.getSessionAttributes());
        bindingSupport.put(AbstractActionNode.DATA_CONTEXT_BINDING, context);
        try{
            actionNode.addToBindingSupport(additionalBindings);
            Boolean res = actionNode.getAttr(AbstractActionNode.REFRESH_VIEW_AFTER_ACTION_ATTR).getRealValue();
            return res;
        }finally {
            bindingSupport.reset();
        }
    }
}

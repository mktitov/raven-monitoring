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

import java.util.Map;
import javax.script.Bindings;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractActionNode extends BaseNode
{
    public static final String ACTION_EXPRESSION_ATTR = "actionExpression";
    public static final String ACTION_ENABLED_ATTR = "actionEnabled";
    public static final String CONFIRMATION_MESSAGE_ATTR = "confirmationMessage";
    public static final String ENABLED_ACTION_TEXT_ATTR = "enabledActionText";
    public static final String REFRESH_ATTRIBUTES_BINDING = "refreshAttributes";

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String actionExpression;

    @Parameter(defaultValue="true")
    private Boolean actionEnabled;

    @Parameter
    private String enabledActionText;

    @Parameter
    private String disabledActionText;

    @Parameter
    private String confirmationMessage;
    protected BindingSupportImpl bindingSupport;

    public abstract Map<String, Object> prepareActionBindings(
            Map<String, NodeAttribute> refreshAttributes);
    public abstract ViewableObject createActionViewableObject(
            Map<String, NodeAttribute> refreshAttributes, Map<String, Object> additionalBindings);

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    public ViewableObject getActionViewableObject(Map<String, NodeAttribute> refreshAttributes)
    {
        try
        {
            bindingSupport.put(REFRESH_ATTRIBUTES_BINDING, refreshAttributes);
            Map<String, Object> additionalBindings = prepareActionBindings(refreshAttributes);
            addToBindingSupport(additionalBindings);
            Boolean enabled = actionEnabled;
            ViewableObject action = null;
            if (enabled==null || !enabled)
                action = new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, disabledActionText);
            else
                action = createActionViewableObject(refreshAttributes, additionalBindings);

            return action;
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

    public String getConfirmationMessage()
    {
        return confirmationMessage;
    }

    public void setConfirmationMessage(String confirmationMessage)
    {
        this.confirmationMessage = confirmationMessage;
    }

    public String getActionExpression()
    {
        return actionExpression;
    }

    public void setActionExpression(String actionExpression)
    {
        this.actionExpression = actionExpression;
    }

    public Boolean getActionEnabled()
    {
        return actionEnabled;
    }

    public void setActionEnabled(Boolean actionEnabled)
    {
        this.actionEnabled = actionEnabled;
    }

    public String getDisabledActionText()
    {
        return disabledActionText;
    }

    public void setDisabledActionText(String disabledActionText)
    {
        this.disabledActionText = disabledActionText;
    }

    public String getEnabledActionText()
    {
        return enabledActionText;
    }

    public void setEnabledActionText(String enabledActionText)
    {
        this.enabledActionText = enabledActionText;
    }

    public void addToBindingSupport(Map<String, Object> bindings)
    {
        if (bindings!=null)
            for (Map.Entry<String, Object> entry: bindings.entrySet())
                bindingSupport.put(entry.getKey(), entry.getValue());
    }
}

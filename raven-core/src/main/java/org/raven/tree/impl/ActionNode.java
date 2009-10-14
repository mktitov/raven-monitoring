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

package org.raven.tree.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class ActionNode extends BaseNode implements Viewable
{
    public static final String ACTION_EXPRESSION_ATTR = "actionExpression";
    public static final String REFRESH_ATTRIBUTES_BINDING = "refreshAttributes";
    
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String actionExpression;

    @Parameter(defaultValue="true")
    private Boolean actionEnabled;

    @NotNull @Parameter
    private String enabledActionText;

    @NotNull @Parameter
    private String disabledActionText;

    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
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
    
    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;
        bindingSupport.put(REFRESH_ATTRIBUTES_BINDING, refreshAttributes);
        try
        {
            Boolean enabled = actionEnabled;
            ViewableObject action = null;
            if (enabled==null || !enabled)
                action = new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, disabledActionText);
            else
                action = new ActionViewableObject(refreshAttributes);
            return Arrays.asList(action);
        }
        finally
        {
            bindingSupport.reset();
        }
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return NodeUtils.extractRefereshAttributes(this);
    }

    public Boolean getAutoRefresh()
    {
        return false;
    }

    private class ActionViewableObject implements ViewableObject
    {
        private final Map<String, NodeAttribute> refreshAttributes;

        public ActionViewableObject(Map<String, NodeAttribute> refreshAttributes)
        {
            this.refreshAttributes = refreshAttributes;
        }

        public String getMimeType()
        {
            return Viewable.RAVEN_ACTION_MIMETYPE;
        }

        public Object getData()
        {
            bindingSupport.put(REFRESH_ATTRIBUTES_BINDING, refreshAttributes);
            try{
                return getNodeAttribute(ACTION_EXPRESSION_ATTR).getValue();
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

        @Override
        public String toString()
        {
            bindingSupport.put(REFRESH_ATTRIBUTES_BINDING, refreshAttributes);
            try{
                return enabledActionText;
            }finally {
                bindingSupport.reset();
            }
        }
    }
}

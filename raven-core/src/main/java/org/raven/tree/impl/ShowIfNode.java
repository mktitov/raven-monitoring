/*
 *  Copyright 2011 Mikhail Titov.
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
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContextService;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(anyChildTypes=true)
public class ShowIfNode extends BaseNode implements Viewable
{
    public final static String EXPRESSION_ATTR = "expression";

    @Service
    private static UserContextService userContextService;

    @NotNull @Parameter()
    private Boolean expression;

    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields() {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    public boolean isConditionalNode() {
        return isTemplate()? false : true;
    }

    @Override
    public Collection<Node> getEffectiveChildrens() 
    {
        if (!isConditionalNode())
            return null;
        bindingSupport.put(BindingNames.USER_CONTEXT, userContextService.getUserContext());
        try {
            Boolean res = expression;
            return res==null || !res? null : super.getEffectiveChildrens();
        } finally {
            bindingSupport.reset();
        }
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public Boolean getExpression() {
        return expression;
    }

    public void setExpression(Boolean expression) {
        this.expression = expression;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        NodeUtils
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Boolean getAutoRefresh() {
        return true
    }
}

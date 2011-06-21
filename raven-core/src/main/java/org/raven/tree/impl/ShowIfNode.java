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

import java.util.HashMap;
import java.util.LinkedList;
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
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(anyChildTypes=true)
public class ShowIfNode extends AbstractViewableNode
{
    public final static String EXPRESSION_ATTR = "expression";

    @Service
    private static UserContextService userContextService;

    @NotNull @Parameter()
    private Boolean expression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean autoRefresh;

    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields() {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    private boolean calculateExpression(Map<String, NodeAttribute> refAttrs)
    {
        if (!Status.STARTED.equals(getStatus()))
            return false;
        bindingSupport.put(BindingNames.USER_CONTEXT, userContextService.getUserContext());
        if (refAttrs!=null)
            bindingSupport.put(BindingNames.REFRESH_ATTRIBUTES, refAttrs);
        try {
            Boolean res = expression;
            return res==null? false : res;
        } finally {
            bindingSupport.reset();
        }
    }

    public Boolean getExpression() {
        return expression;
    }

    public void setExpression(Boolean expression) {
        this.expression = expression;
    }

    @Override
    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        if (!calculateExpression(null))
            return null;

        Map<String, NodeAttribute> refAttrs = super.getRefreshAttributes();

        List<Node> childs = getSortedChildrens();
        if (childs==null)
            return refAttrs;

        if (refAttrs==null)
            refAttrs = new HashMap<String, NodeAttribute>();
        
        for (Node child: childs)
            if (child instanceof Viewable){
                Map<String, NodeAttribute> childRefAttrs = ((Viewable)child).getRefreshAttributes();
                if (childRefAttrs!=null)
                    refAttrs.putAll(childRefAttrs);
            }
        return refAttrs.isEmpty()? null : refAttrs;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        if (!calculateExpression(refreshAttributes))
            return null;
        List<Node> childs = getSortedChildrens();
        if (childs==null)
            return null;
        List<ViewableObject> viewableObjects = new LinkedList<ViewableObject>();
        for (Node child: childs)
            if (child instanceof Viewable){
                List<ViewableObject> childViewableObjects =
                        ((Viewable)child).getViewableObjects(refreshAttributes);
                if (childViewableObjects!=null)
                    viewableObjects.addAll(childViewableObjects);
            }
        return viewableObjects.isEmpty()? null : viewableObjects;
    }

    public void setAutoRefresh(Boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }

    public Boolean getAutoRefresh() {
        return autoRefresh;
    }
}

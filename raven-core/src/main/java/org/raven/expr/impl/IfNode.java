/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven.expr.impl;

import java.util.Collection;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.Description;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(importChildTypesFromParent=true)
public class IfNode extends BaseNode
{
    public final static String EXPRESSION_ATTRIBUTE = "expression";
    public final static String USEDINTEMPLATE_ATTRIBUTE = "usedInTemplate";
    
    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE, defaultValue="false")
    @Description("The expression that must return boolean value")
    private Boolean expression;
    
    @Parameter(defaultValue="true")
    @Description("If true then condition work in the template")
    private Boolean usedInTemplate;

    @Override
    public Collection<Node> getEffectiveChildrens() 
    {
        Boolean res = expression;
        return !isConditionalNode() || res==null || res==false? 
            null : super.getEffectiveChildrens();
    }

    @Override
    public boolean isConditionalNode() 
    {
        Boolean res = usedInTemplate;
        res = res==null? false : res;
        return isTemplate()? res : !res;
    }

    public Boolean getExpression() 
    {
        return expression;
    }

    public Boolean getUsedInTemplate()
    {
        return usedInTemplate;
    }
    
}

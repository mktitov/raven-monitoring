/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.expr.impl;

import java.util.Collection;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.Condition;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=SwitchNode.class, importChildTypesFromParentLevel=2)
public class ExpressionConditionNode extends BaseNode implements Condition {
    
    public final static String CONDITION_ATTR = "condition";
    
    @NotNull @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Boolean condition;
    
    public boolean checkCondition(Object value) {
        Boolean res = condition;
        return res==null? false : res;
    }

    @Override
    public boolean isConditionalNode() {
        return true;
    }

    @Override
    public Collection<Node> getEffectiveNodes() {
        return !isStarted()? null : super.getEffectiveNodes();
    }

    public Boolean getCondition() {
        return condition;
    }

    public void setCondition(Boolean condition) {
        this.condition = condition;
    }
}

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
import javax.script.Bindings;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.Condition;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes=ExpressionConditionNode.class)
public class SwitchNode extends BaseNode {
    
    @NotNull @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object value;
    
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

    @Override
    public boolean isConditionalNode() {
        return true;
    }

    @Override
    public Collection<Node> getEffectiveNodes() {
        if (!isStarted())
            return null;
        try {
            bindingSupport.enableScriptExecution();
            Object _val = value;
            bindingSupport.put(BindingNames.VALUE_BINDING, _val);
            for (Condition cond: NodeUtils.getChildsOfType(this, Condition.class))
                if (cond.checkCondition(_val))
                    return cond.getEffectiveNodes();
            return null;
        } finally {
            bindingSupport.reset();
        }
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}

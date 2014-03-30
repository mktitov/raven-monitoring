/*
 * Copyright 2014 Mikhail Titov.
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

import org.raven.expr.BindingSupport;
import org.raven.expr.VarsSupportState;
import static org.raven.expr.impl.ExpressionAttributeValueHandler.RAVEN_EXPRESSION_VARS_BINDING;
import static org.raven.expr.impl.ExpressionAttributeValueHandler.RAVEN_EXPRESSION_VARS_INITIATED_BINDING;
import org.raven.tree.Tree;

/**
 *
 * @author Mikhail Titov
 */
public class VarsSupportStateImpl implements VarsSupportState {
    private final BindingSupport varsSupport;
    private final boolean varsInitiated;
    private final Object varsBindings;

    public VarsSupportStateImpl(Tree tree) {
        this.varsSupport = tree.getGlobalBindings(Tree.EXPRESSION_VARS_BINDINGS);
        this.varsInitiated = varsSupport.contains(RAVEN_EXPRESSION_VARS_INITIATED_BINDING);
        this.varsBindings = varsInitiated? varsSupport.get(RAVEN_EXPRESSION_VARS_BINDING) : null;
        this.varsSupport.reset();
    }

    public void restoreState() {
        if (!varsInitiated) 
            varsSupport.reset();
        else {
            varsSupport.put(RAVEN_EXPRESSION_VARS_INITIATED_BINDING, true);
            varsSupport.put(RAVEN_EXPRESSION_VARS_BINDING, varsBindings);
        }
    }
}

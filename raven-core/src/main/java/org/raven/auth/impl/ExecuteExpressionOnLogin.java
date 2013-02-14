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
package org.raven.auth.impl;

import javax.script.Bindings;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.LoginListener;
import org.raven.auth.UserContext;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=LoginListenersNode.class)
public class ExecuteExpressionOnLogin extends BaseNode implements LoginListener, BindingNames {
    
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object onUserLoggedIn;
    
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

    public void userLoggedIn(UserContext userContext) {
        if (!isStarted())
            return;
        try {
            bindingSupport.put(USER_BINDING, userContext);
            getOnUserLoggedIn();
        } finally {
            bindingSupport.reset();
        }
    }

    public Object getOnUserLoggedIn() {
        return onUserLoggedIn;
    }

    public void setOnUserLoggedIn(Object onUserLoggedIn) {
        this.onUserLoggedIn = onUserLoggedIn;
    }
}

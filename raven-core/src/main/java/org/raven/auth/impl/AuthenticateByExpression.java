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
import org.raven.auth.AuthenticatorException;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=AuthenticatorsNode.class)
public class AuthenticateByExpression extends AbstractAuthenticatorNode implements BindingNames {
    
    public final static String AUTHENTICATE_ATTR = "authenticate";
    
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Boolean authenticate;
    
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

    public boolean doCheckAuth(String login, String password) throws AuthenticatorException {
        if (!isStarted())
            return false;
        try {
            bindingSupport.put(LOGIN_BINDING, login);
            bindingSupport.put(PASSWORD_BINDING, password);
            Boolean res = authenticate;
            return res==null? false : res;
        } finally {
            bindingSupport.reset();
        }
    }

    public Boolean getAuthenticate() {
        return authenticate;
    }

    public void setAuthenticate(Boolean authenticate) {
        this.authenticate = authenticate;
    }
}

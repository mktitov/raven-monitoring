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
package org.raven.net.impl;

import javax.script.Bindings;
import org.raven.annotations.Parameter;
import org.raven.auth.LoginService;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.tree.NodeWithBindingSupport;
import org.raven.tree.impl.AttributeReferenceValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class NetworkResponseBaseNode extends BaseNode implements NodeWithBindingSupport {
    @Parameter
    private Class namedParameterType;
    
    @Parameter
    private String namedParameterPattern;
    
    @Parameter(valueHandlerType = AttributeReferenceValueHandlerFactory.TYPE,
                defaultValue = "./@namedParameterPattern")
    private String nodeTitle;
    
    @Parameter(valueHandlerType = NodeReferenceValueHandlerFactory.TYPE)
    private LoginService loginService;
    
    @Parameter
    private Boolean useServerSession;
    
    protected BindingSupportImpl bindingSupport;

    public NetworkResponseBaseNode() {
        super();
    }
    
    public NetworkResponseBaseNode(String name) {
        super(name);
    }

    public BindingSupport getBindingSupport() {
        return bindingSupport;
    }

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

    public Class getNamedParameterType() {
        return namedParameterType;
    }

    public void setNamedParameterType(Class namedParameterType) {
        this.namedParameterType = namedParameterType;
    }

    public String getNamedParameterPattern() {
        return namedParameterPattern;
    }

    public void setNamedParameterPattern(String namedParameterPattern) {
        this.namedParameterPattern = namedParameterPattern;
    }

    public String getNodeTitle() {
        return nodeTitle;
    }

    public void setNodeTitle(String nodeTitle) {
        this.nodeTitle = nodeTitle;
    }

    public LoginService getLoginService() {
        return loginService;
    }

    public void setLoginService(LoginService loginService) {
        this.loginService = loginService;
    }

    public Boolean isUseServerSession() {
        return useServerSession;
    }

    public void setUseServerSession(Boolean useServerSession) {
        this.useServerSession = useServerSession;
    }
}

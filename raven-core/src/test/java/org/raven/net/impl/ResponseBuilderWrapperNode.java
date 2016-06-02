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
import javax.script.SimpleBindings;
import org.raven.auth.UserContext;
import org.raven.auth.impl.AccessRight;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.Response;
import org.raven.net.ResponseBuilder;
import org.raven.net.ResponseContext;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class ResponseBuilderWrapperNode extends BaseNode implements ResponseBuilder {
    private ResponseBuilderWrapper wrappedResponseBuilder;

    public ResponseBuilderWrapper getWrappedResponseBuilder() {
        return wrappedResponseBuilder;
    }

    public void setWrappedResponseBuilder(ResponseBuilderWrapper wrappedResponseBuilder) {
        this.wrappedResponseBuilder = wrappedResponseBuilder;
    }

    public Response buildResponse(UserContext user, ResponseContext responseContext) throws NetworkResponseServiceExeption {
        Bindings bindings = new SimpleBindings();
        formExpressionBindings(bindings);
        return wrappedResponseBuilder.buildResponse(user, responseContext, bindings);
    }

    public AccessRight getAccessRight() {
        return wrappedResponseBuilder.getAccessRight();
    }

    public Node getResponseBuilderNode() {
        return this;
    }

    public Boolean isSessionAllowed() {
        return false;
    }

    public boolean canHandleUnknownPath() {
        return false;
    }

    @Override
    public Long getBuildTimeout() {
        return null;
    }

    @Override
    public Boolean getRequireAudit() {
        return false;
    }

    @Override
    public Boolean getRequireSSL() {
        return false;
    }
    
    public interface ResponseBuilderWrapper {
        public AccessRight getAccessRight();
        public Response buildResponse(UserContext user, ResponseContext responseContext, Bindings bindings);
    }
}

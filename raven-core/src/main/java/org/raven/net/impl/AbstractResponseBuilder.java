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

import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.auth.impl.AccessRight;
import org.raven.log.LogLevel;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.Response;
import org.raven.net.ResponseBuilder;
import org.raven.net.ResponseContext;
import org.raven.tree.Node;
import org.raven.util.OperationStatistic;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractResponseBuilder extends NetworkResponseBaseNode implements ResponseBuilder {
    
    @NotNull @Parameter(defaultValue="text/plain")
    private String responseContentType;
    
    @Parameter(readOnly=true)
    private OperationStatistic requestsStat;
    
    @Parameter
    private AccessRight minimalAccessRight;
    
    private ParametersSupport paramsSupport;

    @Override
    protected void initFields() {
        super.initFields(); 
        paramsSupport = new ParametersSupport(converter);
        requestsStat = new OperationStatistic();
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit(); 
        initNodes(false);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart(); 
        initNodes(true);
    }

    private void initNodes(boolean start) {
        paramsSupport.initNodes(this, start);
    }
    
    public Response buildResponse(UserContext user, ResponseContext responseContext) 
            throws NetworkResponseServiceExeption 
    {
        long ts = requestsStat.markOperationProcessingStart();
        try {
            paramsSupport.checkParameters(this, responseContext.getRequest().getParams());
            try {
                return new ResponseImpl(
                        responseContentType, 
                        buildResponseContent(user, responseContext), 
                        responseContext.getHeaders());
            } catch (Exception e) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error("Problem with building response", e);
                throw new NetworkResponseServiceExeption("Problem with building response", e);
            }
        } finally {
            requestsStat.markOperationProcessingEnd(ts);
        }
                
    }

    public AccessRight getAccessRight() {
        final AccessRight _right = minimalAccessRight;
        if (_right!=null) 
            return _right;
        else if (getName().equals("?GET"))
            return AccessRight.READ;
        else if (getName().equals("?PUT"))
            return AccessRight.WRITE;
        else if (getName().equals("?POST"))
            return AccessRight.TREE_EDIT;
        else if (getName().equals("?DELETE"))
            return AccessRight.CONTROL;
        else return null;
    }
    
    protected abstract Object buildResponseContent(UserContext user, ResponseContext responseContext) throws Exception;

    public Node getResponseBuilderNode() {
        return this;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }

    public OperationStatistic getRequestsStat() {
        return requestsStat;
    }

    public AccessRight getMinimalAccessRight() {
        return minimalAccessRight;
    }

    public void setMinimalAccessRight(AccessRight minimalAccessRight) {
        this.minimalAccessRight = minimalAccessRight;
    }

}

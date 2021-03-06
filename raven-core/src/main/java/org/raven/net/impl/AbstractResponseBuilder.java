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

import java.nio.charset.Charset;
import java.util.Date;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.auth.impl.AccessRight;
import org.raven.log.LogLevel;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.Response;
import org.raven.net.ResponseBuilder;
import org.raven.net.ResponseContext;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.util.OperationStatistic;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractResponseBuilder extends NetworkResponseBaseNode implements ResponseBuilder {
    
    public final static String USE_PARAMETERS_ATTR = "useParameters";
    
    @Parameter(readOnly=true)
    private OperationStatistic requestsStat;
    
    @Parameter
    private AccessRight minimalAccessRight;
    
    @Parameter
    private Boolean useServerSession;
    
    @NotNull @Parameter(defaultValue = "false")
    private Boolean useParameters;
    
    @NotNull @Parameter(defaultValue = "false")
    private Boolean canHandleUnknownPath;
    
    @Parameter
    private Long buildTimeout;
    
    @Parameter
    private Boolean requireAudit;
    
    @Parameter
    private Boolean requireSSL;
    
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
//        initNodes(false);
//        syncParametersNodeState(useParameters);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        syncParametersNodeState(useParameters);
//        initNodes(true);
    }

    @Override
    public boolean isStartAfterChildrens() {
        return true;
    }

    private void initNodes(boolean start) {
        paramsSupport.initNodes(this, start);
    }
    
    public Response buildResponse(UserContext user, ResponseContext responseContext) 
            throws NetworkResponseServiceExeption 
    {
        long ts = requestsStat.markOperationProcessingStart();
        try {
            final long ifModifiedSince = responseContext.getRequest().getIfModifiedSince();
            try {
                Long lastModified = doGetLastModified();
                if (ifModifiedSince>=0 && lastModified!=null && lastModified/1000 <= ifModifiedSince/1000)
                    return Response.NOT_MODIFIED;
                else {
                    if (useParameters)
                        paramsSupport.checkParameters(this, responseContext.getRequest().getParams());
                    Object result = buildResponseContent(user, responseContext);
                    if (result instanceof Response)
                        return (Response)result;
                    else
                        return new ResponseImpl(getContentType(), result, responseContext.getHeaders(), 
                                lastModified, getContentCharset());
                }
            } catch (Exception e) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error("Problem with building response", e);
                if (e instanceof NetworkResponseServiceExeption)
                    throw (NetworkResponseServiceExeption)e;
                else
                    throw new NetworkResponseServiceExeption("Problem with building response", e);
            }
        } finally {
            requestsStat.markOperationProcessingEnd(ts);
        }
    }

    public Boolean isSessionAllowed() {
        return useServerSession;
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
    
    protected abstract Long doGetLastModified() throws Exception;
    protected abstract String getContentType();
    protected abstract Charset getContentCharset() throws Exception;
    protected abstract Object buildResponseContent(UserContext user, ResponseContext responseContext) throws Exception;

    public Boolean getUseParameters() {
        return useParameters;
    }

    public void setUseParameters(Boolean useParameters) {
        this.useParameters = useParameters;
    }

    public Node getResponseBuilderNode() {
        return this;
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

    public Boolean getUseServerSession() {
        return useServerSession;
    }

    public void setUseServerSession(Boolean useServerSession) {
        this.useServerSession = useServerSession;
    }

    public Boolean getCanHandleUnknownPath() {
        return canHandleUnknownPath;
    }

    public void setCanHandleUnknownPath(Boolean canHandleUnknownPath) {
        this.canHandleUnknownPath = canHandleUnknownPath;
    }

    public boolean canHandleUnknownPath() {
        return canHandleUnknownPath;
    }

    public Long getBuildTimeout() {
        return buildTimeout;
    }

    public void setBuildTimeout(Long buildTimeout) {
        this.buildTimeout = buildTimeout;
    }

    public Boolean getRequireAudit() {
        return requireAudit;
    }

    public void setRequireAudit(Boolean requireAudit) {
        this.requireAudit = requireAudit;
    }

    public Boolean getRequireSSL() {
        return requireSSL;
    }

    public void setRequireSSL(Boolean requireSSL) {
        this.requireSSL = requireSSL;
    }

    @Override
    public void nodeAttributeValueChanged(Node node, NodeAttribute attr, Object oldValue, Object newValue) {
        super.nodeAttributeValueChanged(node, attr, oldValue, newValue);
        if (node==this && USE_PARAMETERS_ATTR.equals(attr.getName())) {
            syncParametersNodeState(newValue);
        }
    }

    @Parameter(readOnly = true)
    public String getLastModifiedDate() {
        try {
            return doGetLastModified()==null? null : new Date(doGetLastModified()).toString();
        } catch (Exception ex) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                logger.error("Error in getLastModifiedDate", ex);
            return null;
        }
    }

    private void syncParametersNodeState(Object newValue) throws NodeError {
        if (Boolean.TRUE.equals(newValue))
            paramsSupport.initNodes(this, true);
        else {
            Node parametersNode = paramsSupport.getParametersNode(this);
            if (parametersNode!=null) {
                if (parametersNode.getNodesCount()>0)
                    parametersNode.stop();
                else
                    tree.remove(parametersNode);
            }
        }
    }
}

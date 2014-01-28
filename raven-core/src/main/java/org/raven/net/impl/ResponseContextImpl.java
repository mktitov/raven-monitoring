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

import java.util.HashMap;
import java.util.Map;
import org.raven.BindingNames;
import org.raven.auth.LoginService;
import org.raven.auth.UserContext;
import org.raven.auth.impl.AccessRight;
import org.raven.expr.BindingSupport;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.Request;
import org.raven.net.Response;
import org.raven.net.ResponseBuilder;
import org.raven.net.ResponseContext;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class ResponseContextImpl implements ResponseContext {
    public final static String INCLUDE_JQUERY_STR = 
            "<script src=\"%s/jquery/jquery-1.10.1.min.js\"></script>";
    public final static String INCLUDE_JQUERY_CSS_STR = 
            "<link rel=\"%s/stylesheet\" href=\"jquery/themes/base/jquery.ui.all.css\"/>";
    public final static String INCLUDE_JQUERY_UI_STR = 
            "<script src=\"%s/jquery/ui/jquery-ui.js\"></script>";
    private final Request request;
//    private final String contextPath;
    private final String builderPath;
    private final String subcontext;
//    private final String requesterIp;
    private final long requestId;
    private final LoginService loginService;
    private final ResponseBuilder responseBuilder;
    private final LoggerHelper logger;
    private final LoggerHelper responseBuilderLogger;
    private final NetworkResponseServiceNode serviceNode;
    private Map<String, String> headers;

    public ResponseContextImpl(Request request, String builderPath, String subcontext, long requestId, 
            LoginService loginService, ResponseBuilder responseBuilder, NetworkResponseServiceNode serviceNode) 
    {
        this.request = request;
        this.builderPath = builderPath;
        this.subcontext = subcontext;
        this.requestId = requestId;
        this.loginService = loginService;
        this.responseBuilder = responseBuilder;
        this.serviceNode = serviceNode;
        this.logger = new LoggerHelper(serviceNode, "["+requestId+"] ");
        this.responseBuilderLogger = new LoggerHelper(responseBuilder.getResponseBuilderNode(), "["+requestId+"] ");
    }
    
    public LoginService getLoginService() {
        return loginService;
    }

    public ResponseBuilder getResponseBuilder() {
        return responseBuilder;
    }

    public Request getRequest() {
        return request;
    }

    public Logger getLogger() {
        return logger;
    }

    public LoggerHelper getResponseBuilderLogger() {
        return responseBuilderLogger;
    }

    public Map<String, String> getHeaders() {
        if (headers==null)
            headers = new HashMap<String, String>();
        return headers;
    }

    public int getAccessRightsForUser(UserContext user) {
        return user.getAccessForNode(responseBuilder.getResponseBuilderNode());
    }

    public boolean isAccessGranted(UserContext user) {
        AccessRight accessRight = responseBuilder.getAccessRight();
        if (accessRight==null) return false;
        else {
            int rights = user.getAccessForNode(responseBuilder.getResponseBuilderNode());
            int needRights = responseBuilder.getAccessRight().getDecodedRights();
            return (rights & needRights)==needRights;
        }
    }
    
    public Response getResponse(UserContext user) throws NetworkResponseServiceExeption {
        if (subcontext!=null && !subcontext.isEmpty())
            request.getParams().put(NetworkResponseServiceNode.SUBCONTEXT_PARAM, subcontext);
        if (logger.isDebugEnabled()) 
            logger.debug(String.format(
                "[%d] Processing request. User (%s), remote address (%s), context (%s), request parameters: %s"
                , requestId, user.getLogin(), request.getRemoteAddr(), request.getContextPath(), 
                paramsToString(request.getParams())));
        try {
            try {
                BindingSupport bindingSupport = serviceNode.getBindingSupport();
                try {
                    bindingSupport.put(BindingNames.USER_CONTEXT, user);
                    bindingSupport.put(BindingNames.REQUEST_BINDING, request);
                    bindingSupport.put(BindingNames.RESPONSE_BINDING, request.getAppPath());
                    bindingSupport.put(BindingNames.INCLUDE_JQUERY, preparePath(INCLUDE_JQUERY_STR));
                    bindingSupport.put(BindingNames.INCLUDE_JQUERY_CSS, preparePath(INCLUDE_JQUERY_CSS_STR));
                    bindingSupport.put(BindingNames.INCLUDE_JQUERY_UI, preparePath(INCLUDE_JQUERY_UI_STR));
                    Response response = responseBuilder.buildResponse(user, this);
                    return response;
                } finally {
                    bindingSupport.reset();
                }
            } catch (NetworkResponseServiceExeption e) {
                serviceNode.incRequestsCountWithErrors();
                if (logger.isWarnEnabled())
                    logger.warn(String.format(
                            "[%d] Error processing request from (%s). %s"
                            , requestId, request.getContextPath(), e.getMessage()));
                throw e;
            }
        } catch (RuntimeException e) {
            serviceNode.incRequestsCountWithErrors();
            if (logger.isErrorEnabled())
                logger.error(String.format("[%d] Error processing request from (%s). %s"
                        , requestId, request.getContextPath(), e.getMessage())
                    , e);
            throw e;
        }
    }
    
    private String preparePath(String path) {
        return String.format(path, request.getAppPath());
    }
    
    private static String paramsToString(Map<String, Object> params) {
        if (params==null || params.isEmpty())
            return "NO PARAMETERS";
        StringBuilder buf = new StringBuilder();
        boolean firstIteration = true;
        for (Map.Entry<String, Object> param: params.entrySet()) {
            if (!firstIteration)
                buf.append("; ");
            buf.append(param.getKey()).append(" - (").append(param.getValue()).append(")");
            if (firstIteration)
                firstIteration = false;
        }
        return buf.toString();
    }
}

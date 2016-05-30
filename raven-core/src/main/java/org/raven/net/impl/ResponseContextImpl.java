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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import org.raven.BindingNames;
import org.raven.auth.LoginService;
import org.raven.auth.UserContext;
import org.raven.auth.impl.AccessRight;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.Request;
import org.raven.net.Response;
import org.raven.net.ResponseBuilder;
import org.raven.net.ResponseAdapter;
import org.raven.net.ResponseContext;
import org.raven.net.ResponseContextListener;
import org.raven.net.ResponseServiceNode;
import org.raven.net.http.server.HttpSession;
import org.raven.sched.ExecutorService;
import org.raven.tree.Tree;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.weda.internal.annotations.Service;

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
    
    @Service
    private static Tree tree;
    
    private final Request request;
//    private final String contextPath;
    private final String builderPath;
    private final String subcontextPath;
//    private final String requesterIp;
    private final long requestId;
    private final LoginService loginService;
    private final ResponseBuilder responseBuilder;
    private final LoggerHelper logger;
    private final LoggerHelper responseBuilderLogger;
    private final ResponseServiceNode serviceNode;
    private final boolean sessionAllowed;
    private final ExecutorService executor;
    private AtomicBoolean headersAdded = new AtomicBoolean();
    private volatile ResponseAdapter responseAdapter;
    private volatile HttpSession session;
    private Map<String, String> headers;
    private Set<ResponseContextListener> listeners;

    public ResponseContextImpl(Request request, String builderPath, String subcontext, long requestId, 
            LoginService loginService, ResponseBuilder responseBuilder, ResponseServiceNode serviceNode,
            ExecutorService executor) 
    {
        this.request = request;
        this.builderPath = builderPath;
        this.subcontextPath = subcontext;
        this.requestId = requestId;
        this.loginService = loginService;
        this.responseBuilder = responseBuilder;
        this.serviceNode = serviceNode;
        this.logger = new LoggerHelper(serviceNode, "["+requestId+"] ");
        this.responseBuilderLogger = new LoggerHelper(responseBuilder.getResponseBuilderNode(), "["+requestId+"] ");
        Boolean _sessionAllowed = responseBuilder.isSessionAllowed();
        this.sessionAllowed = _sessionAllowed==null? false : _sessionAllowed;
        this.executor = executor;
    }

    @Override
    public void setSession(HttpSession session) {
        this.session = session;
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

    @Override
    public void attachResponseAdapter(ResponseAdapter responseAdapter) {
        this.responseAdapter = responseAdapter;
    }
 
    @Override
    public synchronized void addListener(ResponseContextListener listener) {
        if (listeners==null)
            listeners = new ConcurrentSkipListSet<>();
        listeners.add(listener);
    }

    @Override
    public synchronized void removeListener(ResponseContextListener listener) {
        if (listeners!=null)
            listeners.remove(listener);
    }

    @Override
    public Set<ResponseContextListener> getListeners() {
        return listeners;
    }
    
    @Override
    public String getSubcontextPath() {
        return subcontextPath;
    }

    @Override
    public LoginService getLoginService() {
        return loginService;
    }

    @Override
    public ResponseBuilder getResponseBuilder() {
        return responseBuilder;
    }

    @Override
    public ResponseServiceNode getServiceNode() {
        return serviceNode;
    }

    @Override
    public Request getRequest() {
        return request;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public LoggerHelper getResponseBuilderLogger() {
        return responseBuilderLogger;
    }

    @Override
    public Map<String, String> getHeaders() {
        if (headers==null)
            headers = new HashMap<String, String>();
        return headers;
    }

    public int getAccessRightsForUser(UserContext user) {
        return user.getAccessForNode(responseBuilder.getResponseBuilderNode());
    }

    @Override
    public boolean isAccessGranted(UserContext user) {
        AccessRight accessRight = responseBuilder.getAccessRight();
        if (accessRight==null) return false;
        else {
            int rights = user.getAccessForNode(responseBuilder.getResponseBuilderNode());
            int needRights = responseBuilder.getAccessRight().getDecodedRights();
            return (rights & needRights)==needRights;
        }
    }

    @Override
    public boolean isSessionAllowed() {
        return sessionAllowed;
    }

    @Override
    public Response getResponse(UserContext user) throws NetworkResponseServiceExeption {
        return getResponse(this, user);
    }

    @Override
    public Response getResponse(ResponseContext delegate, UserContext user) throws NetworkResponseServiceExeption {
        final Request request = delegate.getRequest();
        final String subcontextPath = delegate.getSubcontextPath();
        if (subcontextPath!=null && !subcontextPath.isEmpty())
            request.getParams().put(NetworkResponseServiceNode.SUBCONTEXT_PARAM, subcontextPath);
        if (logger.isDebugEnabled()) 
            logger.debug(String.format(
                "[%d] Processing request. User (%s), remote address (%s), context (%s), request parameters: %s"
                , requestId, user.getLogin(), request.getRemoteAddr(), request.getContextPath(), 
                paramsToString(request.getParams())));
        try {
            try {
                BindingSupport bindingSupport = serviceNode.getBindingSupport();
                bindingSupport.enableScriptExecution();
                final BindingSupport globalBindings = new BindingSupportImpl();
                final String bindingsId = tree.addGlobalBindings(globalBindings);
                try {
                    globalBindings.put(BindingNames.USER_CONTEXT, user);
                    globalBindings.put(BindingNames.REQUEST_BINDING, request);
                    globalBindings.put(BindingNames.RESPONSE_BINDING, delegate);
                    globalBindings.put(BindingNames.ROOT_PATH, request.getRootPath());
                    globalBindings.put(BindingNames.APP_NODE, request.getParams().get(BindingNames.APP_NODE));
                    globalBindings.put(BindingNames.APP_PATH, request.getParams().get(BindingNames.APP_PATH));
                    globalBindings.put(BindingNames.CONTEXT_PATH, request.getContextPath());
                    globalBindings.put(BindingNames.INCLUDE_JQUERY, preparePath(INCLUDE_JQUERY_STR));
                    globalBindings.put(BindingNames.INCLUDE_JQUERY_CSS, preparePath(INCLUDE_JQUERY_CSS_STR));
                    globalBindings.put(BindingNames.INCLUDE_JQUERY_UI, preparePath(INCLUDE_JQUERY_UI_STR));
                    Response response = delegate.getResponseBuilder().buildResponse(user, delegate);
                    return response;
                } finally {
                    globalBindings.reset();
                    tree.removeGlobalBindings(bindingsId);
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
        return String.format(path, request.getRootPath());
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
    
    private void addHeadersToResponse() {
        if (headersAdded.compareAndSet(false, true) && headers!=null) 
            for (Map.Entry<String, String> header: headers.entrySet()) 
                responseAdapter.addHeader(header.getKey(), header.getValue());
    }

    @Override
    public OutputStream getResponseStream() throws IOException {
        addHeadersToResponse();
        return responseAdapter.getStream();
    }

    @Override
    public PrintWriter getResponseWriter() throws IOException {        
        addHeadersToResponse();
        return responseAdapter.getWriter();
    }

    @Override
    public void closeChannel() throws IOException {
        responseAdapter.close();
    }

    @Override
    public void channelClosed() {
        Set<ResponseContextListener> _listeners = listeners;
        if (_listeners!=null)
            for (ResponseContextListener listener: _listeners)
                listener.contextClosed(this);
    }

    @Override
    public ExecutorService getExecutor() {
        return executor;
    }

}

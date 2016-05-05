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

package org.raven.ui.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.CometEvent;
import org.raven.auth.LoginService;
import org.raven.auth.UserContext;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.Request;
import org.raven.net.Response;
import org.raven.net.ResponseBuilder;
import org.raven.net.ResponseContext;
import org.raven.net.ResponseContextListener;
import org.raven.net.ResponseServiceNode;
import org.raven.ui.servlet.NetworkResponseServlet.RequestContext;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class CometResponseContext implements ResponseContext {
    private final RequestContext requestContext;
    private final ResponseContext responseContext;
    private final HttpServletResponse httpResponse;
    private final AtomicBoolean headersAdded = new AtomicBoolean();
    private final CometEvent cometEvent;

    public CometResponseContext(RequestContext requestContext, CometEvent event)
    {
        this.responseContext = requestContext.responseContext;
        this.httpResponse = requestContext.response;
        this.requestContext = requestContext;
        this.cometEvent = event;
    }
    
    private void addHeadersToResponse() {
        final Map<String, String> headers = responseContext.getHeaders();
        if (headersAdded.compareAndSet(false, true) && headers!=null) 
            for (Map.Entry<String, String> header: headers.entrySet()) 
                httpResponse.addHeader(header.getKey(), header.getValue());
    }
    
    public OutputStream getResponseStream() throws IOException {
        addHeadersToResponse();
        return httpResponse.getOutputStream();
    }

    public PrintWriter getResponseWriter() throws IOException {
        addHeadersToResponse();
        return httpResponse.getWriter();
    }

    public void closeChannel() throws IOException {
        requestContext.writeProcessed(cometEvent, true);
    }
    
    //other calls to ResponseContext delegating to normal response context   
    public void addListener(ResponseContextListener listener) {
        responseContext.addListener(listener);
    }

    public void removeListener(ResponseContextListener listener) {
        responseContext.removeListener(listener);
    }

    public Set<ResponseContextListener> getListeners() {
        return responseContext.getListeners();
    }

    public void channelClosed() {
        Set<ResponseContextListener> listeners = responseContext.getListeners();
        if (listeners!=null)
            for (ResponseContextListener listener: listeners)
                listener.contextClosed(this);
    }

    @Override
    public LoginService getLoginService() {
        return responseContext.getLoginService();
    }

    @Override
    public Request getRequest() {
        return responseContext.getRequest();
    }

    @Override
    public Map<String, String> getHeaders() {
        return responseContext.getHeaders();
    }

    @Override
    public boolean isAccessGranted(UserContext user) {
        return responseContext.isAccessGranted(user);
    }

    @Override
    public boolean isSessionAllowed() {
        return responseContext.isSessionAllowed();
    }

    @Override
    public String getSubcontextPath() {
        return responseContext.getSubcontextPath();
    }

    @Override
    public Response getResponse(UserContext user) throws NetworkResponseServiceExeption {
        return responseContext.getResponse(this, user);
    }

    public Response getResponse(ResponseContext delegate, UserContext user) throws NetworkResponseServiceExeption {
        return responseContext.getResponse(delegate, user);
    }

    @Override
    public ResponseBuilder getResponseBuilder() {
        return responseContext.getResponseBuilder();
    }

    public ResponseServiceNode getServiceNode() {
        return responseContext.getServiceNode();
    }
    
    @Override
    public Logger getLogger() {
        return responseContext.getLogger();
    }

    @Override
    public Logger getResponseBuilderLogger() {
        return responseContext.getResponseBuilderLogger();
    }    
        
}

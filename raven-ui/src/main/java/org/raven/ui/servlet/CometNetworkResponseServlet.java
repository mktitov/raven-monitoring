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
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.CometEvent;
import org.apache.catalina.CometProcessor;
import org.apache.tapestry5.ioc.Registry;
import org.raven.auth.AuthenticationFailedException;
import org.raven.auth.UserContext;
import org.raven.log.LogLevel;
import org.raven.net.AccessDeniedException;
import org.raven.net.CometRequest;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceUnavailableException;
import org.raven.net.RequiredParameterMissedException;
import org.raven.net.Response;
import org.raven.net.ResponseContext;
import org.raven.net.UnauthoriedException;
import org.raven.net.impl.CometRequestImpl;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.ui.util.RavenRegistry;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class CometNetworkResponseServlet extends NetworkResponseServlet implements CometProcessor {

    public void event(CometEvent ce) throws IOException, ServletException {
        final HttpServletRequest request = ce.getHttpServletRequest();
        final HttpServletResponse response = ce.getHttpServletResponse();
        switch (ce.getEventType()) {
            case BEGIN:
                initResponseProcessing(ce, request, response);
                break;
            case READ:
                break;
            case END:
                break;
            case ERROR:
                break;
        }
    }
    
    private void initResponseProcessing(CometEvent ce, HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException
    {
        final Registry registry = RavenRegistry.getRegistry();
        final NetworkResponseService responseService = registry.getService(NetworkResponseService.class);
        ResponseContext responseContext = null;
        
        try {
            final ExecutorService executor = responseService.getExecutor();
            if (executor==null)
                throw new ServletException("Comet servlet can't work without executor service");
            checkRequest(request, response);
            String context = request.getPathInfo().substring(1);
            Map<String, Object> params = extractParams(request, responseService);
            Map<String, Object> headers = extractHeaders(request);
            CometRequest serviceRequest = new CometRequestImpl(request.getRemoteAddr(), params, headers, context, 
                    request.getMethod().toUpperCase(), request);
            responseContext = responseService.getResponseContext(serviceRequest);
            UserContext user = checkAuth(request, response, responseContext, context);
            Response result = responseContext.getResponse(user);
            executor.execute(new AbstractTask(responseContext.getResponseBuilder().getResponseBuilderNode(), "Processing http request") {                
                @Override
                public void doRun() throws Exception {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
        } catch (Throwable e) {
            processError(request, response, responseService, responseContext, e);
        }
    }
}

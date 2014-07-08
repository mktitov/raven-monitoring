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
import org.raven.net.Request;
import org.raven.net.Response;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class CometNetworkResponseServlet extends NetworkResponseServlet implements CometProcessor {
    private static final String REQUEST_CONTEXT = "RAVEN_REQUEST_CONTEXT";

    public void event(CometEvent ce) throws IOException, ServletException {
        final HttpServletRequest request = ce.getHttpServletRequest();
        final HttpServletResponse response = ce.getHttpServletResponse();
        final RequestContext ctx = getRequestContext(ce);
        if (ctx!=null && ctx.servletLogger.isDebugEnabled())
            ctx.servletLogger.debug(String.format("Processing %s event for URI: %", ce.getEventType(), request.getRequestURI()));
        switch (ce.getEventType()) {
            case BEGIN:
                initResponseProcessing(ce, request, response);
                break;
            case READ:
                processReadEvent(ce);
                break;
            case END:
                processEndEvent(ce);
                break;
            case ERROR:
                processErrorEvent(ce);
                break;
        }
    }
    
    private void initResponseProcessing(CometEvent ce, HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException
    {
        final RequestContext ctx = createContext(request, response);
        request.setAttribute("org.apache.tomcat.comet.timeout", 30 * 1000); //30 seconds
	
        if (ctx.servletLogger.isDebugEnabled())
            ctx.servletLogger.debug("Processing BEGIN event for URI - "+request.getRequestURI());
        request.setAttribute(REQUEST_CONTEXT, ctx);
        try {
            final ExecutorService executor = ctx.responseService.getExecutor();
            if (executor==null)
                throw new ServletException("Comet servlet can't work without executor service");
            configureRequestContext(ctx);
            ctx.responseContext = new CometResponseContext(ctx.responseContext, ctx.response, ce);
            final Node builderNode = ctx.responseContext.getResponseBuilder().getResponseBuilderNode();
            executor.execute(new AbstractTask(builderNode, "Processing http request") {                
                @Override public void doRun() throws Exception {
                    try {
                        long ts = System.currentTimeMillis();
                        if (ctx.servletLogger.isDebugEnabled())
                            ctx.servletLogger.debug("Composing response using builder: "
                                    +ctx.responseContext.getResponseBuilder().getResponseBuilderNode());
                        if (ctx.responseContext.getResponseBuilderLogger().isDebugEnabled())
                            ctx.responseContext.getResponseBuilderLogger().debug("Composing response");
                        Response result = ctx.responseContext.getResponse(ctx.user);
                        if (ctx.servletLogger.isDebugEnabled())
                            ctx.servletLogger.debug("Composed by {}ms. Handling...", System.currentTimeMillis()-ts);
                        processServiceResponse(ctx, result);
                        if (ctx.servletLogger.isDebugEnabled())
                            ctx.servletLogger.debug("Handled", System.currentTimeMillis()-ts);
                    } catch (Throwable e) {
                        if (ctx.servletLogger.isErrorEnabled())
                            ctx.servletLogger.error("Response composing error", e);
                        processError(ctx, e);
                    }
                }
            });
        } catch (Throwable e) {
            processError(ctx, e);
        }
    }

    private void processEndEvent(CometEvent ev) {
        try {
            RequestContext ctx = getRequestContext(ev);
            if (ctx.servletLogger.isDebugEnabled())
                ctx.servletLogger.debug("Processing END event for URI: {}", ctx.request.getRequestURI());
            if (ctx!=null)
                ((CometRequestImpl)ctx.responseContext.getRequest()).requestStreamClosed();
        } finally {
//            ev.close();
        }
    }

    private RequestContext getRequestContext(CometEvent ev) {
        final RequestContext ctx = (RequestContext) ev.getHttpServletRequest().getAttribute(REQUEST_CONTEXT);
        return ctx;
    }
    
    @Override
    protected void processServiceResponse(RequestContext ctx, Response serviceResponse) throws IOException {
        try {
            if (ctx.responseContext.getResponseBuilderLogger().isDebugEnabled())
                ctx.responseContext.getResponseBuilderLogger().debug("Builder returned response. Handling");
            if (serviceResponse != Response.ALREADY_COMPOSED && serviceResponse != Response.MANAGING_BY_BUILDER)
                super.processServiceResponse(ctx, serviceResponse);
        } finally {
            if (serviceResponse!=Response.MANAGING_BY_BUILDER)
                ctx.responseContext.closeChannel();
        }
    }

    @Override
    protected Request createServiceRequest(RequestContext ctx, Map<String, Object> params, 
            Map<String, Object> headers, String context) 
    {
        return new CometRequestImpl(ctx.request.getRemoteAddr(), params, headers, context, 
                    ctx.request.getMethod().toUpperCase(), ctx.request);
    }

    private void processErrorEvent(CometEvent ce) throws IOException {
        try {
            RequestContext ctx = getRequestContext(ce);
            Logger logger = ctx.responseContext.getResponseBuilderLogger();
            if (logger.isWarnEnabled())
                logger.warn("channel disconnected");
        } finally {
            ce.close();
        }
    }

    private void processReadEvent(CometEvent ce) {
        CometRequest req = (CometRequest) getRequestContext(ce).responseContext.getRequest();
        req.requestStreamReady();
    }
}

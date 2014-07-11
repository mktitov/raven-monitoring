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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.CometEvent;
import org.apache.catalina.CometProcessor;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
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
    public static final String REQUEST_CONTEXT = "RAVEN_REQUEST_CONTEXT";

    public void event(CometEvent ce) throws IOException, ServletException {
        final HttpServletRequest request = ce.getHttpServletRequest();
        final HttpServletResponse response = ce.getHttpServletResponse();
        final RequestContext ctx = getRequestContext(ce);
        if (ctx!=null && ctx.servletLogger.isDebugEnabled()) {
            CometEvent.EventSubType subtype = ce.getEventSubType();
            String event = ce.getEventType()+(subtype==null?"":"/"+subtype);
            ctx.servletLogger.debug(String.format("Processing %s event (id: %s) for URI: %s", event, ce, request.getRequestURI()));
        }
        switch (ce.getEventType()) {
            case BEGIN:
                if (ctx==null)
                    initResponseProcessing(ce, request, response);
                else {
                    if (ctx.servletLogger.isDebugEnabled())
                        ctx.servletLogger.debug("received REPEATED BEGIN event");
                    if (ctx.isWriteProcessed()) {
                        if (ctx.servletLogger.isDebugEnabled())
                            ctx.servletLogger.debug("Builder finished response composing. Closing channel");
                        ce.close();
                    }
                }
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
    
    private void initResponseProcessing(final CometEvent ce, final HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException
    {
        final RequestContext ctx = createContext(request, response);
//        request.setAttribute("org.apache.tomcat.comet.timeout", 5); //30 seconds	
        request.setAttribute("org.apache.tomcat.comet.timeout", 1000); //30 seconds	
        final boolean debugEnabled = ctx.servletLogger.isDebugEnabled();
        if (debugEnabled) 
            ctx.servletLogger.debug(String.format("Processing %s event (id: %s) for URI: %s", 
                    ce.getEventType(), ce, request.getRequestURI()));            
        request.setAttribute(REQUEST_CONTEXT, ctx);
        try {
            final ExecutorService executor = ctx.responseService.getExecutor();
            if (executor==null)
                throw new ServletException("Comet servlet can't work without executor service");
            configureRequestContext(ctx);
            ctx.responseContext = new CometResponseContext(ctx.responseContext, ctx.response, ce);
            final Node builderNode = ctx.responseContext.getResponseBuilder().getResponseBuilderNode();
            final long ts = System.currentTimeMillis();
            executor.execute(new AbstractTask(builderNode, "Processing http request") {                
                @Override public void doRun() throws Exception {
                    try {
                        ctx.builderExecutedTs = System.currentTimeMillis();
                        final boolean isMultipart = ServletFileUpload.isMultipartContent(request);
                        if (isMultipart) {
                            if (debugEnabled)
                                ctx.servletLogger.debug("Processing multipart content");
                            parseMultipartContentParams(ctx.responseContext.getRequest().getParams(), ctx.request, 
                                    ctx.responseService);
                            if (debugEnabled)
                                ctx.servletLogger.debug("Multipart content processed");
                        }
                        if (debugEnabled)
                            ctx.servletLogger.debug("Composing response using builder: "
                                    +ctx.responseContext.getResponseBuilder().getResponseBuilderNode());
                        if (!isMultipart)
                            ctx.incomingDataListener = (DataReceiver) ctx.responseContext.getRequest();
                        Response result = ctx.responseContext.getResponse(ctx.user);
                        ctx.builderProcessedTs = System.currentTimeMillis();
                        if (debugEnabled)
                            ctx.servletLogger.debug(String.format(
                                    "Composed by %sms. Handling response (%s)", 
                                    System.currentTimeMillis()-ts, result));
                        processServiceResponse(ce, ctx, result);
                        ctx.writeProcessedTs = System.currentTimeMillis();
//                        ce.setTimeout(1);
                        if (debugEnabled)
                            ctx.servletLogger.debug("Handled", System.currentTimeMillis()-ts);
                    } catch (Throwable e) {
                        if (ctx.servletLogger.isErrorEnabled())
                            ctx.servletLogger.error("Response composing error", e);
                        try {
                            processError(ctx, e);
                        } catch (ServletException ex) {
                            ctx.processingException = ex;
                        } finally {
                            if (ctx.processingException==null)
                                ctx.response.flushBuffer();
                            ctx.writeProcessed();                            
                        }
                    }
                }
            });
        } catch (Throwable e) {
            processError(ctx, e);
        }
    }

    @Override
    protected FileItemIterator getMultipartItemIterator(HttpServletRequest request) throws Exception {
        RequestContext ctx = (RequestContext) request.getAttribute(REQUEST_CONTEXT);
        FileUploadContext fileUploadContext = new FileUploadContext(request);
        ctx.incomingDataListener = fileUploadContext.getCometInputStream();
        return new ServletFileUpload().getItemIterator(fileUploadContext);
    }

    private void processEndEvent(CometEvent ev) throws IOException, ServletException {
        RequestContext ctx = getRequestContext(ev);
        try {
            if (ctx!=null) {
                ctx.dataStreamClosed();
                ctx.closeChannel(ev);                
            }
        } finally {
            try {
//                ev.close();
            } catch (Throwable e) {
                if (ctx!=null)
                    ctx.servletLogger.warn("Read channel close error", e);
            }
//            ctx.
        }
    }

    private RequestContext getRequestContext(CometEvent ev) {
        final RequestContext ctx = (RequestContext) ev.getHttpServletRequest().getAttribute(REQUEST_CONTEXT);
        return ctx;
    }
    
    private void processServiceResponse(CometEvent ev, RequestContext ctx, Response serviceResponse) throws IOException {
        try {
            if (ctx.responseContext.getResponseBuilderLogger().isDebugEnabled())
                ctx.responseContext.getResponseBuilderLogger().debug("Builder returned response. Handling");
            if (serviceResponse != Response.ALREADY_COMPOSED && serviceResponse != Response.MANAGING_BY_BUILDER)
                super.processServiceResponse(ctx, serviceResponse);
        } finally {
            if (serviceResponse!=Response.MANAGING_BY_BUILDER)
                try {
                    try {
                        ev.setTimeout(1);
                    } finally {
                        ctx.writeProcessed();
                    }                    
                } catch (Throwable e) {
                    ctx.servletLogger.error("Write channel close error", e);
                }
        }
    }

    @Override
    protected Request createServiceRequest(RequestContext ctx, Map<String, Object> params, 
            Map<String, Object> headers, String context) 
    {
        return new CometRequestImpl(ctx.request.getRemoteAddr(), params, headers, context, 
                    ctx.request.getMethod().toUpperCase(), ctx.request);
    }

    private void processErrorEvent(CometEvent ce) throws IOException, ServletException {
        try {
            RequestContext ctx = getRequestContext(ce);
            Logger logger = ctx.responseContext.getResponseBuilderLogger();
            if (ce.getEventSubType()!=CometEvent.EventSubType.TIMEOUT || ctx.isWriteProcessed()) {
                if (ctx.servletLogger.isDebugEnabled())
                    ctx.servletLogger.debug("Write finished. Closing channel");
//                ce.close();
                ctx.closeChannel(ce);
            }
        } finally {
//            ce.close();
        }
    }

    private void processReadEvent(CometEvent ce) throws IOException {
        final RequestContext ctx = getRequestContext(ce);
        if (ctx.canPushBuffer()) {
            final InputStream stream = ce.getHttpServletRequest().getInputStream();
            final int size = stream.available();
            if (size>0) {
                ByteBuf buf = Unpooled.buffer(size);
                int written = buf.writeBytes(stream, size);
                ctx.pushBuffer(buf);
                if (ctx.servletLogger.isDebugEnabled())
                    ctx.servletLogger.debug("Written ({}) bytes to request stream consumer", written);
            }
        } else if (ctx.servletLogger.isDebugEnabled())
            ctx.servletLogger.debug("Request stream consumer is FULL. Wating...");
    }
}

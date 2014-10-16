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
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.CometEvent;
import org.apache.catalina.CometProcessor;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.raven.net.Request;
import org.raven.net.Response;
import org.raven.net.ResponsePromise;
import org.raven.net.ResponseReadyCallback;
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
//            final ExecutorService executor = ctx.responseService.getExecutor();
//            if (executor==null)
//                throw new ServletException("Comet servlet can't work without executor service");
            configureRequestContext(ctx);
            ctx.responseContext = new CometResponseContext(ctx);
            final Node builderNode = ctx.responseContext.getResponseBuilder().getResponseBuilderNode();
//            executor.execute(new AbstractTask(builderNode, "Processing http request") {                
//                @Override public void doRun() throws Exception {
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
                        Response result = ctx.responseContext.getResponse(ctx.user);
                        if (result instanceof ResponsePromise)
                            processResponsePromise(ce, ctx, (ResponsePromise)result);
                        else
                            processServiceResponse(ce, ctx, result);
                    } catch (Throwable e) {
                        processResponseError(ctx, e);
                    }
//                }
//
//            });
        } catch (Throwable e) {
            processError(ctx, e);
        }
    }

    @Override
    protected FileItemIterator getMultipartItemIterator(HttpServletRequest request) throws Exception {
        RequestContext ctx = (RequestContext) request.getAttribute(REQUEST_CONTEXT);
        FileUploadContext fileUploadContext = new FileUploadContext(request, ctx.getRequestStream());
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
    
    private void processResponseError(RequestContext ctx, Throwable e) {
        if (ctx.servletLogger.isErrorEnabled())
            ctx.servletLogger.error("Response composing error", e);
        try {
            processError(ctx, e);
        } finally {
            ctx.writeProcessed();                            
        }
    }
    
    private void processResponsePromise(final CometEvent ev, final RequestContext ctx, 
            final ResponsePromise responsePromise) 
    {
        responsePromise.onComplete(new ResponseReadyCallback() {
            public void onSuccess(Response response) {
                try {
                    processServiceResponse(ev, ctx, response);
                } catch (Throwable ex) {
                    processResponseError(ctx, ex);
                }
            }
            public void onError(Throwable e) {
                processResponseError(ctx, e);
            }
        });
    }
    
    private void processServiceResponse(CometEvent ev, RequestContext ctx, Response serviceResponse) 
            throws IOException 
    {
        try {
            final boolean debugEnabled = ctx.servletLogger.isDebugEnabled();
            ctx.responseGenerated();
            ctx.builderProcessedTs = System.currentTimeMillis();
            if (debugEnabled)
                ctx.servletLogger.debug(String.format("Composed by %sms. Handling response (%s)"
                        , System.currentTimeMillis()-ctx.createdTs, serviceResponse));
            
            if (ctx.responseContext.getResponseBuilderLogger().isDebugEnabled())
                ctx.responseContext.getResponseBuilderLogger().debug("Builder returned response. Handling");
            if (serviceResponse != Response.ALREADY_COMPOSED && serviceResponse != Response.MANAGING_BY_BUILDER) {
                super.processServiceResponse(ctx, serviceResponse);
                ctx.writeProcessed();
            } else if (serviceResponse==Response.MANAGING_BY_BUILDER)
                ctx.responseManagingByBuilder = true;
            
            ctx.writeProcessedTs = System.currentTimeMillis();
    //                        ce.setTimeout(1);
            if (debugEnabled)
                ctx.servletLogger.debug("Handled", System.currentTimeMillis()-ctx.createdTs);

//            if (serviceResponse!=Response.MANAGING_BY_BUILDER) {
//                try {
////                    ev.setTimeout(1);
//                } catch (Throwable e) {
//                    ctx.servletLogger.error("Write channel close error", e);
//                } finally {
//                    ctx.writeProcessed();
//                }
//            } else {
//                ctx.responseManagingByBuilder = true;
//            }
        } finally {
        }
    }

    @Override
    protected Request createServiceRequest(RequestContext ctx, Map<String, Object> params, 
            Map<String, Object> headers, String context) 
    {
        return new CometRequestImpl(ctx, params, headers, context);
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
        if (ctx.servletLogger.isDebugEnabled())
            ctx.servletLogger.debug("Request content length: {}", ctx.request.getContentLength());
        if (ctx.canPushBuffer()) {
            final InputStream stream = ce.getHttpServletRequest().getInputStream();
            final int size = stream.available();
            if (size>0) {
                ByteBuf buf = Unpooled.buffer(size);
                boolean eos = false;
                int data;
//                int written=0;
                while (stream.available()>0) {
                    try {
                        data = stream.read();
                        if (data<0) {
                            eos = true;
                            break;
                        } else {
                            buf.writeByte(data);
//                            ++written;
                        }
                    } catch (IOException e) {
                        eos = true;
                        if (!(e instanceof EOFException) && ctx.servletLogger.isErrorEnabled())
                            ctx.servletLogger.warn("Error while reading request content stream.");
                        break;
                    }

                }
                int written = buf.readableBytes();
                ctx.pushBuffer(buf);
                if (ctx.servletLogger.isDebugEnabled())
                    ctx.servletLogger.debug(String.format(
                            "Written (%s) bytes to request stream consumer. Total written: %s", 
                            written, ctx.getRedBytes()));
                if (eos || ctx.request.getContentLength()==ctx.getRedBytes()) {
                    if (ctx.servletLogger.isDebugEnabled())
                        ctx.servletLogger.debug("End of stream detected. Closing");
                    ctx.dataStreamClosed();
                }
            }
        } else if (ctx.servletLogger.isDebugEnabled())
            ctx.servletLogger.debug("Request stream consumer is FULL. Wating...");
    }
}

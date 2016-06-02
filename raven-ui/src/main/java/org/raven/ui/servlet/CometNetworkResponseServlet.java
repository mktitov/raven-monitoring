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

/**
 *
 * @author Mikhail Titov
 */
public class CometNetworkResponseServlet extends NetworkResponseServlet implements CometProcessor {
    public static final String REQUEST_CONTEXT = "RAVEN_REQUEST_CONTEXT";

    public void event(CometEvent ce) throws IOException, ServletException {
        final HttpServletRequest request = ce.getHttpServletRequest();
        final HttpServletResponse response = ce.getHttpServletResponse();
        final NetworkResponseServlet.RequestContext ctx = getRequestContext(ce);
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
                        if (ctx.servletLogger.isWarnEnabled())
                            ctx.servletLogger.warn("Builder finished response composing. Closing channel");
//                        ce.close();
                        ctx.tryToCloseChannel(ce);
                    }
                }
                break;
            case READ:
                processReadEvent(ce, ctx);
                break;
            case END:
                processEndEvent(ce, ctx);
                break;
            case ERROR:
                processErrorEvent(ce, ctx);
                break;
        }
    }
    
    private void initResponseProcessing(final CometEvent ce, final HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException
    {
        final NetworkResponseServlet.RequestContext ctx = createContext(request, response);        
//        request.setAttribute("org.apache.tomcat.comet.timeout", 5); //30 seconds	
        request.setAttribute("org.apache.tomcat.comet.timeout", 1000); //30 seconds	
        final boolean debugEnabled = ctx.servletLogger.isDebugEnabled();
        if (debugEnabled) 
            ctx.servletLogger.debug(String.format("Initializing request processing (%s event) (id: %s) (contentLength: %s) for URI: %s", 
                    ce.getEventType(), ce, request.getContentLength(), request.getRequestURI()));            
        request.setAttribute(REQUEST_CONTEXT, ctx);
        try {
            configureRequestContext(ctx);
            ctx.responseContext = new CometResponseContext(ctx, ce);
            final Node builderNode = ctx.responseContext.getResponseBuilder().getResponseBuilderNode();
            if (request.getContentLength()<=0)
                ctx.readProcessed(ce);
            if (request.getContentType()!=null || !"GET".equals(request.getMethod()) || request.getContentLength()>0)
            {
                final ExecutorService executor = ctx.responseService.getExecutor();
                if (executor==null)
                    throw new ServletException("Comet servlet can't work without executor service");
                executor.execute(new AbstractTask(builderNode, ctx.servletLogger.getPrefix()+"Processing http request") {
                    @Override public void doRun() throws Exception {
                        startReponseProcessing(ctx, request, debugEnabled, ce, true);                        
                    }
                });                
            } else 
                startReponseProcessing(ctx, request, debugEnabled, ce, false);
        } catch (Throwable e) {
            processError(ctx, e);
        }
    }

    public void startReponseProcessing(final NetworkResponseServlet.RequestContext ctx, final HttpServletRequest request, 
            final boolean debugEnabled, final CometEvent ce, final boolean notNioThread) 
    {
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
                processServiceResponse(ce, ctx, result, notNioThread);
        } catch (Throwable e) {
            processResponseError(ctx, e, ce);
        }
    }

    @Override
    protected FileItemIterator getMultipartItemIterator(HttpServletRequest request) throws Exception {
        NetworkResponseServlet.RequestContext ctx = (NetworkResponseServlet.RequestContext) request.getAttribute(REQUEST_CONTEXT);
        FileUploadContext fileUploadContext = new FileUploadContext(request, ctx.getRequestStream());
        return new ServletFileUpload().getItemIterator(fileUploadContext);
    }

    private void processEndEvent(final CometEvent ev, final NetworkResponseServlet.RequestContext ctx) throws IOException, ServletException {
//        RequestContext ctx = getRequestContext(ev);
//        try {
            if (ctx!=null) {
                ctx.dataStreamClosed();
//                IOUtils.closeQuietly(ev.getHttpServletRequest().getInputStream());
//                ctx.closeChannel(ev);
                
                //если клиент разрывает соединение, тогда tomcat шлет END event хотя должен слать ERROR/CLIENT_DISCONNECT.
                //а поскольку END event так же информирует о том что поток данных от клиента завершился,
                //то нельзя с уверенностью сказать что произошло. Так же известно, что request без content'а 
                //не генерирует END, соответственно при получении END для запроса без контента можно более или менее с
                //уверенностью сказать что это разрыв.
                if (ev.getHttpServletRequest().getContentLength()<=0) 
                    ctx.forceCloseChannel(ev);
                else {
                    ctx.readProcessed(ev);
                    ctx.writeResponseIfNeed(ev);
                    ctx.tryToCloseChannel(ev);
                }
            } else {
//                if (ctx.servletLogger.isDebugEnabled())
//                    ctx.servletLogger.debug("Not found RequestContext for END event (id: %s). Closing channel", ev);
//                ev.close();
            }
//        } finally {
//            try {
////                ev.close();
//            } catch (Throwable e) {
//                if (ctx!=null)
//                    ctx.servletLogger.warn("Read channel close error", e);
//            }
////            ctx.
//        }
    }

    private NetworkResponseServlet.RequestContext getRequestContext(CometEvent ev) {
        return (NetworkResponseServlet.RequestContext) ev.getHttpServletRequest().getAttribute(REQUEST_CONTEXT);
    }
    
    private void processResponseError(NetworkResponseServlet.RequestContext ctx, Throwable e, CometEvent ce) {
        if (ctx.servletLogger.isErrorEnabled())
            ctx.servletLogger.error("Response composing error", e);
        try {
            processError(ctx, e);
        } finally {
            ctx.writeProcessed(ce);                            
        }
    }
    
    private void processResponsePromise(final CometEvent ev, final NetworkResponseServlet.RequestContext ctx, 
            final ResponsePromise responsePromise) 
    {
        responsePromise.onComplete(new ResponseReadyCallback() {
            public void onSuccess(Response response) {
                try {
                    processServiceResponse(ev, ctx, response, true);
                } catch (Throwable ex) {
                    processResponseError(ctx, ex, ev);
                }
            }
            public void onError(Throwable e) {
                processResponseError(ctx, e, ev);
            }
        });
    }
    
    private void processServiceResponse(CometEvent ev, NetworkResponseServlet.RequestContext ctx, Response serviceResponse, boolean notNioThread) 
            throws IOException 
    {
        try {
            final boolean debugEnabled = ctx.servletLogger.isDebugEnabled();
            ctx.responseGenerated();
//            ctx.builderProcessedTs = System.currentTimeMillis();
            if (debugEnabled)
                ctx.servletLogger.debug(String.format("Composed by %sms. Handling response (%s)"
                        , System.currentTimeMillis()-ctx.createdTs, serviceResponse));
            
            if (ctx.responseContext.getResponseBuilderLogger().isDebugEnabled())
                ctx.responseContext.getResponseBuilderLogger().debug("Builder returned response. Handling");
            
            if (serviceResponse != Response.MANAGING_BY_BUILDER) {
                if (serviceResponse != Response.ALREADY_COMPOSED) {
//                    if (!notNioThread)
                        super.processServiceResponse(ctx, serviceResponse);
//                    else
//                        ctx.setServiceResponse(ev, serviceResponse, this);
                }
                ctx.writeProcessed(ev);
            } else if (serviceResponse==Response.MANAGING_BY_BUILDER)
                ctx.responseManagingByBuilder = true;
            
//            ctx.writeProcessedTs = System.currentTimeMillis();
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
    protected Request createServiceRequest(NetworkResponseServlet.RequestContext ctx, Map<String, Object> params, 
            Map<String, Object> headers, String context) 
    {
        return new CometRequestImpl(ctx, params, headers, context);
    }

    private void processErrorEvent(final CometEvent ce, final NetworkResponseServlet.RequestContext ctx) throws IOException, ServletException {
        try {
//            RequestContext ctx = getRequestContext(ce);
//            Logger logger = ctx.responseContext.getResponseBuilderLogger();            
            if (ce.getEventSubType()==CometEvent.EventSubType.TIMEOUT)
                ctx.tryToCloseChannel(ce);
            else
                ctx.forceCloseChannel(ce);
////            if (ce.getEventSubType()!=CometEvent.EventSubType.TIMEOUT || ctx.isWriteProcessed()) {
//                if (ctx.servletLogger.isDebugEnabled())
//                    ctx.servletLogger.debug("Write finished. Closing channel");
////                ce.close();
//                ctx.tryToCloseChannel(ce);
//            }
        } finally {
//            ce.close();
        }
    }

    private void processReadEvent(CometEvent ce, final NetworkResponseServlet.RequestContext ctx) throws IOException {
//        final RequestContext ctx = getRequestContext(ce);
        
        if (ctx.isDataStreamClosed()) {
            if (ctx.servletLogger.isDebugEnabled())
                ctx.servletLogger.debug("Request stream already PROCESSED. Ignoring READ event");
            return;
        }
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
                            ctx.servletLogger.warn("Error while reading request content stream.", e);
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
                        ctx.servletLogger.debug("End of stream detected. Closing request stream");
                    ctx.dataStreamClosed();
                    ctx.readProcessed(ce);
//                    IOUtils.closeQuietly(stream);
                }
            }
        } else if (ctx.servletLogger.isDebugEnabled())
            ctx.servletLogger.warn("Request stream consumer is FULL. Wating...");
    }
}

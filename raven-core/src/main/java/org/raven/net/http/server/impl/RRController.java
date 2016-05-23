/*
 * Copyright 2016 Mikhail Titov.
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
package org.raven.net.http.server.impl;

import groovy.lang.Writable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.ReferenceCounted;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.raven.auth.UserContext;
import org.raven.dp.FutureCallback;
import org.raven.dp.RavenFuture;
import org.raven.net.Outputable;
import org.raven.net.Response;
import org.raven.net.ResponseAdapter;
import org.raven.net.ResponseContext;
import org.raven.net.Result;
import org.raven.net.http.server.HttpConsts;
import org.raven.net.http.server.HttpServerContext;
import org.raven.net.http.server.HttpSession;
import org.raven.net.impl.RedirectResult;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.impl.LoggerHelper;
import org.weda.converter.TypeConverterException;
import org.weda.services.TypeConverter;

/**
 * Request/Response controller
 * @author Mikhail Titov
 */
public class RRController {    
    public final static DefaultLastHttpContent EMPTY_LAST_HTTP_CONTENT = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER);
    private final HttpServerContext serverContext;
    private final ChannelHandlerContext ctx;
    private final ResponseContext responseContext;
    private final HttpServerHandler.RequestImpl request;
    private final LoggerHelper logger;
    private final UserContext user;
    private final Cookie sessionCookie;
    private final boolean formUrlEncoded;
    private final AtomicBoolean released = new AtomicBoolean();
    private volatile boolean writeStarted = false;
    private volatile List<ReferenceCounted> resources;        
    private volatile boolean building;
    private volatile CompositeByteBuf formUrlParamsBuf;
    private volatile boolean keepAlive;

    public RRController(HttpServerContext serverContext,
            HttpServerHandler.RequestImpl request, ResponseContext responseContext, ChannelHandlerContext ctx, 
            UserContext user, LoggerHelper logger, Cookie sessionCookie, boolean keepAlive
    ) {
        this.serverContext = serverContext;
        this.request = request;
        this.ctx = ctx;
        this.user = user;
        this.logger = logger;
        this.responseContext = responseContext;
        this.formUrlEncoded = HttpConsts.FORM_URLENCODED_MIME_TYPE.equals(request.getContentType());
        this.sessionCookie = sessionCookie;
        this.keepAlive = keepAlive;
    }

    public boolean isWriteStarted() {
        return writeStarted;
    }
    
    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setWriteStarted(boolean writeStarted) {
        this.writeStarted = writeStarted;
    }
    
    public void start(boolean receivedFullRequest) throws InternalServerError {
        if (receivedFullRequest || formUrlEncoded)
            return; //wating for calling onRequestContent
        buildResponse(null); //creating response context with dynamic request content 
    }
    
    public void onRequestContent(final HttpContent chunk) throws Exception {
        if (chunk instanceof LastHttpContent) {
            if (!building)
                buildResponse((LastHttpContent) chunk); //creating response context adapter with static request content
            else 
                appendToRequestInputStream(chunk);
        } else {
            if (formUrlEncoded) {
                if (formUrlParamsBuf==null) 
                    formUrlParamsBuf = ctx.alloc().compositeBuffer();
                formUrlParamsBuf.addComponent(chunk.content());
            } else 
                appendToRequestInputStream(chunk);
        }
    }
    
    private void appendToRequestInputStream(final HttpContent chunk) throws Exception {
        if (request.getContent() instanceof AsyncInputStream) {
            AsyncInputStream stream = (AsyncInputStream) request.getContent();                    
            stream.onNext(chunk.content());
            if (chunk instanceof LastHttpContent)
                stream.onComplete();
        } else 
            throw new InternalServerError("The request input stream must be a type of AyncInputStream");        
    }
    
    private void buildResponse(final LastHttpContent chunk) throws InternalServerError {
        building = true;
        if (logger.isDebugEnabled())
            logger.debug("Initializing request processing");                    
        final ResponseWriter responseWriter = createAndAttachResponseAdapter();
        final boolean staticStream = createAndAttachRequestInputStream(chunk);
        
        //building response
        if (!staticStream || serverContext.getAlwaysExecuteBuilderInExecutor()) {
            boolean executed = serverContext.getExecutor().executeQuietly(
                    new AbstractTask(responseContext.getResponseBuilder().getResponseBuilderNode(), logger.getPrefix()+"Processing http request") {
                        @Override public void doRun() throws Exception {
                            startResponseProcessing(responseWriter);
                        }
                    }
            );
            if (!executed) {
                String msg = "Executor rejected response builder task";
                if (logger.isErrorEnabled())
                    logger.error(msg);
                throw new InternalServerError(msg);
            }
        } else
            startResponseProcessing(responseWriter);
    }

    private ResponseWriter createAndAttachResponseAdapter() {
        if (logger.isDebugEnabled())
            logger.debug("Creating response adapter (writer)");
        final ResponseWriter responseWriter = new ResponseWriter();
        responseContext.attachResponseAdapter(responseWriter);
        return responseWriter;
    }
    
    private void startResponseProcessing(final ResponseWriter responseWriter) {
        try {
            Response result = responseContext.getResponse(user);
            if (result instanceof RavenFuture) {
                //wating for future completing
                processFutureResponse((RavenFuture) result, responseWriter);
            }
            composeResponse(result, responseWriter);
        } catch (Throwable ex) {
            //TODO обрабатываем ошибку
            //1 Если запись в поток не началась можно сформировать html
            //2 Если запись в поток response началась, тогда можем только матюгнуться в лог и закрыть connection
            processError(ex);
        }        
    }
    
    private void processFutureResponse(RavenFuture responseFuture, final ResponseWriter responseWriter) {
        responseFuture.onComplete(new FutureCallback() {
            @Override public void onSuccess(Object result) {
                try {
                    composeResponse((Response) result, responseWriter);
                } catch(Throwable e) {
                    processError(e);
                }
            }
            @Override public void onError(Throwable error) {
                processError(error);
            }
            @Override public void onCanceled() {
                processError(new InternalServerError("Response generation canceled"));
            }
        });
    }
        
    private void processError(Throwable error) {
        if (logger.isErrorEnabled())
            logger.error("Error while processing request", error);
            //TODO обрабатываем ошибку
            //1 Если запись в поток не началась можно сформировать html
            //2 Если запись в поток response началась, тогда можем только матюгнуться в лог и закрыть connection
        
    }
    
    private void composeResponse(final Response response, final ResponseWriter responseWriter) throws Exception {
        if (response == Response.MANAGING_BY_BUILDER || response == Response.ALREADY_COMPOSED)
            return;        
        HttpResponse httpResponse = responseWriter.responseHeader;
        if (response == Response.NOT_MODIFIED) {
            httpResponse.setStatus(HttpResponseStatus.NOT_MODIFIED);
            responseWriter.close();
        } else {
            addHeadersToResponseWriter(response, responseWriter);
            Object content = response.getContent();
            if (content instanceof RedirectResult) 
                composeRedirectResponse(httpResponse, content, responseWriter);
            else {
                String contentType = response.getContentType();
                if (content instanceof Result) {
                    Result result = (Result) content;
                    responseWriter.setStatus(result.getStatusCode());
                    content = result.getContent();
                    if (result.getContentType()!=null)
                        contentType = result.getContentType();
                } else 
                    responseWriter.setStatus(content!=null? HttpServletResponse.SC_OK : HttpServletResponse.SC_NO_CONTENT);
                String charset = response.getCharset()==null? null : response.getCharset().name();
                responseWriter.setContentType(ContentType.create(contentType, charset).toString());
                if (response.getLastModified()!=null) 
                    httpResponse.headers().set(HttpHeaders.Names.LAST_MODIFIED, new Date(response.getLastModified()));
                httpResponse.headers().set(HttpHeaders.Names.CACHE_CONTROL, "no-cache");
                httpResponse.headers().add(HttpHeaders.Names.PRAGMA, "no-cache");
                if (content!=null) 
                    writeResponseContent(content, responseWriter, charset);
                responseWriter.close();
            }
        }
    }

    private void writeResponseContent(Object content, final ResponseWriter responseWriter, String charset) 
            throws TypeConverterException, IOException 
    {
        if (content instanceof Writable)
            ((Writable)content).writeTo(responseWriter.getWriter());
        else if (content instanceof Outputable)
            ((Outputable)content).outputTo(responseWriter.getStream());
        else {
            InputStream contentStream = serverContext.getTypeConverter().convert(InputStream.class, content, charset);
            if (contentStream!=null) {
                try {
                    IOUtils.copy(contentStream, responseWriter.getStream());
                } finally {
                    IOUtils.closeQuietly(contentStream);
                }
            }
        }
    }

    private void composeRedirectResponse(HttpResponse httpResponse, Object content, final ResponseWriter responseWriter) throws IOException {
        if (user.isNeedRelogin() && responseContext.getSession()!=null)
            responseContext.getSession().invalidate();
        httpResponse.setStatus(HttpResponseStatus.TEMPORARY_REDIRECT);
        httpResponse.headers().add(HttpHeaders.Names.LOCATION, ((RedirectResult)content).getContent().toString());
        responseWriter.close();
    }

    private void addHeadersToResponseWriter(final Response result, final ResponseWriter responseWriter) {
        Map<String, String> headers = result.getHeaders();
        if (headers != null)
            for (Map.Entry<String, String> e : headers.entrySet())
                responseWriter.addHeader(e.getKey(), e.getValue());
    }

    private boolean createAndAttachRequestInputStream(final LastHttpContent chunk) {
        //создаем request input stream
        InputStream requestStream;
        boolean staticStream = false;
        if (logger.isDebugEnabled())
            logger.debug("Creating request input stream. Received full request: "+(chunk!=null));
        if (chunk!=null) {
            staticStream = true;
            if (formUrlEncoded) {
                if (logger.isDebugEnabled())
                    logger.debug("Content-Type is application/x-www-form-urlencoded, so extracting parameters");
                //парсим и добавляем переметры
                String contentCharset = request.getContentCharset()==null? 
                        HttpConsts.DEFAULT_CONTENT_CHARSET : request.getContentCharset();
                ByteBuf buf = formUrlParamsBuf==null? 
                        chunk.content().retain() : formUrlParamsBuf.addComponent(chunk.content().retain());
                try {
                    String content = buf.toString(Charset.forName(contentCharset));
                    QueryStringDecoder decoder = new QueryStringDecoder(content);
                    for (Map.Entry<String, List<String>> param: decoder.parameters().entrySet()) 
                        request.getParams().put(param.getKey(), param.getValue());
                    requestStream = EmptyInputStream.INSTANCE;
                } finally {
                    buf.release();
                    formUrlParamsBuf = null;
                }
            } else {
                if (logger.isDebugEnabled())
                    logger.debug("Creating static request input stream");
                //создаем статический request stream                
                requestStream = new ByteBufInputStream(chunk.content());
                resources.add(chunk.content().retain());
            }
        } else {
            if (logger.isDebugEnabled())
                logger.debug("Creating async request input stream");
            //creating async input stream
            requestStream = new AsyncInputStream(serverContext.getRequestStreamBuffersCount());            
        }
        request.attachContentInputStream(requestStream);        
        return staticStream;
    }
    
    //free resources. For example reference counted. Must be called from the netty channel processing thread
    public void release() throws Exception {
        if (released.compareAndSet(false, true)) {
            if (resources!=null)
                for (ReferenceCounted resource: resources)
                    resource.release();
            if (request.getContent() instanceof AsyncInputStream) 
                ((AsyncInputStream)request.getContent()).forceComplete();
            if (formUrlParamsBuf!=null)
                formUrlParamsBuf.release();
        }
    }
    
    private void addResource(ReferenceCounted resource) {
        if (resources==null) 
            resources = new ArrayList<>();
        resources.add(resource);
    }
    
    private class ResponseWriter  implements ResponseAdapter {
//        private final Map<String, String> headers = new LinkedHashMap<>();
        private final AtomicBoolean headerWritten = new AtomicBoolean();
        private final DefaultHttpResponse responseHeader;
        private volatile Stream stream = new Stream();
        private volatile PrintWriter writer;

        public ResponseWriter() {
            this.responseHeader = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        }

        @Override
        public OutputStream getStream() {
            if (stream==null) {
                synchronized(this) {
                    if (stream==null)
                        stream = new Stream();                                
                }
            }
            return stream;
        }
        
        public void setContentType(String contentType) {
            responseHeader.headers().set(HttpHeaders.Names.CONTENT_TYPE, contentType);
        }
        
        public void setStatus(int code) {
            responseHeader.setStatus(HttpResponseStatus.valueOf(code));
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer==null) 
                synchronized(this) {
                    if (writer==null) {
                        //перед вызовом метода все header'ы должны быть добавлены
                        //определяем charset из contentType
                        String contentTypeStr = responseHeader.headers().get(HttpHeaders.Names.CONTENT_TYPE);
                        if (contentTypeStr==null) {
                            throw new IOException("Can't detect response content type charset beacause of "
                                    + "Content-Type header not defined");
                        }
                        String charset = null;
                        if (contentTypeStr!=null) 
                            charset = ContentType.parse(contentTypeStr).getCharset().name();
                        if (charset==null)
                            charset = HttpConsts.DEFAULT_CONTENT_CHARSET;
                        writer = new PrintWriter(new OutputStreamWriter(getStream(), charset));
                    }                        
                }
            return writer;
        }

        @Override
        public void close() throws IOException { 
            Stream _stream = stream;
            if (_stream!=null)
                _stream.close();
            else {
                writeResponseHeaderIfNeed();
                writeLastHttpMessage();
                ctx.flush();
            }
        }
        
        public void writeResponseHeaderIfNeed() {
            if (headerWritten.compareAndSet(false, true)) {
                //cookies encoding
                HttpSession session = responseContext.getSession();
                Cookie _sessionCookie = null;
                if (session==null || !session.isValid()) {
                    if (sessionCookie!=null && responseContext.isSessionAllowed()) {
                        //reseting sessionCookie
                        _sessionCookie = new DefaultCookie(HttpConsts.SESSIONID_COOKIE_NAME, "");
                        _sessionCookie.setMaxAge(0);
                    }
                } else {
                    if (sessionCookie==null || !session.getId().equals(sessionCookie.value())) {
                        //setting new session cookie
                        _sessionCookie = new DefaultCookie(HttpConsts.SESSIONID_COOKIE_NAME, session.getId());                        
                    }
                }
                if (_sessionCookie!=null) {
                    _sessionCookie.setPath(request.getProjectPath());
                    responseHeader.headers().add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.STRICT.encode(_sessionCookie));
                }
                ctx.write(new ResponseMessage(RRController.this, responseHeader));
            }
        }                

        @Override
        public void addHeader(String name, String value) {
            responseHeader.headers().add(name, value);
        }

        private void writeLastHttpMessage() {
            ctx.write(new ResponseMessage(RRController.this, EMPTY_LAST_HTTP_CONTENT));
        }
        
        private class Stream extends OutputStream {
            private final ByteBuf buf;

            public Stream() {
                buf = ctx.alloc().buffer(serverContext.getResponseStreamBufferSize());                
            }

            @Override
            public void write(byte[] b) throws IOException {
                buf.writeBytes(b);
                writeToChannelIfNeedAndCan(false);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                buf.writeBytes(b, off, len);
                writeToChannelIfNeedAndCan(false);
            }

            @Override
            public void write(int i) throws IOException {
                buf.writeByte(i);
                writeToChannelIfNeedAndCan(false);
            }

            @Override
            public void flush() throws IOException {
                writeToChannelIfNeedAndCan(true);
                ctx.flush();
            }

            @Override
            public void close() throws IOException {
                writeToChannelIfNeedAndCan(true);
                writeLastHttpMessage();
                ctx.flush();
                buf.release();
            }
            
            private void writeToChannelIfNeedAndCan(boolean forceWrite) {
                if (forceWrite || buf.readableBytes()>=serverContext.getResponseStreamBufferSize()) {
                    //forming and sending data chunk to the channel
                    writeResponseHeaderIfNeed();
                    if (buf.isReadable()) {
                        while (!ctx.channel().isWritable())  {
                            try {
                                Thread.sleep(10); //TODO Is it correct to do this at event loop thread??
                            } catch (InterruptedException ex) {
                                if (logger.isErrorEnabled())
                                    logger.error("Interrupted", ex);
                            }
                        }
                        ByteBuf bufForWrite = buf.retain().slice();
                        ctx.write(new ResponseMessage(RRController.this, new DefaultHttpContent(bufForWrite)));
                    }
                }
            }
        }         
    }    
}

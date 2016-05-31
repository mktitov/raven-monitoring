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
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.ReferenceCounted;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.activation.DataSource;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;
import org.raven.RavenUtils;
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
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.impl.LoggerHelper;
import org.weda.converter.TypeConverterException;

/**
 * Request/Response controller
 * @author Mikhail Titov
 */
public class RRController {    
    public final static DefaultLastHttpContent EMPTY_LAST_HTTP_CONTENT = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER);
    private final HttpServerContext serverContext;
    private final Channel channel;
    private final ResponseContext responseContext;
    private final HttpServerHandler.RequestImpl request;
    private final LoggerHelper logger;
    private final UserContext user;
    private final Cookie sessionCookie;
    private final boolean formUrlEncoded;
    
    //this fields must bre references only from event loop thread
    private boolean isMultipart = false;
    private List<ReferenceCounted> resources;        
    private HttpPostMultipartRequestDecoder multipartDecoder;
    private CompositeByteBuf formUrlParamsBuf;
    private boolean building;
    
    private final AtomicBoolean released = new AtomicBoolean();
    private volatile boolean writeStarted = false;
    private volatile ResponseWriter responseWriter;
    private volatile boolean keepAlive;
    private volatile long nextTimeout = 0l;

    public RRController(HttpServerContext serverContext,
            HttpServerHandler.RequestImpl request, ResponseContext responseContext, Channel channel, 
            UserContext user, LoggerHelper logger, Cookie sessionCookie, boolean keepAlive
    ) {
        this.serverContext = serverContext;
        this.request = request;
        this.channel = channel;
        this.user = user;
        this.logger = logger;
        this.responseContext = responseContext;
        this.formUrlEncoded = HttpConsts.FORM_URLENCODED_MIME_TYPE.equals(request.getContentType());
        this.sessionCookie = sessionCookie;
        this.keepAlive = keepAlive;
    }

    public HttpServerHandler.RequestImpl getRequest() {
        return request;
    }
    
    public boolean hasTimeout(long curTime) {
        return nextTimeout!=0l && curTime>nextTimeout;
    }
    
    public ResponseContext getResponseContext() {
        return responseContext;
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
    
    public void start(HttpRequest request) throws Exception {
        if (HttpPostRequestDecoder.isMultipart(request)) {
            isMultipart = true;
            multipartDecoder = new HttpPostMultipartRequestDecoder(
                    new RavenHttpDataFactory(serverContext.getUploadedFilesTempDir()), request); //mixed store 16k, utf-8            
        }
        if (request instanceof FullHttpRequest || formUrlEncoded || isMultipart)
            return; //wating for calling onRequestContent
        buildResponse(null); //creating response context with dynamic request content 
    }
    
    public void onRequestContent(final HttpContent chunk) throws Exception {
        if (chunk instanceof LastHttpContent) {
            if (logger.isDebugEnabled())
                logger.debug("Processing LastHttpContent");
            if (!building)
                buildResponse((LastHttpContent) chunk); //creating response context adapter with static request content
            else 
                appendToRequestInputStream(chunk);
        } else {
            if (logger.isDebugEnabled())
                logger.debug("Processing HttpContent");            
            if (formUrlEncoded) {
                if (logger.isDebugEnabled())
                    logger.debug("Collecting HttpContent for later decoding (application/x-www-form-urlencoded)");
                if (formUrlParamsBuf==null) 
                    formUrlParamsBuf = channel.alloc().compositeBuffer();
                formUrlParamsBuf.addComponent(chunk.content());
            } if (isMultipart) {
                multipartDecoder.offer(chunk);
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
    
    private void buildResponse(final LastHttpContent chunk) throws Exception {
        building = true;
        if (logger.isDebugEnabled())
            logger.debug("Initializing request processing");                    
        final ResponseWriter responseWriter = createAndAttachResponseAdapter();
        this.responseWriter = responseWriter;
        final boolean staticStream = createAndAttachRequestInputStream(chunk);
        
        //building response
        Long _timeout = responseContext.getResponseBuilder().getBuildTimeout();
        nextTimeout = _timeout==null? serverContext.getDefaultResponseBuildTimeout() : _timeout;
        if (!staticStream || serverContext.getAlwaysExecuteBuilderInExecutor()) {
            if (logger.isDebugEnabled())
                logger.debug("Response will be built in executor ({})", serverContext.getExecutor().getPath());
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
        } else {
            if (logger.isDebugEnabled()) 
                logger.debug("Response will be built in Netty thread");
            startResponseProcessing(responseWriter);
        }
    }

    private ResponseWriter createAndAttachResponseAdapter() {
        if (logger.isDebugEnabled())
            logger.debug("Creating response adapter (writer)");
        final ResponseWriter responseWriter = new ResponseWriter(logger);
        responseContext.attachResponseAdapter(responseWriter);
        return responseWriter;
    }
    
    private void startResponseProcessing(final ResponseWriter responseWriter) {
        try {
            if (logger.isDebugEnabled())
                logger.debug("Composing response using builder: "+responseContext.getResponseBuilder().getResponseBuilderNode().getPath());
            Response result = responseContext.getResponse(user);
            if (result instanceof RavenFuture) {
                if (logger.isDebugEnabled())
                    logger.debug("Response builder returned the future. Wating for completion");
                //wating for future completion
                processFutureResponse((RavenFuture) result, responseWriter);
            } else {
                composeResponse(result, responseWriter);
            }
        } catch (Throwable ex) {
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
        //we must to deliver the error to the server handler
        channel.write(new ErrorResponseMessage(this, error));
    }
    
    private void composeResponse(final Response response, final ResponseWriter responseWriter) throws Exception {
        if (logger.isDebugEnabled())
            logger.debug("Response created by builder. Forming HTTP Response");        
        if (response == Response.MANAGING_BY_BUILDER)
            return;        
        if (response == Response.ALREADY_COMPOSED) {
            responseWriter.close(); //just in case
            return;
        }
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
                    responseWriter.setStatus(content!=null? 
                            HttpServletResponse.SC_OK : HttpServletResponse.SC_NO_CONTENT);
                String charset = response.getCharset()==null? null : response.getCharset().name();
                responseWriter.setContentType(ContentType.create(contentType, charset).toString());
                if (response.getLastModified()!=null) 
                    httpResponse.headers().set(HttpHeaders.Names.LAST_MODIFIED, new Date(response.getLastModified()));
                httpResponse.headers().set(HttpHeaders.Names.CACHE_CONTROL, "no-cache");
                httpResponse.headers().add(HttpHeaders.Names.PRAGMA, "no-cache");
                if (logger.isTraceEnabled())
                    logger.trace("Response content: "+content);
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

    private boolean createAndAttachRequestInputStream(final LastHttpContent chunk) throws Exception {
        //создаем request input stream
        InputStream requestStream;
        boolean staticStream = false;
        if (logger.isDebugEnabled())
            logger.debug("Creating request input stream. Received full request: "+(chunk!=null));
        if (chunk!=null) {
            staticStream = true;
            if (formUrlEncoded) {
                requestStream = decodeFormUrl(chunk);
            } else if (isMultipart) {
                requestStream = decodeMultipart(chunk);
            } else {
                if (logger.isDebugEnabled())
                    logger.debug("Creating static request input stream");
                //создаем статический request stream                
                requestStream = new ByteBufInputStream(chunk.content());
                addResource(chunk.content().retain());
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
    
    private InputStream decodeMultipart(final LastHttpContent chunk) throws Exception {
        if (logger.isDebugEnabled())
            logger.debug("Content-Type is multipart/form-data, so extracting parameters/files");
        try {
            multipartDecoder.offer(chunk);
            List<InterfaceHttpData> dataList = multipartDecoder.getBodyHttpDatas();
            if (dataList!=null) {
                Map<String, List> allParams = new HashMap<>();
                for (InterfaceHttpData data: dataList) {
                    switch(data.getHttpDataType()) {
                        case Attribute:
                            addParam(allParams, data.getName(), ((Attribute)data).getValue());
                            break;
                        case FileUpload:
                            FileUpload fileParam = (FileUpload) data;
                            String key = RavenUtils.generateUniqKey("httserver_file_upload");                            
                            File tempFile = serverContext.getTempFileManager().createFile(
                                    serverContext.getOwner(), key, fileParam.getContentType(), 
                                    fileParam.getName());
                            if (!fileParam.renameTo(tempFile))
                                throw new InternalServerError(String.format(
                                        "Error while storing uploaded file (param name: %s, filename: %s)",
                                        fileParam.getName(), fileParam.getFilename()));
                            DataSource fileSource = serverContext.getTempFileManager().getDataSource(key);
                            addParam(allParams, fileParam.getName(), fileSource);
                            break;
                        default:
                            throw new InternalServerError(String.format(
                                    "Invalid multipart attribute type (%s) for param (%s)",
                                    data.getHttpDataType(), data.getName()));
                    }
                }
                request.getAllParams().putAll(allParams);
            }
        } finally {
            multipartDecoder.destroy();
            multipartDecoder = null;
        }
        return EmptyInputStream.INSTANCE;
    }
    
    private void addParam(Map<String, List> allParams, String name, Object value) {
        request.getParams().put(name, value);
        List values = allParams.get(name);
        if (values==null) {
            values = new ArrayList(1);
            allParams.put(name, values);
        }
        values.add(value);
    }

    private InputStream decodeFormUrl(final LastHttpContent chunk) {
        if (logger.isDebugEnabled())
            logger.debug("Content-Type is application/x-www-form-urlencoded, so extracting parameters");
        //парсим и добавляем переметры
        String contentCharset = request.getContentCharset()==null?
                HttpConsts.DEFAULT_CONTENT_CHARSET : request.getContentCharset();
        ByteBuf buf = formUrlParamsBuf==null?
                chunk.content().retain() : formUrlParamsBuf.addComponent(chunk.content().retain());
        try {
            String content = buf.toString(Charset.forName(contentCharset));
            if (logger.isTraceEnabled())
                logger.trace("FormUrl encoded params: "+content);
            QueryStringDecoder decoder = new QueryStringDecoder(content, false);
            for (Map.Entry<String, List<String>> param: decoder.parameters().entrySet()) {
                List<String> values = param.getValue();
                if (values!=null && !values.isEmpty()) {
                    request.getParams().put(param.getKey(), values.get(0));
                    request.getAllParams().put(param.getKey(), new ArrayList(values));
                } else {
                    request.getParams().put(param.getKey(), null);
                    request.getAllParams().put(param.getKey(), new ArrayList(1));
                }
            }
        } finally {
            buf.release();
            formUrlParamsBuf = null;
        }
        return EmptyInputStream.INSTANCE;
    }
    
    //free resources. For example reference counted. Must be called from the netty channel processing thread
    public void release() throws Exception {
        if (logger.isDebugEnabled())
            logger.debug("Releasing resources");
        if (released.compareAndSet(false, true)) {
            ResponseWriter _responseWriter = responseWriter;
            if (_responseWriter!=null)
                _responseWriter.release();
            if (resources!=null)
                for (ReferenceCounted resource: resources)
                    resource.release();
            if (request.getContent() instanceof AsyncInputStream) 
                ((AsyncInputStream)request.getContent()).forceComplete();
            if (formUrlParamsBuf!=null)
                formUrlParamsBuf.release();
            if (multipartDecoder!=null) {
                multipartDecoder.destroy();
                multipartDecoder = null;
            }
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
        private final AtomicBoolean closed = new AtomicBoolean();
        private final DefaultHttpResponse responseHeader;
        private final LoggerHelper logger;        
        private volatile Stream stream = new Stream();
        private volatile PrintWriter writer;

        public ResponseWriter(LoggerHelper logger) {
            this.responseHeader = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            responseHeader.headers().set(HttpHeaders.Names.SERVER, HttpConsts.HTTP_SERVER_HEADER);
            this.logger = new LoggerHelper(logger, "ChannelWriter. ");
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
                        if (contentTypeStr==null) 
                            throw new IOException("Can't detect response content type charset beacause of "
                                    + "Content-Type header not defined");
                        writer = new PrintWriter(new OutputStreamWriter(getStream(), getContentCharset(contentTypeStr)));
                    }                        
                }
            return writer;
        }

        private String getContentCharset(String contentTypeStr) throws ParseException, UnsupportedCharsetException {
            String charset = null;
            if (contentTypeStr!=null) {
                Charset contentCharset = ContentType.parse(contentTypeStr).getCharset();
                if (contentCharset!=null)
                    charset = contentCharset.name();
            }
            if (charset==null)
                charset = HttpConsts.DEFAULT_CONTENT_CHARSET;
            return charset;
        }

        @Override
        public void close() throws IOException { 
            if (closed.compareAndSet(false, true)) {
                nextTimeout = 0l;
                if (logger.isDebugEnabled())
                    logger.debug("Closing");
                Stream _stream = stream;
                if (_stream!=null)
                    if (writer!=null)
                        writer.close();
                    else
                        _stream.close();
                else {
                    writeResponseHeaderIfNeed();
                    writeLastHttpMessage();
                    channel.flush();
                }
            }
        }
        
        public void release() {
            if (closed.compareAndSet(false, true)) {
                Stream _stream = stream;
                if (_stream!=null)
                    _stream.release();
            }
        }
        
        public void writeResponseHeaderIfNeed() {
            if (headerWritten.compareAndSet(false, true)) {
                logger.debug("Writing response headers to channel");
                HttpHeaders headers = responseHeader.headers();
                //cookies encoding
                if (!headers.contains(HttpHeaders.Names.CONTENT_LENGTH)) 
                    headers.set(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
                if (!headers.contains(HttpHeaders.Names.CONNECTION))
                    headers.set(HttpHeaders.Names.CONNECTION, keepAlive? HttpHeaders.Values.KEEP_ALIVE : HttpHeaders.Values.CLOSE);
                else 
                    keepAlive = HttpHeaders.Values.KEEP_ALIVE.equals(
                            headers.get(HttpHeaders.Names.TRANSFER_ENCODING).trim().toLowerCase());
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
                channel.write(new HttpObjectResponseMessage(RRController.this, responseHeader));
            }
        }                

        @Override
        public void addHeader(String name, String value) {
            responseHeader.headers().add(name, value);
        }

        private void writeLastHttpMessage() {
            channel.write(new HttpObjectResponseMessage(RRController.this, EMPTY_LAST_HTTP_CONTENT));
        }
        
        private class Stream extends OutputStream {
            private final ByteBuf buf;
            private final AtomicBoolean writeStarted = new AtomicBoolean();
            private final AtomicBoolean closed = new AtomicBoolean();

            public Stream() {
                buf = channel.alloc().buffer(serverContext.getResponseStreamBufferSize());                
            }
            
            private void ensureWrite() throws IOException {
//                if (closed.get() || ResponseWriter.this.closed.get())
                if (closed.get())
                    throw new IOException("Attempt to write to closed channel");
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
                writeToChannelIfNeedAndCan(false, false);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                ensureWrite();
                do {
                    int _len = Math.min(len, buf.writableBytes());
                    buf.writeBytes(b, off, _len);
                    len -= _len;
                    off += _len;
                    writeToChannelIfNeedAndCan(false, false);
                } while (len>0);
            }

            @Override
            public void write(int i) throws IOException {
                ensureWrite();
                buf.writeByte(i);
                writeToChannelIfNeedAndCan(false, false);
            }

            @Override
            public void flush() throws IOException {
                ensureWrite();
                if (logger.isDebugEnabled())
                    logger.debug("Flushing...");
                writeToChannelIfNeedAndCan(true, false);
                channel.flush();
            }

            @Override
            public void close() throws IOException {                
                if (closed.compareAndSet(false, true)) {
                    if (logger.isDebugEnabled())
                        logger.debug("Closing response stream");
                    writeToChannelIfNeedAndCan(true, true);
                    channel.flush();
                    buf.release();
                } else 
                    ensureWrite();
            }
            
            public void release() {
                if (closed.compareAndSet(false, true)) {
                    buf.release();
                }
            }
            
            private void writeToChannelIfNeedAndCan(boolean forceWrite, boolean lastWrite) {
                if (forceWrite || buf.readableBytes()>=serverContext.getResponseStreamBufferSize()) {
                    //forming and sending data chunk to the channel
                    if (!headerWritten.get() && lastWrite && writeStarted.compareAndSet(false, true)) 
                        responseHeader.headers().set(HttpHeaders.Names.CONTENT_LENGTH, buf.readableBytes());
                    writeResponseHeaderIfNeed();
                    if (buf.isReadable()) {
                        int bytesToRead = buf.readableBytes();
                        if (logger.isDebugEnabled())
                            logger.debug("Have ({}) bytes ready to write. Writing...", bytesToRead);
                                    
                        while (!channel.isWritable())  {
                            try {
                                Thread.sleep(10); //TODO Is it correct to do this at netty event loop thread??
                            } catch (InterruptedException ex) {
                                if (logger.isErrorEnabled())
                                    logger.error("Interrupted", ex);
                            }
                        }
                        ByteBuf bufForWrite = buf.readBytes(bytesToRead);
                        buf.discardSomeReadBytes();
                        HttpContent chunk = lastWrite? new DefaultLastHttpContent(bufForWrite) : new DefaultHttpContent(bufForWrite);
                        channel.write(new HttpObjectResponseMessage(RRController.this, chunk));
                    } else if (logger.isDebugEnabled()) 
                        logger.debug("Nothing to write");
                }
            }
        }         
    }    
}

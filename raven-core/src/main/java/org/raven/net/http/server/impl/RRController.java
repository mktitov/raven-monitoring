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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.ReferenceCounted;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.http.entity.ContentType;
import org.apache.tika.metadata.HttpHeaders;
import org.raven.net.ResponseAdapter;
import org.raven.net.ResponseContext;
import org.raven.net.http.server.HttpConsts;
import org.raven.net.http.server.HttpServerContext;
import org.raven.sched.ExecutorService;
import org.raven.tree.impl.LoggerHelper;

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
    private final boolean formUrlEncoded;
    private volatile List<ReferenceCounted> resources;        
    private volatile boolean building;

    public RRController(HttpServerContext serverContext,
            HttpServerHandler.RequestImpl request, ResponseContext responseContext, ChannelHandlerContext ctx, 
            ExecutorService executor, LoggerHelper logger
    ) {
        this.serverContext = serverContext;
        this.request = request;
        this.ctx = ctx;
        this.logger = logger;
        this.responseContext = responseContext;
        this.formUrlEncoded = HttpConsts.FORM_URLENCODED_MIME_TYPE.equals(request.getContentType());
    }
    
    public void start(boolean receivedFullRequest) {
        if (receivedFullRequest || formUrlEncoded)
            return; //wating for calling onRequestContent
        buildResponse(null); //создаем динамический адаптер 
        
    }
    
    public void onRequestContent(final HttpContent chunk) {
        if (chunk instanceof LastHttpContent) {
            if (!building)
                buildResponse(null); //создаем адаптер со статическим контентом
        } else {
            
        }
    }
    
    private void buildResponse(final LastHttpContent chunk) {
        building = true;
        final ResponseWriter responseAdapter = new ResponseWriter();
        //создаем response input stream
        if (chunk!=null) {
            if (formUrlEncoded) {
                //парсим и добавляем переметры
                String contentCharset = request.getContentCharset()==null? 
                        HttpConsts.DEFAULT_CONTENT_CHARSET : request.getContentCharset();
                String content = chunk.content().toString(Charset.forName(contentCharset));
                QueryStringDecoder decoder = new QueryStringDecoder(content);
                for (Map.Entry<String, List<String>> param: decoder.parameters().entrySet()) 
                    request.getParams().put(param.getKey(), param.getValue());
                request.attachContentInputStream(EmptyInputStream.INSTANCE);
            } else {
                //создаем статический request stream                
                final ByteBufInputStream requestStream = new ByteBufInputStream(chunk.content());
                resources.add(chunk.content().retain());
                request.attachContentInputStream(requestStream);
            }
        } else {
            //creating async input stream
        }
        //building response
    }
    
    private void tryBuildResponse() {        
        //Response можем начать билдить если
        //1. Если прочитаны все параметры учитывая и те что находятся в content:
        //   contentType==application/x-www-form-urlencoded
        //   contentType==multipart/
        //2. 
    }
    
    //free resources. For example reference counted
    public void release() {
        if (resources!=null)
            for (ReferenceCounted resource: resources)
                resource.release();
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

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer==null) 
                synchronized(this) {
                    if (writer==null) {
                        //перед вызовом метода все header'ы должны быть добавлены
                        //определяем charset из contentType
                        String contentTypeStr = responseHeader.headers().get(HttpHeaders.CONTENT_TYPE);
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

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
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
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
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.http.entity.ContentType;
import org.apache.tika.metadata.HttpHeaders;
import org.raven.net.ResponseAdapter;
import org.raven.net.ResponseContext;
import org.raven.net.http.server.HttpConsts;
import org.raven.net.http.server.HttpServerContext;
import org.raven.sched.ExecutorService;

/**
 * Request/Response controller
 * @author Mikhail Titov
 */
public class RRController {    
    private final HttpServerContext serverContext;
    private final ChannelHandlerContext ctx;
    private final ResponseContext responseContext;
    private final HttpServerHandler.RequestImpl request;
    private final boolean formUrlEncoded;
    private volatile List<ReferenceCounted> resources;        
    private volatile boolean building;

    public RRController(HttpServerContext serverContext,
            HttpServerHandler.RequestImpl request, ResponseContext responseContext, ChannelHandlerContext ctx, 
            ExecutorService executor
    ) {
        this.serverContext = serverContext;
        this.request = request;
        this.ctx = ctx;
        this.responseContext = responseContext;
        this.formUrlEncoded = HttpConsts.FORM_URLENCODED_MIME_TYPE.equals(request.getContentType());
    }
    
    public void start(boolean receivedFullRequest) {
        if (receivedFullRequest || formUrlEncoded)
            return; //wating for calling onRequestContent
        buildResponse(null); //������� ������������ ������� 
        
    }
    
    public void onRequestContent(final HttpContent chunk) {
        if (chunk instanceof LastHttpContent) {
            if (!building)
                buildResponse(null); //������� ������� �� ����������� ���������
        } else {
            
        }
    }
    
    private void buildResponse(final LastHttpContent chunk) {
        building = true;
        final ResponseAdapter responseAdapter = new ResponseWriter();
        //������� response input stream
        if (chunk!=null) {
            final ByteBufInputStream requestStream = new ByteBufInputStream(chunk.content());
            resources.add(chunk.content().retain());
            if (formUrlEncoded) {
                //������ � ��������� ���������
                String contentCharset = request.getContentCharset()==null? 
                        HttpConsts.DEFAULT_CONTENT_CHARSET : request.getContentCharset();
                String content = chunk.content().toString(Charset.forName(contentCharset));
                QueryStringDecoder decoder = new QueryStringDecoder(content);
                request.attachContentInputStream(EmptyInputStream.INSTANCE);
            } else {
                request.attachContentInputStream(requestStream);
                //������� ����������� request stream
                
            }
        }
        
    }
    
    private void tryBuildResponse() {        
        //Response ����� ������ ������� ����
        //1. ���� ��������� ��� ��������� �������� � �� ��� ��������� � content:
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
        private volatile PrintWriter writer;

        public ResponseWriter() {
            this.responseHeader = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        }

        @Override
        public OutputStream getStream() {
            return this;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer==null) 
                synchronized(this) {
                    if (writer==null) {
                        //����� ������� ������ ��� header'� ������ ���� ���������
                        //���������� charset �� contentType
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
        public void close() { //close ���� � � ResponseAdapter � OutputStream. �����??
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
        private void writeResponseHeader() {
            
        }

        @Override
        public void addHeader(String name, String value) {
            responseHeader.headers().add(name, value);
        }
        
        private class Stream extends OutputStream {
            private final ByteBuf buf;

            public Stream() {
                buf = ctx.alloc().buffer(serverContext.getResponseStreamBufferSize());                
            }

            @Override
            public void write(int i) throws IOException {
                buf.writeByte(i);
                writeToChannelIfNeed(false);
            }

            @Override
            public void flush() throws IOException {
                super.flush();
                writeToChannelIfNeed(true);
                ctx.flush();
            }

            @Override
            public void close() throws IOException {
                super.close();
                flush();
                buf.release();
            }
            
            private void writeToChannelIfNeed(boolean forceWrite) {
                if (forceWrite || buf.readableBytes()>=serverContext.getResponseStreamBufferSize()) {
                    //��������� ���������� chunk � �����
                    //���� http header �� �����������, ����������?
                    if (headerWritten.compareAndSet(false, true)) {
                        ctx.write(new ResponseMessage(RRController.this, responseHeader));
                    }
                    if (buf.isReadable()) {
                        ByteBuf bufForWrite = buf.retain().slice();
                        ctx.write(new ResponseMessage(RRController.this, new DefaultHttpContent(bufForWrite)));
                    }
                }
            }
        } 
        
    }
    
    private class ResponseStream extends OutputStream {
        public final AtomicBoolean headersWritten = new AtomicBoolean();

        @Override
        public void write(int i) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void flush() throws IOException {
            super.flush();
        }

        @Override
        public void close() throws IOException {
            super.close();
        }        
    }
}

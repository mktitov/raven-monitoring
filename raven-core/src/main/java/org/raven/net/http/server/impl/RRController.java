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

import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.ReferenceCounted;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.raven.net.ResponseAdapter;
import org.raven.net.ResponseContext;
import org.raven.net.http.server.HttpConsts;
import org.raven.sched.ExecutorService;

/**
 * Request/Response controller
 * @author Mikhail Titov
 */
public class RRController {
    private final ChannelHandlerContext ctx;
    private final ResponseContext responseContext;
    private final ExecutorService executor;
    private final HttpServerHandler.RequestImpl request;
    private final boolean formUrlEncoded;
    private volatile List<ReferenceCounted> resources;        
    private volatile boolean building;

    public RRController(HttpServerHandler.RequestImpl request, ResponseContext responseContext, ChannelHandlerContext ctx, 
            ExecutorService executor
    ) {
        this.request = request;
        this.ctx = ctx;
        this.responseContext = responseContext;
        this.executor = executor;
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
        final ResponseAdapter responseAdapter = new ResponseAdapterImpl();
        //создаем response input stream
        if (chunk!=null) {
            final ByteBufInputStream requestStream = new ByteBufInputStream(chunk.content());
            resources.add(chunk.content().retain());
            if (formUrlEncoded) {
                //парсим и добавляем переметры
                String contentCharset = request.getContentCharset()==null? 
                        HttpConsts.DEFAULT_CONTENT_CHARSET : request.getContentCharset();
                String content = chunk.content().toString(Charset.forName(contentCharset));
                QueryStringDecoder decoder = new QueryStringDecoder(content);
                request.attachContentInputStream(EmptyInputStream.INSTANCE);
            } else {
                request.attachContentInputStream(requestStream);
                //создаем статический request stream
                
            }
        }
        
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
    
    private class ResponseAdapterImpl implements ResponseAdapter {

        @Override
        public OutputStream getStream() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public PrintWriter getWriter() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addHeader(String name, String value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
}

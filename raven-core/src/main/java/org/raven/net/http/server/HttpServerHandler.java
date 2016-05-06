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
package org.raven.net.http.server;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.ReferenceCountUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.raven.net.NetworkResponseService;
import org.raven.net.impl.RequestImpl;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class HttpServerHandler extends ChannelDuplexHandler {
    private final ExecutorService executor;
    private final Node owner;
    private final AtomicLong connectionCounter;
    private final AtomicLong requestCounter;
    private final NetworkResponseService responseService;
    
    private LoggerHelper logger;
    private RRController rrController;

    public HttpServerHandler(ExecutorService executor, Node owner, AtomicLong connectionCounter, AtomicLong requestCounter, 
            NetworkResponseService responseService) 
    {
        this.executor = executor;
        this.owner = owner;
        this.connectionCounter = connectionCounter;
        this.requestCounter = requestCounter;
        this.responseService = responseService;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        long connectionNum = connectionCounter.incrementAndGet();
        logger = new LoggerHelper(owner, "(+"+connectionNum+") " + ctx.channel().remoteAddress().toString()+" ");
        if (logger.isDebugEnabled())
            logger.debug("New connection established");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (logger.isDebugEnabled())
            logger.debug("Connection closed");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (logger.isErrorEnabled())
            logger.error("Error catched", cause);
        if (rrController!=null)
            rrController.release();
        rrController = null;
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof HttpRequest) {
                processHttpRequest((HttpRequest) msg, ctx);
            }
            if (msg instanceof HttpContent) {
                processHttpRequestContent((HttpContent) msg);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    }
    
    private void processHttpRequest(HttpRequest request, ChannelHandlerContext ctx) {
        final long requestNum = requestCounter.incrementAndGet();
        if (logger.isDebugEnabled())
            logger.debug("Received new request #"+requestNum);
        //экстрактим параметры
        Map<String, Object> params = decodeParams(request);
        //экстрактим заголовки
        Map<String, Object> headers = decodeHeaders(request);
        //нужно создать Raven Request
        
        //нужно создать ResponseContext
        
        //проверяем аутентификацию
        
        //добавляем запись в audit
        
        //создаем RRController
    }
    
    private RequestImpl
    
    private static Map<String, Object> decodeParams(final HttpRequest request) {
        final Map<String, List<String>> params = new QueryStringDecoder(request.getUri()).parameters();
        final Map<String, Object> requestParams = new HashMap<>();
        if (params!=null && !params.isEmpty()) {
            List<String> vals;
            for (Map.Entry<String, List<String>> param: params.entrySet()) {            
                vals = param.getValue();
                if (vals!=null && !vals.isEmpty()) 
                    requestParams.put(param.getKey(), vals.get(0)); //берем только первой значение параметра, хоть это не совсем корректно
            }
        }
        return requestParams;
    }
    
    private static Map<String, Object> decodeHeaders(final HttpRequest request) {
        final HttpHeaders httpHeaders = request.headers();
        Map<String, Object> headers = new HashMap<>();
        if (!httpHeaders.isEmpty())
            for (Map.Entry<String, String> header: httpHeaders)
                headers.put(header.getKey(), header.getValue());
        return headers;
    }

    private void processHttpRequestContent(HttpContent content) {
        
        if (content instanceof LastHttpContent) {
            
        }
    }
}

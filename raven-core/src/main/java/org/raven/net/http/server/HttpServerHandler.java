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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import java.util.concurrent.atomic.AtomicLong;
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
    private LoggerHelper logger;
    private RRController rrController;

    public HttpServerHandler(ExecutorService executor, Node owner, AtomicLong connectionCounter, AtomicLong requestCounter) {
        this.executor = executor;
        this.owner = owner;
        this.connectionCounter = connectionCounter;
        this.requestCounter = requestCounter;
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
        
    }

    private void processHttpRequestContent(HttpContent content) {
        
        if (content instanceof LastHttpContent) {
            
        }
    }
}

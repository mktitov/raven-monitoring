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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.raven.net.NetworkResponseService;
import org.raven.net.Request;
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
    private InetSocketAddress remoteAddr;
    private InetSocketAddress localAddr;

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
        remoteAddr = (InetSocketAddress) ctx.channel().remoteAddress();
        localAddr = (InetSocketAddress) ctx.channel().localAddress();
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
    
    private void processHttpRequest(HttpRequest request, ChannelHandlerContext ctx) throws Exception {
        final long requestNum = requestCounter.incrementAndGet();
        if (logger.isDebugEnabled())
            logger.debug("Received new request #"+requestNum);        
        
        final String contentTypeStr = request.headers().get(HttpHeaders.Names.CONTENT_TYPE);
        final ContentType contentType = contentTypeStr==null || contentTypeStr.isEmpty()? null : ContentType.parse(contentTypeStr);
        //экстрактим параметры
        QueryStringDecoder queryString = new QueryStringDecoder(request.getUri());
        Map<String, Object> params = new HashMap<>();
        addQueryStringParams(params, queryString.parameters());
        //экстрактим заголовки
        Map<String, Object> headers = decodeHeaders(request);
        //нужно создать Raven Request
        RequestImpl ravenRequest = new RequestImpl(request, localAddr, remoteAddr, params, headers, contentTypeStr, contentType);
        
        //нужно создать ResponseContext
        
        //проверяем аутентификацию
        
        //добавляем запись в audit
        
        //создаем RRController
    }
        
    private static Map<String, Object> addQueryStringParams(Map<String, Object> requestParams, Map<String, List<String>> queryStringParams) {
        if (queryStringParams!=null && !queryStringParams.isEmpty()) {
            List<String> vals;
            for (Map.Entry<String, List<String>> param: queryStringParams.entrySet()) {            
                vals = param.getValue();
                if (vals!=null && !vals.isEmpty()) 
                    requestParams.put(param.getKey(), vals.get(0)); 
            }
        }
        return requestParams;
    }
    
//    private static Map<String, Object> addUrl
    
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
    
    private static class RequestImpl implements Request {
        private final InetSocketAddress remoteAddr;
        private final InetSocketAddress localAddr;
        private final Map<String, Object> params;
        private final Map<String, Object> headers;
        private final String servicePath;
        private final String contextPath;
        private final String method;
        private final String contentType;
        private final String contentCharset;
        private final long ifModifiedSince;
        private Map<String, Object> attrs = null;
        private InputStream content;
        private InputStreamReader contentReader;
        
        
        public RequestImpl(HttpRequest nettyRequest, InetSocketAddress localAddr, InetSocketAddress remoteAddr,
                Map<String, Object> params, Map<String, Object> headers, String path, ContentType contentType) throws Exception
        {
            this.remoteAddr = remoteAddr;
            this.localAddr = localAddr;
            this.params = params;
            this.headers = headers;
            String[] pathElems = path.split("/");
            if (pathElems.length<2)
                throw new InvalidPathException(path);
            this.servicePath = pathElems[0];
            this.contextPath = StringUtils.join(pathElems, "/", 1, pathElems.length);
            this.method = nettyRequest.getMethod().name();
            this.ifModifiedSince = nettyRequest.headers().contains(HttpHeaders.Names.IF_MODIFIED_SINCE)?
                    HttpHeaders.getDateHeader(nettyRequest, HttpHeaders.Names.IF_MODIFIED_SINCE).getTime()
                    : -1;
            this.contentType = contentType!=null? contentType.getMimeType() : null;
            String _contentCharset=null;
            if (contentType!=null)
                _contentCharset = contentType.getCharset()==null? null : contentType.getCharset().name();
            this.contentCharset = _contentCharset;
        }

        @Override
        public String getRemoteAddr() {
            return remoteAddr.getAddress().getHostAddress();
        }

        @Override
        public int getRemotePort() {
            return remoteAddr.getPort();
        }

        @Override
        public String getServerHost() {
            return localAddr.getHostName();
        }

        @Override
        public Map<String, Object> getHeaders() {
            return headers;
        }

        @Override
        public Map<String, Object> getAttrs() {
            if (attrs!=null)
                return attrs;
            else {
                synchronized(this) {
                    if (attrs==null)
                        attrs = new HashMap<>();
                    return attrs;
                }
            }
        }

        @Override
        public Map<String, Object> getParams() {
            return params;
        }

        @Override
        public String getServicePath() {
            return servicePath;
        }

        @Override
        public String getContextPath() {
            return contextPath;
        }

        @Override
        public String getRootPath() {
            return "";
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public long getIfModifiedSince() {
            return ifModifiedSince;
        }

        @Override
        public String getContentType() {
            return contentType==null? null : contentType;
        }

        @Override
        public InputStream getContent() throws IOException {
            return content;
        }

        @Override
        public Reader getContentReader() throws IOException {
            if (contentReader==null)
                contentReader = contentCharset==null? new InputStreamReader(content) : new InputStreamReader(content, contentCharset);
            return contentReader;
        }
        
    }
}

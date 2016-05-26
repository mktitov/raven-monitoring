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
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.raven.audit.Action;
import org.raven.audit.AuditRecord;
import org.raven.auth.AnonymousLoginService;
import org.raven.auth.LoginService;
import org.raven.auth.UserContext;
import org.raven.auth.UserContextService;
import org.raven.net.AccessDeniedException;
import org.raven.net.AuthorizationNeededException;
import org.raven.net.ContextUnavailableException;
import org.raven.net.Request;
import org.raven.net.ResponseContext;
import org.raven.net.UnauthoriedException;
import org.raven.net.http.server.HttpConsts;
import org.raven.net.http.server.HttpServerContext;
import org.raven.net.http.server.HttpSession;
import org.raven.net.http.server.InvalidPathException;
import org.raven.net.http.server.SessionManager;
import org.raven.prj.impl.ProjectNode;
import org.raven.prj.impl.WebInterfaceNode;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class HttpServerHandler extends ChannelDuplexHandler {
    private final static AuthorizationNeededException authorizationNeededException = new AuthorizationNeededException();
    
    private final HttpServerContext serverContext;    
    
    private LoggerHelper logger;
    private RRController rrController;
    private InetSocketAddress remoteAddr;
    private InetSocketAddress localAddr;
    private HttpRequest httpRequest;
    private boolean hasError = false;

    public HttpServerHandler(HttpServerContext serverContext) {
        this.serverContext = serverContext;
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        long connectionNum = serverContext.getConnectionsCounter().incrementAndGet();
        remoteAddr = (InetSocketAddress) ctx.channel().remoteAddress();
        localAddr = (InetSocketAddress) ctx.channel().localAddress();
        logger = new LoggerHelper(serverContext.getOwner(), "("+connectionNum+") " + ctx.channel().remoteAddress().toString()+" ");
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
        hasError = true;
        if (logger.isErrorEnabled())
            logger.error("Error catched", cause);
        boolean writeStarted = rrController==null? false : rrController.isWriteStarted(); 
        RRController _rrController = rrController;
        HttpRequest _httpRequest = httpRequest;
        if (rrController!=null) {
            rrController.release();
            rrController = null;
            httpRequest = null;
        }
        if (!writeStarted && _httpRequest != null) {
            //if write not started we can produce http response with error            
            responseWithError(ctx, _httpRequest, _rrController, cause);
        } else
            ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof HttpRequest) 
                processNewHttpRequest((HttpRequest) msg, ctx);
            if (msg instanceof HttpContent) {
                processHttpRequestContent((HttpContent) msg);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ResponseMessage) {
            final ResponseMessage resp = (ResponseMessage) msg;
            if (resp.getRrController()!=rrController) {
                resp.getRrController().release();
                throw new InternalServerError("Attempt to write for comopleted response");
            }
            //After processing LastHttpContent then:
            //1. Must release the RRController
            //2. if connection not keep alive we must close connection
            ChannelFuture writeFuture = ctx.write(resp.getMessage(), promise);
            if (resp.getMessage() instanceof LastHttpContent) {
                rrController = null;
                writeFuture.addListener(new GenericFutureListener<ChannelFuture>() {
                    @Override public void operationComplete(ChannelFuture future) throws Exception {
                        resp.getRrController().release();
                        if (!resp.getRrController().isKeepAlive()) {
                            if (logger.isDebugEnabled())
                                logger.debug("Request processed. Closing connection");
                            future.channel().close();
                        } else if (logger.isDebugEnabled()) {
                            logger.debug("Request processed. Keeping connection alive");
                        }
                    }
                });
            }
            
            if (!resp.getRrController().isWriteStarted())
                resp.getRrController().setWriteStarted(true);
        } else
            throw new InternalServerError("Invalid message type. Exepect ResponseMessage but was: "
                    +(msg==null?null:msg.getClass().getName()));
    }
        
    private void processNewHttpRequest(HttpRequest request, ChannelHandlerContext ctx) throws Exception {
        httpRequest = request;
        hasError = false;
        final long requestNum = serverContext.getRequestsCounter().incrementAndGet();
        if (logger.isDebugEnabled())
            logger.debug("Received new request #"+requestNum);        
        if (rrController!=null && logger.isWarnEnabled())  {
            logger.warn("Received new request, but previous RRController not closed");
            rrController.release();
            rrController = null;
        }
        final LoggerHelper requestLogger = new LoggerHelper(logger, " req#"+requestNum+": ");
        final String contentTypeStr = request.headers().get(HttpHeaders.Names.CONTENT_TYPE);
        final ContentType contentType = contentTypeStr==null || contentTypeStr.isEmpty()? null : ContentType.parse(contentTypeStr);
        
        //экстрактим параметры
        QueryStringDecoder queryString = new QueryStringDecoder(request.getUri());        
        Map<String, Object> params = new HashMap<>();
        addQueryStringParams(params, queryString.parameters());        
        //экстрактим заголовки
        Map<String, Object> headers = decodeHeaders(request);
        
        //creating Raven Request        
        RequestImpl ravenRequest = new RequestImpl(request, localAddr, remoteAddr, params, headers, queryString.path(), 
                contentType, serverContext);
        
        //creating ResponseContext
        ResponseContext responseContext = serverContext.getNetworkResponseService().getResponseContext(ravenRequest);
        
        //проверяем аутентификацию
        final Set<Cookie> cookies = decodeCookies(request);
        final Cookie sessionCookie = getSessionIdCookie(cookies, ravenRequest.getProjectPath());
        final UserContext userContext = checkAuth(request, responseContext, sessionCookie, contentTypeStr);                
        //добавляем запись в audit
        serverContext.getAuditor().write(new AuditRecord(
                responseContext.getResponseBuilder().getResponseBuilderNode(), 
                userContext.getLogin(), 
                userContext.getHost(), 
                Action.VIEW, "Method: "+request.getMethod()+"\nParams: "+params));        
        
        //создаем RRController
        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        rrController = new RRController(serverContext, ravenRequest, responseContext, ctx.channel(), userContext, 
                requestLogger, sessionCookie, keepAlive);
        rrController.start(request instanceof LastHttpContent);
    }
    
    private UserContext checkAuth(HttpRequest request, ResponseContext responseContext, Cookie sessionCookie, String path) throws Exception {
        final LoginService loginService = responseContext.getLoginService();
        final SessionManager sessionManager = loginService.getSessionManager();
        if (!loginService.isStarted() || !loginService.isLoginAllowedFromIp(remoteAddr.getAddress().getHostAddress()))
            throw new AccessDeniedException();
        if (loginService instanceof AnonymousLoginService)
            return responseContext.getLoginService().login(null, null, null, null);        
        UserContext userContext = null;
        if (responseContext.isSessionAllowed()) {
            if (sessionManager==null) 
                throw new Exception("LoginService (%s) doesn't have session manager");
            final String sessionId = sessionCookie==null? null : sessionCookie.name();
            HttpSession session;
            if (sessionId!=null && (session = sessionManager.getSession(sessionId))!=null) {
                userContext = session.getUserContext();
                if (userContext!=null && userContext.isNeedRelogin()) {
                    //handling relogin event
                    if (responseContext.getLogger().isDebugEnabled())
                        responseContext.getLogger().debug("User ({}, {}) logged out", userContext.getLogin(), 
                                userContext.getName());
                    session.invalidate();
                    userContext = null;
                    throw authorizationNeededException;
                } else 
                    responseContext.setSession(session);
            }
        }
        boolean created = false;
        if (userContext==null) {
            created = true;
            String requestAuth = request.headers().get(HttpHeaders.Names.AUTHORIZATION);
            if (requestAuth == null) throw authorizationNeededException;
            else {
                String userAndPath = new String(Base64.decodeBase64(requestAuth.substring(6).getBytes()));
                final int colonPos = userAndPath.indexOf(':');
                if (colonPos==-1 || colonPos==0 || colonPos==userAndPath.length()-1)
                    throw authorizationNeededException;
                else {
                    final String login = userAndPath.substring(0, colonPos);
                    final String pwd = userAndPath.substring(colonPos+1);
                    userContext = loginService.login(login, pwd, remoteAddr.getAddress().getHostAddress(), responseContext);
                }
            }
        } else if (responseContext.getResponseBuilderLogger().isDebugEnabled())
            responseContext.getResponseBuilderLogger().debug("User ({}) already logged in. Skipping auth.", userContext);
        if (responseContext.isAccessGranted(userContext)) {
            if (created && responseContext.isSessionAllowed()) {
                if (responseContext.getResponseBuilderLogger().isDebugEnabled())
                    responseContext.getResponseBuilderLogger().debug("Created new session for user: "+userContext);
                serverContext.getAuditor().write(new AuditRecord(
                        responseContext.getResponseBuilder().getResponseBuilderNode(), 
                        userContext.getLogin(), remoteAddr.getAddress().getHostAddress(), Action.SESSION_START, null));
                final HttpSession newSession = sessionManager.createSession(); //TODO сессию нужно в response context запихать
                newSession.setUserContext(userContext);
                
//                final javax.servlet.http.HttpSession newSession = request.getSession();
                
                newSession.setAttribute(UserContextService.SERVICE_NODE_SESSION_ATTR, responseContext.getServiceNode());
                newSession.setUserContext(userContext);
                responseContext.setSession(newSession);
            }
        } else {
            if (responseContext.getLogger().isWarnEnabled())
                responseContext.getLogger().warn(String.format(
                        "User (%s) has no access to (%s) using (%s) operation", 
                        userContext, path, request.getMethod()));
            throw new UnauthoriedException();
        }        
        return userContext;
    }
    
    public Set<Cookie> decodeCookies(final HttpRequest request) {
        String cookieStr = request.headers().get(HttpHeaders.Names.COOKIE);
        return cookieStr==null? Collections.EMPTY_SET : ServerCookieDecoder.STRICT.decode(cookieStr);
    }
    
    public Cookie getSessionIdCookie(final Set<Cookie> cookies, final String projectPath) {
        for (Cookie cookie: cookies)
            if (HttpConsts.SESSIONID_COOKIE_NAME.equals(cookie.name()) && projectPath.equals(cookie.path()))
                return cookie;
        return null;
    }
    
    public String getSessionId(final Set<Cookie> cookies, final String projectPath) {
        Cookie sessionIdCookie = getSessionIdCookie(cookies, projectPath);
        return sessionIdCookie==null? null : sessionIdCookie.value();
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

    private void processHttpRequestContent(HttpContent content) throws Exception {
        if (rrController==null && !hasError) 
            throw new InternalServerError("Received HttpContent but RRController does not exists");
        else if (rrController!=null)
            rrController.onRequestContent(content);
    }

    private void responseWithError(ChannelHandlerContext ctx, HttpRequest httpRequest, RRController rrController, 
            Throwable error) 
    {
        try {
            HttpResponseStatus status;
            ResponseContext respContext = rrController==null? null : rrController.getResponseContext();
            RequestImpl req = rrController==null? null : rrController.getRequest();
            Map<String, Object> bindings = new HashMap<>();
            ProjectNode projectNode = getProjectNode(rrController);
            bindings.put("projectName", getProjectName(projectNode));
            bindings.put("message", null);
            bindings.put("exceptions", createListOfExceptions(error, new ArrayList()));
            
            bindings.put("requestURL", httpRequest.getUri());
            bindings.put("queryString", decodeQueryString(httpRequest));
            bindings.put("responseBuilderNodePath", getResponseBuilderPath(rrController));        
            bindings.put("devMode", isInDevMode(rrController));
            bindings.put("parameters", req==null? Collections.EMPTY_MAP : req.getParams());
            bindings.put("headers", req==null? Collections.EMPTY_MAP : req.getHeaders());
            //determine the status code
            if (error instanceof InvalidPathException || error instanceof ContextUnavailableException)
                status = HttpResponseStatus.NOT_FOUND;
            else {
                status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
            }
            //adding http status to bindings
            bindings.put("statusCode", status.code());
            bindings.put("statusCodeDesc", status.reasonPhrase());
            //detecting request locale
            //composing response 
            String pageContent = serverContext.getErrorPageGenerator().buildPage(
                    bindings, getLocaleFromRequest(httpRequest), logger.isTraceEnabled());
            ByteBuf buf = ctx.alloc().buffer().writeBytes(pageContent.getBytes("utf-8"));
            DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buf);
            resp.headers().set(HttpHeaders.Names.SERVER, HttpConsts.HTTP_SERVER_HEADER);
            resp.headers().set(HttpHeaders.Names.CACHE_CONTROL, "no-cache");
            resp.headers().add(HttpHeaders.Names.PRAGMA, "no-cache");
            resp.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html;charset=UTF-8");
            resp.headers().set(HttpHeaders.Names.DATE, new Date());
            resp.headers().set(HttpHeaders.Names.CONTENT_LENGTH, buf.readableBytes());
            
            ChannelFuture writeFuture = ctx.writeAndFlush(resp);
            if (!HttpHeaders.isKeepAlive(httpRequest))
                writeFuture.addListener(ChannelFutureListener.CLOSE);
        } catch (Throwable e) {
            if (logger.isErrorEnabled())
                logger.error("Error while composing ERROR_PAGE", e);
            ctx.close();
        }
    }

    private static String getResponseBuilderPath(RRController rrController) {
        return rrController != null ? 
                rrController.getResponseContext().getResponseBuilder().getResponseBuilderNode().getPath() 
                : null;
    }
    
    private boolean isInDevMode(RRController rrController) {
        if (rrController==null)
            return false;
        Node serviceNode = rrController.getResponseContext().getServiceNode();
        if (serviceNode instanceof WebInterfaceNode)
            serviceNode = serviceNode.getParent();
        NodeAttribute prodAttr = serviceNode.getAttr("prod");
        if (Boolean.class.equals(prodAttr.getType())) {
            Boolean res = prodAttr.getRealValue();
            return res==null? false : res;
        } else 
            return false;
    }
    
    private String getProjectName(ProjectNode projectNode) {
        return projectNode==null? null : projectNode.getName();        
    }

    private List createListOfExceptions(Throwable error, List errors) {
        if (error==null)
            return errors;
        errors.add(error);
        return createListOfExceptions(error.getCause(), errors);        
    }

    private ProjectNode getProjectNode(RRController rrController) {
        if (rrController==null)
            return null;
        Node serviceNode = rrController.getResponseContext().getServiceNode();
        return serviceNode instanceof WebInterfaceNode? ((ProjectNode)serviceNode.getParent()) : null;
    }

    private Locale getLocaleFromRequest(HttpRequest httpRequest) {
        String acceptLanguage = httpRequest.headers().get(HttpHeaders.Names.ACCEPT_LANGUAGE);
        if (acceptLanguage==null || acceptLanguage.isEmpty())
            return Locale.ENGLISH;
        AcceptLanguageHttpHeader header = new AcceptLanguageHttpHeader(acceptLanguage);
        return header.getLocale()==null? Locale.ENGLISH : header.getLocale();
    }

    private Object decodeQueryString(HttpRequest httpRequest) {
        QueryStringDecoder decoder = new QueryStringDecoder(httpRequest.getUri());
        return decoder.path();
    }
    
    public static class RequestImpl implements Request {
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
        private final HttpServerContext serverContext;
        private final String projectPath;
        private InputStream content;
        private Map<String, Object> attrs = null;
        private InputStreamReader contentReader;
        
        
        public RequestImpl(HttpRequest nettyRequest, InetSocketAddress localAddr, InetSocketAddress remoteAddr,
                Map<String, Object> params, Map<String, Object> headers, String path, ContentType contentType,
                HttpServerContext serverContext) 
            throws Exception
        {
            this.remoteAddr = remoteAddr;
            this.localAddr = localAddr;
            this.params = params;
            this.headers = headers;
            String[] pathElems = path.split("/");
            if (pathElems.length<2)
                throw new InvalidPathException(path);
            this.servicePath = pathElems[0];            
            this.projectPath = SRI_SERVICE.equals(servicePath)? "/"+SRI_SERVICE : "/"+pathElems[0]+"/"+pathElems[1];
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
            this.serverContext = serverContext;
        }

        public String getProjectPath() {
            return projectPath;
        }

        @Override
        public ExecutorService getExecutor() {
            return serverContext.getExecutor();
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
        public String getContentCharset() {
            return contentCharset;
        }

        @Override
        public InputStream getContent() throws IOException {
            return content;
        }
        
        public void attachContentInputStream(InputStream is) {
            this.content = is;
        }
        
        @Override
        public Reader getContentReader() throws IOException {
            if (contentReader==null)
                contentReader = contentCharset==null? new InputStreamReader(content) : new InputStreamReader(content, contentCharset);
            return contentReader;
        }        
    }
}

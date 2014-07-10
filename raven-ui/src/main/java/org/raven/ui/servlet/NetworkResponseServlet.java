/*
 *  Copyright 2009 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.raven.ui.servlet;

import groovy.lang.Writable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.activation.DataSource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.catalina.CometEvent;
//import org.apache.catalina.CometEvent;
//import org.apache.catalina.CometProcessor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.apache.tapestry5.ioc.Registry;
import org.raven.auth.AnonymousLoginService;
import org.raven.auth.AuthenticationFailedException;
import org.raven.auth.LoginException;
import org.raven.auth.LoginService;
import org.raven.auth.UserContext;
import org.raven.cache.TemporaryFileManager;
import org.raven.log.LogLevel;
import org.raven.net.AccessDeniedException;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.NetworkResponseServiceUnavailableException;
import org.raven.net.Outputable;
import org.raven.net.Request;
import org.raven.net.RequiredParameterMissedException;
import org.raven.net.Response;
import org.raven.net.ResponseContext;
import org.raven.net.Result;
import org.raven.net.UnauthoriedException;
import org.raven.net.impl.RedirectResult;
import org.raven.net.impl.RequestImpl;
import org.raven.tree.impl.LoggerHelper;
import org.raven.ui.util.RavenRegistry;
import org.slf4j.Logger;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NetworkResponseServlet extends HttpServlet  {

    private static final long serialVersionUID = 3540687833508728534L;
    private final AtomicLong fileUploadsCounter = new AtomicLong();

    protected class BadRequestException extends Exception {
        public BadRequestException() {
        }
        public BadRequestException(String string) {
            super(string);
        }
        
    }

    protected void checkRequest(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, BadRequestException 
    {
        if (request.getPathInfo().length() < 2) 
            throw new BadRequestException("Invalid context path");
        
//        {
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
//            return false;
//        } else {
//            return true;
//        }
    }

    protected UserContext checkAuth(HttpServletRequest request, HttpServletResponse response,
            ResponseContext responseContext, String context) 
        throws LoginException, AccessDeniedException, UnauthoriedException 
    {
        final LoginService loginService = responseContext.getLoginService();
        if (!loginService.isLoginAllowedFromIp(request.getRemoteAddr()))
            throw new AccessDeniedException();
        if (loginService instanceof AnonymousLoginService)
            return responseContext.getLoginService().login(null, null, null);        
        UserContext userContext = null;
        String userContextAttrName = "sri_user_context_"+loginService.getId();
        HttpSession session = null;
        if (responseContext.isSessionAllowed()) {
            session = request.getSession(false);
            if (session!=null) {
                userContext = (UserContext) session.getAttribute(userContextAttrName);
                if (userContext!=null && userContext.isNeedRelogin()) {
                    //handling relogin event
                    if (responseContext.getLogger().isDebugEnabled())
                        responseContext.getLogger().debug("User ({}, {}) logged out", userContext.getLogin(), 
                                userContext.getName());
                    session.invalidate();
                    userContext = null;
                    throw new UnauthoriedException();
                }
            }
        }
//        if (responseContext.getRequest().getParams().containsKey("logout")) {
//            if (session!=null)
//                session.invalidate();
//            throw new UnauthoriedException();
//        }
        boolean created = false;
        if (userContext==null) {
            created = true;
            String requestAuth = request.getHeader("Authorization");
            if (requestAuth == null) throw new UnauthoriedException();
            else {
                String userAndPath = new String(Base64.decodeBase64(requestAuth.substring(6).getBytes()));
                String elems[] = userAndPath.split(":");
                userContext = responseContext.getLoginService().login(elems[0], elems[1], request.getRemoteAddr());
            }
        } else if (responseContext.getResponseBuilderLogger().isDebugEnabled())
            responseContext.getResponseBuilderLogger().debug("User ({}) already logged in. Skiping auth.", userContext);
        if (responseContext.isAccessGranted(userContext)) {
            if (created && responseContext.isSessionAllowed()) {
                if (responseContext.getResponseBuilderLogger().isDebugEnabled())
                    responseContext.getResponseBuilderLogger().debug("Created new session for user: "+userContext);
                request.getSession().setAttribute(userContextAttrName, userContext);
            }
        } else {
            if (responseContext.getLogger().isWarnEnabled())
                responseContext.getLogger().warn(String.format(
                        "User (%s) has no access to (%s) using (%s) operation", 
                        userContext, request.getPathInfo(), request.getMethod()));
            throw new UnauthoriedException();
        }
        return userContext;
    }

    protected Map<String, Object> extractParams(HttpServletRequest request, NetworkResponseService responseService) 
            throws Exception 
    {
        Map<String, Object> params = new HashMap<String, Object>();
        Enumeration<String> reqParams = request.getParameterNames();
        if (reqParams != null) 
            while (reqParams.hasMoreElements()) {
                String paramName = reqParams.nextElement();
                params.put(paramName, request.getParameter(paramName));
            }
//        if (ServletFileUpload.isMultipartContent(request)) {
//            TemporaryFileManager tempFileManager = responseService.getTemporaryFileManager();
//            if (tempFileManager==null) 
//                throw new NetworkResponseServiceExeption("Can't store uploaded file because of "
//                        + "TemporaryFileManager not assigned to NetworkResponseServiceNode");
//            ServletFileUpload upload = new ServletFileUpload();
//            FileItemIterator it = upload.getItemIterator(request);
//            while (it.hasNext()) {
//                FileItemStream item = it.next();
//                String name = item.getFieldName();
//                if (item.isFormField())
//                    params.put(name, Streams.asString(item.openStream(), getTextFieldCharset(item)));
//                else {
//                    DataSource tempFile = tempFileManager.saveFile(
//                            responseService.getNetworkResponseServiceNode(), 
//                            "sri_fileupload_"+fileUploadsCounter.incrementAndGet(), 
//                            item.openStream(), item.getContentType(), true, item.getName());
//                    params.put(item.getFieldName(), tempFile);
//                }
//            }
//        }
        return params;
    }
    
    protected void parseMultipartContentParams(Map<String, Object> params, HttpServletRequest request, 
            NetworkResponseService responseService) throws Exception 
    {
        if (ServletFileUpload.isMultipartContent(request)) {
            TemporaryFileManager tempFileManager = responseService.getTemporaryFileManager();
            if (tempFileManager==null) 
                throw new NetworkResponseServiceExeption("Can't store uploaded file because of "
                        + "TemporaryFileManager not assigned to NetworkResponseServiceNode");
            FileItemIterator it = getMultipartItemIterator(request);
            while (it.hasNext()) {
                FileItemStream item = it.next();
                String name = item.getFieldName();
                if (item.isFormField())
                    params.put(name, Streams.asString(item.openStream(), getTextFieldCharset(item)));
                else {
                    DataSource tempFile = tempFileManager.saveFile(
                            responseService.getNetworkResponseServiceNode(), 
                            "sri_fileupload_"+fileUploadsCounter.incrementAndGet(), 
                            item.openStream(), item.getContentType(), true, item.getName());
                    params.put(item.getFieldName(), tempFile);
                }
            }
        }        
    }
    
    protected FileItemIterator getMultipartItemIterator(HttpServletRequest request) throws Exception {
        return new ServletFileUpload().getItemIterator(request);
    }
    
    protected String getTextFieldCharset(FileItemStream item) {
        Iterator<String> elems = item.getHeaders().getHeaders("content-type");
        while (elems.hasNext()) {
            String elem = elems.next();
            if (elem.startsWith("charset")) 
                return elem.split("=")[1];
        }
        return "utf-8";
    }
    
    protected Map<String, Object> extractHeaders(HttpServletRequest request) {
        final Map<String, Object> headers = new HashMap<String, Object>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) 
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }
        return headers;
    }

    protected void processServiceResponse(RequestContext ctx, Response serviceResponse) throws IOException 
    {
        final HttpServletRequest request = ctx.request;
        final HttpServletResponse response = ctx.response;
        if (serviceResponse == Response.NOT_MODIFIED) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            response.getOutputStream().close();
        } else {
            Map<String, String> headers = serviceResponse.getHeaders();
            if (headers != null) 
                for (Map.Entry<String, String> e : headers.entrySet()) 
                    response.addHeader(e.getKey(), e.getValue());
            Object content = serviceResponse.getContent();
            if (content instanceof RedirectResult) {
                response.sendRedirect(((RedirectResult)content).getContent().toString());
                response.flushBuffer();
                response.getOutputStream().close();
            } else {
                response.setContentType(serviceResponse.getContentType());
                if (content instanceof Result) {
                    Result result = (Result) content;
                    response.setStatus(result.getStatusCode());
                    content = result.getContent();
                    if (result.getContentType()!=null)
                        response.setContentType(result.getContentType());
                } else 
                    response.setStatus(content!=null? HttpServletResponse.SC_OK : HttpServletResponse.SC_NO_CONTENT);
                String charset = getCharset(request, serviceResponse);
                if (charset!=null)
                    response.setCharacterEncoding(charset);
//                response.setContentType(serviceResponse.getContentType());            
                if (serviceResponse.getLastModified()!=null) 
                    response.setDateHeader("Last-Modified", serviceResponse.getLastModified());
                response.addHeader("Cache-control", "no-cache");
                response.addHeader("Pragma", "no-cache");
                TypeConverter converter = ctx.registry.getService(TypeConverter.class);
                if (content!=null) {
                    if (content instanceof Writable) {
                        Writable writable = (Writable) content;
                        writable.writeTo(response.getWriter());
                        response.getWriter().close();
                    } else if (content instanceof Outputable) {
                        ((Outputable)content).outputTo(response.getOutputStream());
                        response.getOutputStream().close();
                    } else {
                        InputStream contentStream = converter.convert(InputStream.class, content, charset);
                        if (contentStream!=null) {
                            OutputStream out = response.getOutputStream();
                            try {
                                IOUtils.copy(contentStream, out);
                            } finally {
                                IOUtils.closeQuietly(out);
                                IOUtils.closeQuietly(contentStream);
//                                out.flush();
                            }
                        }
                    }
                } else
                    response.getOutputStream().close();
            }
        }
    }

    private String getCharset(HttpServletRequest request, Response serviceResponse) {
        if (serviceResponse.getCharset()!=null)
            return serviceResponse.getCharset().name();
        String charset = null;
        String charsetsStr = request.getHeader("Accept-Charset");
        if (charsetsStr != null) {
            String[] charsets = charsetsStr.split("\\s*,\\s*");
            if (charsets != null && charsets.length > 0) {
                charset = charsets[0].split(";")[0];
                getServletContext().log(String.format("Charset (%s) selected from request", charset));
            }
        }
        if (charset == null) {
//            getServletContext().log("Can't detect charset from request. Using default charset (UTF-8)");
            charset = "UTF-8";
        }
        return charset;
    }

    @SuppressWarnings("unchecked")
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException 
    {
        final RequestContext ctx = createContext(request, response);
//        Registry registry = RavenRegistry.getRegistry();
//        NetworkResponseService responseService = registry.getService(NetworkResponseService.class);
//        ResponseContext responseContext = null;
        try {
//            checkRequest(request, response);
//            String context = request.getPathInfo().substring(1);
//            Map<String, Object> params = extractParams(request, responseService);
//            Map<String, Object> headers = extractHeaders(request);
//            Request serviceRequest = new RequestImpl(request.getRemoteAddr(), params, headers, context, 
//                    request.getMethod().toUpperCase(), request);
//            responseContext = responseService.getResponseContext(serviceRequest);
//            UserContext user = checkAuth(request, response, responseContext, context);
            configureRequestContext(ctx);
            parseMultipartContentParams(ctx.responseContext.getRequest().getParams(), request, ctx.responseService);
            Response result = ctx.responseContext.getResponse(ctx.user);
            processServiceResponse(ctx, result);
        } catch (Throwable e) {
            processError(ctx, e);
        }
    }
    
    protected RequestContext createContext(HttpServletRequest request, HttpServletResponse response) {
        Registry registry = RavenRegistry.getRegistry();
        return new RequestContext(request, response, registry);
    }
    
    protected RequestContext configureRequestContext(RequestContext ctx)  
        throws Exception
    {
        checkRequest(ctx.request, ctx.response);
        String context = ctx.request.getPathInfo().substring(1);
        Map<String, Object> params = extractParams(ctx.request, ctx.responseService);
        Map<String, Object> headers = extractHeaders(ctx.request);
        Request serviceRequest = createServiceRequest(ctx, params, headers, context);
        ctx.responseContext = ctx.responseService.getResponseContext(serviceRequest);
        ctx.user = checkAuth(ctx.request, ctx.response, ctx.responseContext, context);
        return ctx;
    }
    
    protected Request createServiceRequest(RequestContext ctx, Map<String, Object> params, 
            Map<String, Object> headers, String context) 
    {
        return new RequestImpl(ctx.request.getRemoteAddr(), params, headers, context, 
                ctx.request.getMethod().toUpperCase(), ctx.request);
    }
    
    protected void processError(RequestContext ctx, Throwable e) 
        throws ServletException, IOException
    {
        boolean rethrow = false;
        if (e instanceof NetworkResponseServiceUnavailableException) {
            ctx.response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
        } else if (e instanceof ContextUnavailableException) {
            ctx.response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } else if (e instanceof AccessDeniedException) {
            ctx.response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } else if (e instanceof RequiredParameterMissedException || e instanceof NetworkResponseServlet.BadRequestException) {
            ctx.response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } else if (e instanceof UnauthoriedException || e instanceof AuthenticationFailedException) {
            ctx.response.setHeader("WWW-Authenticate", "BASIC realm=\"RAVEN\"");
            ctx.response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            rethrow = true;
        }
        String mess = String.format("Error processing request (%s) from host (%s)", 
                ctx.request.getPathInfo(), ctx.request.getRemoteAddr());
        if (ctx.responseService.getNetworkResponseServiceNode().isLogLevelEnabled(LogLevel.WARN)) {
            Logger logger = ctx.responseService.getNetworkResponseServiceNode().getLogger();
            if (rethrow)
                logger.warn(mess, e);
            else
                logger.warn(mess+"."+(e.getMessage()==null? e.getClass().getName() : e.getMessage()));
        }
        if (ctx.responseContext!=null && ctx.responseContext.getResponseBuilderLogger().isErrorEnabled()) 
            ctx.responseContext.getResponseBuilderLogger().error(mess, e);
        if (rethrow)
            throw new ServletException(e);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException 
    {
        processRequest(req, resp);
    }

    @Override
    public String getServletInfo() {
        return "Simple requests interface";
    }
    
    protected static class RequestContext implements IncomingDataListener {
        private final static AtomicLong requestId = new AtomicLong(0);
        public final HttpServletRequest request;
        public final HttpServletResponse response;
        public final Registry registry;
        public final NetworkResponseService responseService;
        public volatile IncomingDataListener incomingDataListener;
        public final LoggerHelper servletLogger;
        public volatile ServletException processingException;
        
        //timestamps for statistics 
        public final long createdTs = System.currentTimeMillis();
        public volatile long writeProcessedTs;
        public volatile long channelClosedTs;
        public volatile long builderExecutedTs;
        public volatile long builderProcessedTs;
        public volatile long waitForCloseTs;
//        public final Atomic
        private final AtomicReference<CometEvent> readProcessed = new AtomicReference<CometEvent>();
        private final AtomicBoolean writeProcessed = new AtomicBoolean();
        private final AtomicBoolean closed = new AtomicBoolean();
        
        public UserContext user;
        public ResponseContext responseContext;

        public RequestContext(HttpServletRequest request, HttpServletResponse response, Registry registry) 
        {
            this.request = request;
            this.response = response;
            this.registry = registry;
            this.responseService = registry.getService(NetworkResponseService.class);;
            this.servletLogger = new LoggerHelper(responseService.getNetworkResponseServiceNode(), 
                    "Servlet ["+requestId.incrementAndGet()+" "+request.getMethod()+" from "
                    +request.getRemoteAddr()+":"+request.getRemotePort()+"] ");
        }
               
        public void readProcessed(CometEvent ev) throws IOException {
            if (writeProcessed.get()) {
                if (servletLogger.isDebugEnabled())
                    servletLogger.debug("Closing channel");
                ev.close();
            }
//                tryClose();
        }
        
        public boolean isWriteProcessed() {
            return writeProcessed.get();
        }
        
        public void writeProcessed() throws IOException {
            writeProcessed.set(true);
//            if (writeProcessed.compareAndSet(false, true) && (readProcessed.get()!=null))
//                tryClose();
        }              

        private void tryClose() throws IOException{
            if (closed.compareAndSet(false, true)) {                
                if (servletLogger.isDebugEnabled())
                    servletLogger.debug("Closing channel");
                try {
//                    if (readProcessed.get()!=null)
//                        readProcessed.get().close();
                } finally {
                    ((CometResponseContext)responseContext).closeChannel();
                }
            }
        }
        
        public void closeChannel(CometEvent ev) throws IOException, ServletException {
            if (writeProcessed.get()) {
                waitForCloseTs = System.currentTimeMillis();
                channelClosedTs = System.currentTimeMillis();
                logStat();
                if (processingException!=null)
                    throw processingException;
                else 
                    ev.close();
            }
        }
        
        public void logStat() {
            if (servletLogger.isDebugEnabled())
                servletLogger.debug(String.format(
                        "Timing: TOTAL=%s; wait for buileder exec=%s; response compose time=%s; "
                                + "response send time=%s; wait for close ev=%s; channel close=%s",
                        channelClosedTs-createdTs, builderExecutedTs-createdTs, builderProcessedTs-builderExecutedTs, 
                        writeProcessedTs-builderProcessedTs, waitForCloseTs-writeProcessedTs, channelClosedTs-waitForCloseTs));
        }

        public void newDataAvailable() {
            final IncomingDataListener _listener = incomingDataListener;
            if (_listener!=null)
                _listener.newDataAvailable();
        }

        public void dataStreamClosed() {
            final IncomingDataListener _listener = incomingDataListener;
            if (_listener!=null)
                _listener.dataStreamClosed();
        }
    } 
}

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
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.activation.DataSource;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.*;
import javax.servlet.http.HttpSession;
import org.apache.catalina.CometEvent;
//import org.apache.catalina.CometEvent;
//import org.apache.catalina.CometProcessor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.ioc.Registry;
import org.raven.auth.AnonymousLoginService;
import org.raven.auth.AuthenticationFailedException;
import org.raven.auth.LoginException;
import org.raven.auth.LoginService;
import org.raven.auth.UserContext;
import org.raven.cache.TemporaryFileManager;
import org.raven.log.LogLevel;
import org.raven.net.AccessDeniedException;
import org.raven.net.AuthorizationNeededException;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.NetworkResponseServiceUnavailableException;
import org.raven.net.Outputable;
import org.raven.net.Request;
import org.raven.net.RequiredParameterMissedException;
import org.raven.net.Response;
import org.raven.net.ResponseContext;
import org.raven.net.ResponseServiceNode;
import org.raven.net.Result;
import org.raven.net.UnauthoriedException;
import org.raven.net.impl.RedirectResult;
import org.raven.net.impl.RequestImpl;
import org.raven.prj.WebInterface;
import org.raven.prj.impl.ProjectNode;
import org.raven.prj.impl.WebInterfaceNode;
import org.raven.tree.PropagatedAttributeValueError;
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
    
    public final static Map STATUS_CODES = new HashMap();

    static {
        STATUS_CODES.put(100,"CONTINUE");
        STATUS_CODES.put(101,"SWITCHING_PROTOCOLS");
        STATUS_CODES.put(200,"OK");
        STATUS_CODES.put(201,"CREATED");
        STATUS_CODES.put(202,"ACCEPTED");
        STATUS_CODES.put(203,"NON_AUTHORITATIVE_INFORMATION");
        STATUS_CODES.put(204,"NO_CONTENT");
        STATUS_CODES.put(205,"RESET_CONTENT");
        STATUS_CODES.put(206,"PARTIAL_CONTENT");
        STATUS_CODES.put(300,"MULTIPLE_CHOICES");
        STATUS_CODES.put(301,"MOVED_PERMANENTLY");
        STATUS_CODES.put(302,"MOVED_TEMPORARILY");
        STATUS_CODES.put(302,"FOUND");
        STATUS_CODES.put(303,"SEE_OTHER");
        STATUS_CODES.put(304,"NOT_MODIFIED");
        STATUS_CODES.put(305,"USE_PROXY");
        STATUS_CODES.put(307,"TEMPORARY_REDIRECT");
        STATUS_CODES.put(400,"BAD_REQUEST");
        STATUS_CODES.put(401,"UNAUTHORIZED");
        STATUS_CODES.put(402,"PAYMENT_REQUIRED");
        STATUS_CODES.put(403,"FORBIDDEN");
        STATUS_CODES.put(404,"NOT_FOUND");
        STATUS_CODES.put(405,"METHOD_NOT_ALLOWED");
        STATUS_CODES.put(406,"NOT_ACCEPTABLE");
        STATUS_CODES.put(407,"PROXY_AUTHENTICATION_REQUIRED");
        STATUS_CODES.put(408,"REQUEST_TIMEOUT");
        STATUS_CODES.put(409,"CONFLICT");
        STATUS_CODES.put(410,"GONE");
        STATUS_CODES.put(411,"LENGTH_REQUIRED");
        STATUS_CODES.put(412,"PRECONDITION_FAILED");
        STATUS_CODES.put(413,"REQUEST_ENTITY_TOO_LARGE");
        STATUS_CODES.put(414,"REQUEST_URI_TOO_LONG");
        STATUS_CODES.put(415,"UNSUPPORTED_MEDIA_TYPE");
        STATUS_CODES.put(416,"REQUESTED_RANGE_NOT_SATISFIABLE");
        STATUS_CODES.put(417,"EXPECTATION_FAILED");
        STATUS_CODES.put(500,"INTERNAL_SERVER_ERROR");
        STATUS_CODES.put(501,"NOT_IMPLEMENTED");
        STATUS_CODES.put(502,"BAD_GATEWAY");
        STATUS_CODES.put(503,"SERVICE_UNAVAILABLE");
        STATUS_CODES.put(504,"GATEWAY_TIMEOUT");
        STATUS_CODES.put(505,"HTTP_VERSION_NOT_SUPPORTED");
    }
    
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
                    throw new AuthorizationNeededException();
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
            if (requestAuth == null) throw new AuthorizationNeededException();
            else {
                String userAndPath = new String(Base64.decodeBase64(requestAuth.substring(6).getBytes()));
                String elems[] = userAndPath.split(":");
                if (elems.length>=2)
                    userContext = responseContext.getLoginService().login(elems[0], elems[1], request.getRemoteAddr());
                else 
                    throw new AuthorizationNeededException();
            }
        } else if (responseContext.getResponseBuilderLogger().isDebugEnabled())
            responseContext.getResponseBuilderLogger().debug("User ({}) already logged in. Skiping auth.", userContext);
        if (responseContext.isAccessGranted(userContext)) {
            if (created && responseContext.isSessionAllowed()) {
                if (responseContext.getResponseBuilderLogger().isDebugEnabled())
                    responseContext.getResponseBuilderLogger().debug("Created new session for user: "+userContext);
//                if (session==null) {
//                    Cookie[] cookies = request.getCookies();
//                    if (cookies!=null) 
//                        for (Cookie cookie: cookies)
//                            if (   "JSESSIONID".equals(cookie.getName()) && cookie.getValue()!=null 
//                                && !cookie.getValue().isEmpty())
//                            {
//                            if (responseContext.getLogger().isWarnEnabled())
//                                responseContext.getLogger().warn(String.format(
//                                        "User (%s) session invalidated need relogon", 
//                                        userContext));
//                            resetSessionIdCookie(response);
//                            throw new UnauthoriedException();
//                            }
//                }
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
    
    private void resetSessionIdCookie(HttpServletResponse response) {
        Cookie jsession = new Cookie("JSESSIONID", null);
        jsession.setMaxAge(0);
        response.addCookie(jsession);        
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
    
    protected void sendError(RequestContext ctx, int statusCode) throws IOException {
        sendError(ctx, statusCode, null, null);
    }
    
    protected void sendError(RequestContext ctx, int statusCode, String message) throws IOException {
        sendError(ctx, statusCode, message, null);
    }
    
    protected void sendError(RequestContext ctx, int statusCode, String message, Throwable exception) 
            throws IOException 
    {
        final HttpServletRequest request = ctx.request;
        final HttpServletResponse response = ctx.response;
        String charset = getCharset(request, null);
        response.setCharacterEncoding(charset);
        response.setContentType("text/html");
        response.setStatus(statusCode);
        
        response.setHeader("Cache-control", "no-cache");
        response.setHeader("Pragma", "no-cache");
        PrintWriter writer = response.getWriter();
        
        //adding html initial markup with css
        appendErrorHeader(writer);
        
        //adding report header
        boolean hasContext = ctx.responseContext!=null;
        ResponseServiceNode service = hasContext? ctx.responseContext.getServiceNode() : null;        
        writer.append("<h1>Problem detected");
        if (service instanceof WebInterfaceNode) {
            ProjectNode project = (ProjectNode) service.getParent();
            writer.append(" in project \"").append(project.getName()).append("\"");
        } 
        writer.append(": "+statusCode+" ("+STATUS_CODES.get(statusCode)+")");
        writer.append("</h1>");
                
        //adding message if need
        if (message==null && exception!=null) {
            message = exception.getCause() instanceof PropagatedAttributeValueError? 
                    exception.getCause().getMessage() : exception.getMessage();
//            message = exception.getMessage();
        } 
        if (message!=null) 
            writer.append("<pre>"+StringEscapeUtils.escapeHtml(message)+"</pre>");
        
        //adding request info
        writer.append("<h2>REQUEST: </h2>");
        writer.append("<ul>");
        writer.append("<li><b>URL: </b>"+request.getRequestURL());
        if (hasContext)
            writer.append("<li><b>RESPONSE BUILDER NODE: </b>"+ctx.responseContext.getResponseBuilder().getResponseBuilderNode().getPath());
        writer.append("<li><b>QUERY STRING: </b>"+request.getQueryString());
        writer.append("<li><b>HEADERS: </b>");
        writer.append("    <table><tr><th>Name</th><th>Value</th></tr>");
        Map<String, Object> headers = ctx.serviceRequest.getHeaders();
        if (headers!=null)
            for (Map.Entry<String, Object> entry: headers.entrySet())
                writer.append(String.format("<tr><td>%s</td><td>%s</td></tr>", entry.getKey(), entry.getValue()));
        writer.append("    </table>");
        writer.append("<li><b>PARAMETERS: </b>");
        writer.append("    <table><tr><th>Name</th><th>Value</th></tr>");
        Map<String, Object> params = ctx.serviceRequest.getParams();
        if (params!=null)
            for (Map.Entry<String, Object> entry: params.entrySet())
                writer.append(String.format("<tr><td>%s</td><td>%s</td></tr>", entry.getKey(), entry.getValue()));
        writer.append("    </table>");
        writer.append("</ul>");
        
        if (exception!=null) {
            writer.append("<h2>EXCEPTION(s) STACK: </h2>");
            writer.append("<a id='open-stack' tabindex=1 href=\"#s\">Show stack</a> <a id='close-stack' "
                    + "tabindex=2 href=\"#s\">Collapse stack</a>");
            writer.append("<ol>");
            appendException(writer, exception);
            writer.append("</ol>");
        }
        
        writer.append("</div></body>");
        writer.append("</html>");
        ctx.response.flushBuffer();
        writer.close();
    }
    
    private void appendException(final PrintWriter writer, final Throwable exception) {
        if (exception==null)
            return;
        if (!(exception.getCause() instanceof PropagatedAttributeValueError)) {
            writer.append("<li>"+exception.getClass().getName()+": ");
            String message = exception.getMessage();
            if (message!=null) {
                writer.append("<pre>"+StringEscapeUtils.escapeHtml(message)+"</pre>");
            }
            writer.append("<pre class=\"exception-stack\">");
            for (StackTraceElement elem: exception.getStackTrace())
                writer.append("at "+elem.toString()+"\n");
            writer.append("</pre>");
        }
        appendException(writer, exception.getCause());
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
//                response.sendRedirect(((RedirectResult)content).getContent().toString());
                if (ctx.user.isNeedRelogin() && ctx.responseContext.isSessionAllowed()) {
                    //removing jsession cookie
                    resetSessionIdCookie(response);
                }
                response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                response.addHeader("Location", ((RedirectResult)content).getContent().toString());
//                response.flushBuffer();
//                response.getOutputStream().flush();
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
                        response.getWriter().flush();
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
                                IOUtils.closeQuietly(out);
                            } finally {
//                                out.flush();
                                IOUtils.closeQuietly(contentStream);
//                                out.flush();
                            }
                        }
                    }
                } else {
                    response.flushBuffer();
                    response.getOutputStream().close();
                }
            }
        }
    }

    private String getCharset(HttpServletRequest request, Response serviceResponse) {
        if (serviceResponse!=null && serviceResponse.getCharset()!=null)
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
//        Request serviceRequest = 
        ctx.serviceRequest = createServiceRequest(ctx, params, headers, context);;
        ctx.responseContext = ctx.responseService.getResponseContext(ctx.serviceRequest);
        ctx.user = checkAuth(ctx.request, ctx.response, ctx.responseContext, context);
        return ctx;
    }
    
    protected Request createServiceRequest(RequestContext ctx, Map<String, Object> params, 
            Map<String, Object> headers, String context) 
    {
        return new RequestImpl(ctx.request.getRemoteAddr(), params, headers, context, 
                ctx.request.getMethod().toUpperCase(), ctx.request);
    }    
    
    private ResponseContext tryCreateErrorResponseContext(RequestContext ctx, String servicePath, String contextPath) 
            throws NetworkResponseServiceExeption 
    {
        ErrorRequest errorRequest = new ErrorRequest(ctx.responseContext.getRequest(), contextPath, servicePath);
        try {
            ResponseContext errorResponseContext = ctx.responseService.getResponseContext(errorRequest);
            return errorResponseContext;
        } catch (ContextUnavailableException e) {
            return null;
        } catch (NetworkResponseServiceUnavailableException e) {
            return null;
        } catch (NetworkResponseServiceExeption e) {
            String mess = "Error rendering error page";
            
            throw e;
        }
    }
    
    protected void processError2(RequestContext ctx, Throwable e) 
        throws ServletException, IOException
    {
        ErrorContext err = null;
        Request req = ctx.responseContext!=null? ctx.responseContext.getRequest() : null;
        if (e instanceof NetworkResponseServiceUnavailableException) {
            err = new ErrorContextImpl(SC_SERVICE_UNAVAILABLE, req, e.getMessage(), null);
        } else if (e instanceof ContextUnavailableException) {
            err = new ErrorContextImpl(SC_NOT_FOUND, req, e.getMessage(), null);
        } else if (e instanceof AccessDeniedException) {
            err = new ErrorContextImpl(SC_FORBIDDEN, req, e.getMessage(), null);
        } else if (e instanceof RequiredParameterMissedException || e instanceof NetworkResponseServlet.BadRequestException) {
            err = new ErrorContextImpl(SC_BAD_REQUEST, req, e.getMessage(), null);
        } else if (e instanceof UnauthoriedException || e instanceof AuthenticationFailedException) {
            ctx.response.setHeader("WWW-Authenticate", "BASIC realm=\"RAVEN\"");
            ctx.response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            err = new ErrorContextImpl(SC_INTERNAL_SERVER_ERROR, req, e.getMessage(), e);
        }
        if (err!=null) {
            //trying to find error page template in project
            //  1. Need to create org.raven.net.Request
            //  2. Create ResponseContext
            //  3. Build response
            //  4. Write response
            
            //if error page template not found in project trying to find it in global SRI service

            //if not found using simple hard coded template
        }
    }
    
    protected void processError(RequestContext ctx, Throwable e) {
        try {
            boolean internalError = false;
            boolean isWarn = true;
            if (e instanceof NetworkResponseServiceUnavailableException) {
                sendError(ctx, HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
            } else if (e instanceof ContextUnavailableException) {
                sendError(ctx, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else if (e instanceof AccessDeniedException) {
                sendError(ctx, HttpServletResponse.SC_FORBIDDEN);
            } else if (e instanceof RequiredParameterMissedException || e instanceof NetworkResponseServlet.BadRequestException) {
                sendError(ctx, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else if (e instanceof UnauthoriedException || e instanceof AuthenticationFailedException) {
                isWarn = !(e instanceof AuthorizationNeededException);
                ResponseServiceNode service = ctx.responseContext.getServiceNode();
                String realm = service instanceof WebInterfaceNode? service.getParent().getName() : "RAVEN";
                if ("system".equals(realm))
                    realm = "RAVEN";
                ctx.response.setHeader("WWW-Authenticate", String.format("BASIC realm=\"%s\"", realm));
                sendError(ctx, HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                sendError(ctx, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, e);
                internalError = true;
            }
            String mess = String.format("Error processing request (%s) from host (%s)", 
                    ctx.request.getPathInfo(), ctx.request.getRemoteAddr());
            if (ctx.servletLogger.isWarnEnabled() && isWarn) {
                if (internalError)
                    ctx.servletLogger.warn(mess, e);
                else
                    ctx.servletLogger.warn(mess+"."+(e.getMessage()==null? e.getClass().getName() : e.getMessage()));
            } else if (!isWarn && ctx.servletLogger.isDebugEnabled())
                ctx.servletLogger.debug(mess+"."+(e.getMessage()==null? e.getClass().getName() : e.getMessage()));
                
            if (ctx.responseContext!=null) {
                final Logger logger = ctx.responseContext.getResponseBuilderLogger();
                if (isWarn && logger.isErrorEnabled()) 
                    logger.error(mess, e);
            }
        } catch (IOException ex) {
            if (ctx.servletLogger.isErrorEnabled())
                ctx.servletLogger.error("Error while processing ERROR!", ex);
        }
    }
    
//    protected void processError(RequestContext ctx, Throwable e) 
//        throws ServletException, IOException
//    {
//        boolean rethrow = false;
//        if (e instanceof NetworkResponseServiceUnavailableException) {
//            ctx.response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
//        } else if (e instanceof ContextUnavailableException) {
//            ctx.response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
//        } else if (e instanceof AccessDeniedException) {
//            ctx.response.sendError(HttpServletResponse.SC_FORBIDDEN);
//        } else if (e instanceof RequiredParameterMissedException || e instanceof NetworkResponseServlet.BadRequestException) {
//            ctx.response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
//        } else if (e instanceof UnauthoriedException || e instanceof AuthenticationFailedException) {
//            ctx.response.setHeader("WWW-Authenticate", "BASIC realm=\"RAVEN\"");
//            ctx.response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
//        } else {
//            rethrow = true;
//        }
//        String mess = String.format("Error processing request (%s) from host (%s)", 
//                ctx.request.getPathInfo(), ctx.request.getRemoteAddr());
//        if (ctx.responseService.getNetworkResponseServiceNode().isLogLevelEnabled(LogLevel.WARN)) {
//            Logger logger = ctx.responseService.getNetworkResponseServiceNode().getLogger();
//            if (rethrow)
//                logger.warn(mess, e);
//            else
//                logger.warn(mess+"."+(e.getMessage()==null? e.getClass().getName() : e.getMessage()));
//        }
//        if (ctx.responseContext!=null && ctx.responseContext.getResponseBuilderLogger().isErrorEnabled()) 
//            ctx.responseContext.getResponseBuilderLogger().error(mess, e);
//        if (rethrow)
//            throw new ServletException(e);
//    }
    
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
    
    private void appendErrorHeader(PrintWriter writer) {
        writer.append("<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <meta charset=\"UTF-8\">\n" +
            "  <title>Request processing error</title>\n" +
            "  <style>\n" +
            "  <!--\n" +
            "  * {\n" +
            "    font-size: 10pt;\n" +
            "    font-family: \"Lucida Sans Unicode\", \"Lucida Grande\", sans-serif;\n" +
            "  } \n" +
            "  h1 {\n" +
            "    color: red;\n" +
            "    font-size: 12pt;\n" +
            "  }\n" +
            "  h2 {\n" +
            "    color: #565656;\n" +
            "    font-size: 12pt;\n" +
            "  }\n" +
            "  body {\n" +
            "    background-color: #FFEBE5;\n" +
            "  }\n" +
            "  pre {\n" +
            "    font-family: \"Lucida Console\", Monaco, monospace, \"Courier New\", Courier;\n" +
            "    font-size: 10pt;\n" +
            "    color: black;" +
            "    background-color: white;\n" +
            "    padding-left:20px;\n" +
            "    padding-top: 5px;\n" +
            "    padding-right:5px;\n" +
            "    padding-bottom:5px;\n" +
            "  }\n" +
            "  ol > li {\n" +
            "    font-family: \"Lucida Console\", Monaco, monospace, \"Courier New\", Courier;\n" +
            "    font-size: 10pt;\n" +
            "    color: #AE2000;\n" +
            "  }\n" +
            "  pre.exception-stack {\n" +
            "    display: none;\n" +
            "    color: black;\n" +
            "  }\n" +
            "  \n" +
            "  #open-stack:focus ~ ol > li pre.exception-stack {\n" +
            "    display:block;\n" +
            "  }\n" +
            "  \n" +
            "  #close-stack:focus ~ ol > li pre.exception-stack {\n" +
            "    display:none;\n" +
            "  }\n" +
            "  \n" +
            "  pre.exception-stack:hover {\n" +
            "    display: block;\n" +
            "  }\n" +
            "  "+
            "  table {\n" +
            "    margin-top: 5px;\n" +
            "    margin-bottom:7px;\n" +
            "    border-collapse: collapse;\n" +
            "    padding:2px;\n"+
            "  }\n" +
            "  table, th, td {\n" +
            "    border: 1px solid black;\n" +
            "  }  \n" +
            "  th, td {\n" +
            "    padding: 2px;\n" +
            "  }\n" +
            "  -->\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div style='width:100%;height:100%; overflow:auto;'>");
    }
    
    protected static class RequestContext implements DataReceiver {
        public final static long REQUEST_STREAM_WAIT_TIMEOUT = 5000;
        public final static long RESPONSE_GENERATION_TIMEOUT = 30000;
        private final static AtomicLong requestId = new AtomicLong(0);
        public final HttpServletRequest request;
        public final HttpServletResponse response;
        public final Registry registry;
        public final NetworkResponseService responseService;
        public final LoggerHelper servletLogger;
        private final CometInputStream requestStream;
        private volatile Reader requestReader;
        public final String requestEncoding;
        public volatile ServletException processingException;
        public volatile boolean responseManagingByBuilder = false;
        
        private final AtomicLong requestStreamRequestTime = new AtomicLong();
        private volatile boolean responseGenerated = false;
        //timestamps for statistics 
        public final long createdTs = System.currentTimeMillis();
//        public volatile long 
        public volatile long writeProcessedTs;
        public volatile long channelClosedTs;
        public volatile long builderExecutedTs;
        public volatile long builderProcessedTs;
        public volatile long waitForCloseTs;
        public volatile long redBytes = 0;
        public volatile AtomicBoolean dataStreamClosed = new AtomicBoolean();
//        public final Atomic
        private final AtomicReference<CometEvent> readProcessed = new AtomicReference<CometEvent>();
        private final AtomicBoolean writeProcessed = new AtomicBoolean();
        private final AtomicBoolean closed = new AtomicBoolean();
        
        public UserContext user;
        public ResponseContext responseContext;
        public Request serviceRequest;

        public RequestContext(HttpServletRequest request, HttpServletResponse response, Registry registry) 
        {
            this.request = request;
            this.response = response;
            this.registry = registry;
            this.responseService = registry.getService(NetworkResponseService.class);
            if (!request.getMethod().equals("GET") && isStreamContentType(request))
                requestStream = new CometInputStreamImpl();
            else
                requestStream = CometInputStream.EMPTY_STREAM;
            this.servletLogger = new LoggerHelper(responseService.getNetworkResponseServiceNode(), 
                    "Servlet ["+requestId.incrementAndGet()+" "+request.getMethod()+" from "
                    +request.getRemoteAddr()+":"+request.getRemotePort()+"] ");
            this.requestEncoding = request.getCharacterEncoding()!=null? request.getCharacterEncoding() : "utf-8";
        }
        
        private static boolean isStreamContentType(HttpServletRequest req) {
            String ct = req.getContentType();
            if (ct==null || ct.contains("application/x-www-form-urlencoded"))
                return false;
            else
                return true;
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
        
        public void writeProcessed() {
            writeProcessed.set(true);
        }              
       
        public void closeChannel(CometEvent ev) throws IOException, ServletException {
            if (closed.get()) {
                if (servletLogger.isDebugEnabled())
                    servletLogger.debug("Can't close channel because of it already closed");
                return;
            }
            if (writeProcessed.get()) {
                waitForCloseTs = System.currentTimeMillis();
                channelClosedTs = System.currentTimeMillis();
                logStat();
                if (processingException!=null)
                    throw processingException;
                else {
                    processChannelClose(ev);
                }
            } else {
                final long curTime = System.currentTimeMillis();
                if (   requestStreamRequestTime.get()>0 
                       && curTime>requestStreamRequestTime.get()+REQUEST_STREAM_WAIT_TIMEOUT)
                {
                    if (servletLogger.isWarnEnabled())
                        servletLogger.warn("Request stream read timeout detected! Closing request stream");
                    requestStream.dataStreamClosed();
                }
                if (!responseGenerated && curTime > createdTs + RESPONSE_GENERATION_TIMEOUT) 
                {
                    if (servletLogger.isWarnEnabled())
                        servletLogger.warn("Response generating by builder timeout detected! Closing channel");
                    processChannelClose(ev);
                } else if (responseManagingByBuilder) {
                    if (servletLogger.isDebugEnabled())
                        servletLogger.debug("Channel closed by client");
                    processChannelClose(ev);
                }
            }
        }

        private void processChannelClose(CometEvent ev) throws IOException {
            if (closed.compareAndSet(false, true)) {
                IOUtils.closeQuietly(ev.getHttpServletRequest().getInputStream());
                ev.close();
                responseContext.channelClosed();
                if (servletLogger.isDebugEnabled())
                    servletLogger.debug("Channel CLOSED");
            } else {
                if (servletLogger.isDebugEnabled())
                    servletLogger.debug("Trying to close channel but it already closed. Ignoring");                
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

        public boolean canPushBuffer() {
            return requestStream.canPushBuffer();
        }

        public void pushBuffer(ByteBuf buf) {
            redBytes += buf.readableBytes();
            requestStream.pushBuffer(buf);
        }

        public long getRedBytes() {
            return redBytes;
        }
        
        public boolean isDataStreamClosed() {
            return dataStreamClosed.get();
        }

        public void dataStreamClosed() {
            if (dataStreamClosed.compareAndSet(false, true))
                requestStream.dataStreamClosed();
        }

        public Reader getRequestReader() throws IOException {
            if (requestReader==null)
                synchronized(this) {
                    if (requestReader==null)
                        requestReader = new InputStreamReader(requestStream, requestEncoding);
                }
            return requestReader;
        }

        public CometInputStream getRequestStream() {
            if (requestStreamRequestTime.get()==0)
                requestStreamRequestTime.compareAndSet(0, System.currentTimeMillis());
            return requestStream;
        }        
        
        public void responseGenerated() {
            responseGenerated = true;
        }
    } 
}

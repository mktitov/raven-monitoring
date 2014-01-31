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
import java.util.concurrent.atomic.AtomicLong;
import javax.activation.DataSource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
//import org.apache.catalina.CometEvent;
//import org.apache.catalina.CometProcessor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
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
import org.raven.net.AccessDeniedException;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.NetworkResponseServiceUnavailableException;
import org.raven.net.RedirectResult;
import org.raven.net.Request;
import org.raven.net.RequiredParameterMissedException;
import org.raven.net.Response;
import org.raven.net.ResponseContext;
import org.raven.net.UnauthoriedException;
import org.raven.net.impl.RequestImpl;
import org.raven.ui.util.RavenRegistry;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NetworkResponseServlet extends HttpServlet  {

    private static final long serialVersionUID = 3540687833508728534L;
    private final AtomicLong fileUploadsCounter = new AtomicLong();

    private class BadRequestException extends Exception {
        public BadRequestException() {
        }
        public BadRequestException(String string) {
            super(string);
        }
        
    }

    private void checkRequest(HttpServletRequest request, HttpServletResponse response) 
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

    private UserContext checkAuth(HttpServletRequest request, HttpServletResponse response,
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
        if (responseContext.isSessionAllowed()) {
            HttpSession session = request.getSession(false);
            if (session!=null) 
                userContext = (UserContext) session.getAttribute(userContextAttrName);                
        }
        if (userContext==null) {
            String requestAuth = request.getHeader("Authorization");
            if (requestAuth == null) throw new UnauthoriedException();
            else {
                String userAndPath = new String(Base64.decodeBase64(requestAuth.substring(6).getBytes()));
                String elems[] = userAndPath.split(":");
                userContext = responseContext.getLoginService().login(elems[0], elems[1], request.getRemoteAddr());
                if (responseContext.isAccessGranted(userContext)) {
                    if (responseContext.isSessionAllowed()) {
                        if (responseContext.getResponseBuilderLogger().isDebugEnabled())
                            responseContext.getResponseBuilderLogger().debug("Created new session for user: "+userContext);
                        request.getSession().setAttribute(userContextAttrName, userContext);
                    }
                }
                else throw new UnauthoriedException();
            }
        } else if (responseContext.getResponseBuilderLogger().isDebugEnabled())
            responseContext.getResponseBuilderLogger().debug("User ({}) already logged in. Skiping auth.", userContext);
        return userContext;
    }

    private Map<String, Object> extractParams(HttpServletRequest request, NetworkResponseService responseService) 
            throws Exception 
    {
        Map<String, Object> params = new HashMap<String, Object>();
        Enumeration<String> reqParams = request.getParameterNames();
        if (reqParams != null) 
            while (reqParams.hasMoreElements()) {
                String paramName = reqParams.nextElement();
                params.put(paramName, request.getParameter(paramName));
            }
        if (ServletFileUpload.isMultipartContent(request)) {
            TemporaryFileManager tempFileManager = responseService.getTemporaryFileManager();
            if (tempFileManager==null) 
                throw new NetworkResponseServiceExeption("Can't store uploaded file because of "
                        + "TemporaryFileManager not assigned to NetworkResponseServiceNode");
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator it = upload.getItemIterator(request);
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
        return params;
    }
    
    private String getTextFieldCharset(FileItemStream item) {
        Iterator<String> elems = item.getHeaders().getHeaders("content-type");
        while (elems.hasNext()) {
            String elem = elems.next();
            if (elem.startsWith("charset")) 
                return elem.split("=")[1];
        }
        return "utf-8";
    }
    
    private Map<String, Object> extractHeaders(HttpServletRequest request) {
        final Map<String, Object> headers = new HashMap<String, Object>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) 
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }
        return headers;
    }

    private void processServiceResponse(HttpServletRequest request, HttpServletResponse response,
            Response serviceResponse, Registry registry) throws IOException 
    {
        if (serviceResponse == Response.NOT_MODIFIED) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            Map<String, String> headers = serviceResponse.getHeaders();
            if (headers != null) 
                for (Map.Entry<String, String> e : headers.entrySet()) 
                    response.addHeader(e.getKey(), e.getValue());
            Object content = serviceResponse.getContent();
            if (content instanceof RedirectResult)
                response.sendRedirect(((RedirectResult)content).getUrl());
            else {
                String charset = getCharset(request);
                if (serviceResponse.getContentType().toUpperCase().startsWith("TEXT")) 
                    response.setCharacterEncoding(charset);
                response.setContentType(serviceResponse.getContentType());            
                if (serviceResponse.getLastModified()!=null) 
                    response.setDateHeader("Last-Modified", serviceResponse.getLastModified());
                response.addHeader("Cache-control", "no-cache");
                response.addHeader("Pragma", "no-cache");
                TypeConverter converter = registry.getService(TypeConverter.class);
                if (content!=null) {
                    if (content instanceof Writable) {
                        Writable writable = (Writable) content;
                        writable.writeTo(response.getWriter());
                    } else {
                        OutputStream out = response.getOutputStream();
                        InputStream contentStream = converter.convert(InputStream.class, serviceResponse.getContent(), charset);
                        try {
                            IOUtils.copy(contentStream, out);
                        } finally {
                            IOUtils.closeQuietly(out);
                            IOUtils.closeQuietly(contentStream);
                        }
                    }
                }
                response.setStatus(content!=null? HttpServletResponse.SC_OK : HttpServletResponse.SC_NO_CONTENT);
            }
        }
    }

    private String getCharset(HttpServletRequest request) {
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
            getServletContext().log("Can't detect charset from request. Using default charset (UTF-8)");
            charset = "UTF-8";
        }
        return charset;
    }

    @SuppressWarnings("unchecked")
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Registry registry = RavenRegistry.getRegistry();
        NetworkResponseService responseService = registry.getService(NetworkResponseService.class);
        ResponseContext responseContext = null;
        try {
            checkRequest(request, response);
            String context = request.getPathInfo().substring(1);
            Map<String, Object> params = extractParams(request, responseService);
            Map<String, Object> headers = extractHeaders(request);
            Request serviceRequest = new RequestImpl(request.getRemoteAddr(), params, headers, context, 
                    request.getMethod().toUpperCase(), request);
            responseContext = responseService.getResponseContext(serviceRequest);
            UserContext user = checkAuth(request, response, responseContext, context);
//            if (!checkAuth(request, response, responseService, context))
//                return;

//            boolean isPut = request.getMethod().equalsIgnoreCase("PUT");
//            if (isPut) 
//                params.put(NetworkResponseService.REQUEST_CONTENT_PARAMETER, request.getInputStream());
//            Response result = responseService.getResponse(context, request.getRemoteAddr(), params);
            Response result = responseContext.getResponse(user);
            processServiceResponse(request, response, result, registry);
        } catch (Throwable e) {
            boolean rethrow = false;
            if (e instanceof NetworkResponseServiceUnavailableException) {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
            } else if (e instanceof ContextUnavailableException) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else if (e instanceof AccessDeniedException) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else if (e instanceof RequiredParameterMissedException || e instanceof BadRequestException) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else if (e instanceof UnauthoriedException || e instanceof AuthenticationFailedException) {
                response.setHeader("WWW-Authenticate", "BASIC realm=\"RAVEN\"");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                rethrow = true;
//                StringWriter buffer = new StringWriter();
//                PrintWriter writer = new PrintWriter(buffer);
//                e.printStackTrace(writer);
//                String mess = e.getMessage()==null? "" : e.getMessage();
//                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
//                        mess+"<br>"+"<pre style='font-size: 10pt; font-style: normal'>"+buffer.toString()+"</pre>");
            }
            if (responseContext!=null && responseContext.getResponseBuilderLogger().isErrorEnabled()) 
                responseContext.getResponseBuilderLogger().error(
                        String.format("Request (%s) processing error", request.getPathInfo()), e);
            if (rethrow)
                throw new ServletException(e);
        }
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
    public String getServletInfo() {
        return "Simple requests interface";
    }
}

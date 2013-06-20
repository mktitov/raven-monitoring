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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.tapestry5.ioc.Registry;
import org.raven.net.AccessDeniedException;
import org.raven.net.Authentication;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceUnavailableException;
import org.raven.net.RequiredParameterMissedException;
import org.raven.net.Response;
import org.raven.ui.util.RavenRegistry;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NetworkResponseServlet extends HttpServlet {
    private static final long serialVersionUID = 3540687833508728534L;
    private class BadRequestException extends Exception {}
    
    private boolean checkRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getPathInfo().length()<2) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        } else                 
            return true;        
    }
    
    private boolean checkAuth(HttpServletRequest request, HttpServletResponse response, 
        NetworkResponseService responseService, String context) throws Exception
    {
        Authentication contextAuth = responseService.getAuthentication(context, request.getRemoteAddr());
        if (contextAuth!=null) {
            String requestAuth = request.getHeader("Authorization");
            if (requestAuth==null) {
                response.setHeader(
                        "WWW-Authenticate", "BASIC realm=\"RAVEN simple request interface\"");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            } else {
                String userAndPath = new String(Base64.decodeBase64(requestAuth.substring(6).getBytes()));
                String elems[] = userAndPath.split(":");
                if (elems.length!=2
                    || !contextAuth.getUser().equals(elems[0])
                    || !contextAuth.getPassword().equals(elems[1]))
                {
                    throw new AccessDeniedException();
                } 
                return true;
            }
        } else 
            return true;
    }
    
    private Map<String, Object> extractParams(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<String, Object>();
        
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames!=null)
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                params.put(headerName, request.getHeader(headerName));
            }

        Enumeration<String> reqParams = request.getParameterNames();
        if (reqParams!=null)
            while (reqParams.hasMoreElements()) {
                String paramName = reqParams.nextElement();
                params.put(paramName, request.getParameter(paramName));
            }
        return params;
    } 
    
    private void processServiceResponse(HttpServletRequest request, HttpServletResponse response, 
        Response serviceResponse, Registry registry) throws IOException 
    {
        if (serviceResponse.getContent()==null)
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        else {
            Map<String, String> headers = serviceResponse.getHeaders();
            if (headers!=null)
                for (Map.Entry<String, String> e: headers.entrySet())
                    response.addHeader(e.getKey(), e.getValue());
            String charset = getCharset(request);
            if (serviceResponse.getContentType().toUpperCase().startsWith("TEXT"))
                response.setCharacterEncoding(charset);
            response.setContentType(serviceResponse.getContentType());
            TypeConverter converter = registry.getService(TypeConverter.class);
            OutputStream out = response.getOutputStream();
            InputStream contentStream = converter.convert(InputStream.class, serviceResponse.getContent(), charset);
            try {
                IOUtils.copy(contentStream, out);
            } finally {
                IOUtils.closeQuietly(out);
                IOUtils.closeQuietly(contentStream);
            }
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }
    
    private String getCharset(HttpServletRequest request) {
        String charset = null;
        String charsetsStr = request.getHeader("Accept-Charset");
        if (charsetsStr!=null) {
            String[] charsets = charsetsStr.split("\\s*,\\s*");
            if (charsets!=null && charsets.length>0) {
                charset = charsets[0].split(";")[0];
                getServletContext().log(String.format("Charset (%s) selected from request", charset));
            }
        }
        if (charset==null) {
            getServletContext().log("Can't detect charset from request. Using default charset (UTF-8)");
            charset = "UTF-8";
        }
        return charset;
    }

	@SuppressWarnings("unchecked")
	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        Registry registry = RavenRegistry.getRegistry();
        NetworkResponseService responseService = registry.getService(NetworkResponseService.class);
//        request.setCharacterEncoding("windows-1251");
        try {
            if (!checkRequest(request, response))
                return;
            String context = request.getPathInfo().substring(1);
            if (!checkAuth(request, response, responseService, context))
                return;
            Map<String, Object> params = extractParams(request);

            boolean isPut = request.getMethod().equalsIgnoreCase("PUT");
            if (isPut)
                params.put(NetworkResponseService.REQUEST_CONTENT_PARAMETER, request.getInputStream());
            Response result = responseService.getResponse(context, request.getRemoteAddr(), params);
            processServiceResponse(request, response, result, registry);
        } catch(Throwable e) {
            if (e instanceof NetworkResponseServiceUnavailableException)
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
            else if (e instanceof ContextUnavailableException)
                response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            else if (e instanceof AccessDeniedException)
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            else if (e instanceof RequiredParameterMissedException || e instanceof BadRequestException)
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            else
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    } 

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        processRequest(request, response);
    } 

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        processRequest(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo()
    {
        return "Simple requests interface";
    }

//    public void event(CometEvent event) throws IOException, ServletException
//    {
//        switch (event.getEventType())
//        {
//            case BEGIN: checkAuth(event); processRequest(event); break;
//            case READ: processRequest(event); break;
//            case END: event.close(); break;
//            case ERROR: event.close(); break;
//        }
//
//    }
//
//    private void checkAuth(CometEvent event) throws IOException
//    {
//        Registry registry = RavenRegistry.getRegistry();
//        NetworkResponseService responseService = registry.getService(NetworkResponseService.class);
//        HttpServletRequest request = event.getHttpServletRequest();
//        HttpServletResponse response = event.getHttpServletResponse();
//        try
//        {
//            String context = request.getPathInfo();
//            if (context.length()<2)
//            {
//                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
//                event.close();
//                return;
//            }
//
//            context = context.substring(1);
//
//            Authentication contextAuth = responseService.getAuthentication(context);
//            if (contextAuth!=null)
//            {
//                String requestAuth = request.getHeader("Authorization");
//                if (requestAuth==null)
//                {
//                    response.setHeader(
//                            "WWW-Authenticate", "BASIC realm=\"RAVEN simple request interface\"");
//                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
//                    event.close();
//                    return;
//                }
//                else
//                {
//                    String userAndPath = new String(Base64.decodeBase64(
//                            requestAuth.substring(6).getBytes()));
//                    String elems[] = userAndPath.split(":");
//                    if (elems.length!=2
//                        || !contextAuth.getUser().equals(elems[0])
//                        || !contextAuth.getPassword().equals(elems[1]))
//                    {
//                        throw new AccessDeniedException();
//                    }
//                }
//            }
//        }
//        catch(Throwable e)
//        {
//            if (e instanceof NetworkResponseServiceUnavailableException)
//                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
//            else if (e instanceof ContextUnavailableException)
//                response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
//            else if (e instanceof AccessDeniedException)
//                response.sendError(HttpServletResponse.SC_FORBIDDEN);
//            else if (e instanceof RequiredParameterMissedException)
//                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage()==null? "":e.getMessage());
//            else
//                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
//            event.close();
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//	private void processRequest(CometEvent event) throws IOException
//    {
//        Registry registry = RavenRegistry.getRegistry();
//        NetworkResponseService responseService = registry.getService(NetworkResponseService.class);
//        HttpServletRequest request = event.getHttpServletRequest();
//        HttpServletResponse response = event.getHttpServletResponse();
//        boolean isPut = request.getMethod().equalsIgnoreCase("PUT");
//        if (event.getEventType()==CometEvent.EventType.BEGIN && isPut)
//            return;
//        try
//        {
//            String context = request.getPathInfo().substring(1);
//
//            Map<String, Object> params = new HashMap<String, Object>();
//
//            Enumeration<String> headerNames = request.getHeaderNames();
//            if (headerNames!=null)
//                while (headerNames.hasMoreElements())
//                {
//                    String headerName = headerNames.nextElement();
//                    params.put(headerName, request.getHeader(headerName));
//                }
//
//            Enumeration<String> reqParams = request.getParameterNames();
//            if (reqParams!=null)
//                while (reqParams.hasMoreElements())
//                {
//                    String paramName = reqParams.nextElement();
//                    params.put(paramName, request.getParameter(paramName));
//                }
//
//            if (isPut)
//            {
//                params.put(
//                        NetworkResponseService.REQUEST_CONTENT_PARAMETER, request.getInputStream());
////                BufferedReader reader = new BufferedReader(
////                        new InputStreamReader(request.getInputStream()));
////                try
////                {
////                    String line;
////                    while ((line=reader.readLine())!=null)
////                        System.out.println("PUT>>> "+line);
////                }
////                finally
////                {
////                    reader.close();
////                }
//            }
////            String result = null;
//            String result = responseService.getResponse(
//                    context, request.getRemoteAddr(), params);
//            if (!isPut)
//            {
//                String charset = null;
//                String charsetsStr = request.getHeader("Accept-Charset");
//                if (charsetsStr!=null)
//                {
//                    String[] charsets = charsetsStr.split("\\s*,\\s*");
//                    if (charsets!=null && charsets.length>0)
//                    {
//                        charset = charsets[0].split(";")[0];
//                        getServletContext().log(
//                                String.format("Charset (%s) selected from response", charset));
//                    }
//                }
//                if (charset==null)
//                {
//                    getServletContext().log(
//                            "Can't detect charset from request. Using default charset (UTF-8)");
//                    charset = "UTF-8";
//                }
//                response.setCharacterEncoding(charset);
//                PrintWriter out = response.getWriter();
//                try
//                {
//                    out.append(result);
//                }
//                finally
//                {
//                    out.close();
//                }
//            }
//            response.setStatus(
//                    isPut? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_OK);
//        }
//        catch(Throwable e)
//        {
//            if (e instanceof NetworkResponseServiceUnavailableException)
//                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
//            else if (e instanceof ContextUnavailableException)
//                response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
//            else if (e instanceof AccessDeniedException)
//                response.sendError(HttpServletResponse.SC_FORBIDDEN);
//            else if (e instanceof RequiredParameterMissedException)
//                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
//            else
//                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
//        }
//        response.flushBuffer();
//    }
}

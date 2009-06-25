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
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.tapestry5.ioc.Registry;
import org.raven.net.AccessDeniedException;
import org.raven.net.Authentication;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceUnavailableException;
import org.raven.net.RequiredParameterMissedException;
import org.raven.ui.util.RavenRegistry;

/**
 *
 * @author Mikhail Titov
 */
public class NetworkResponseServlet extends HttpServlet
{
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        Registry registry = RavenRegistry.getRegistry();
        NetworkResponseService responseService = registry.getService(NetworkResponseService.class);
        try
        {
            String context = request.getPathInfo();
            if (context.length()<2)
            {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            context = context.substring(1);

            Authentication contextAuth = responseService.getAuthentication(context);
            if (contextAuth!=null)
            {
                String requestAuth = request.getHeader("Authorization");
                if (requestAuth==null)
                {
                    response.setHeader(
                            "WWW-Authenticate", "BASIC realm=\"RAVEN simple request interface\"");
                    response.sendError(response.SC_UNAUTHORIZED);
                    return;
                }
                else
                {
                    String userAndPath = new String(Base64.decodeBase64(
                            requestAuth.substring(6).getBytes()));
                    String elems[] = userAndPath.split(":");
                    if (elems.length!=2 
                        || !contextAuth.getUser().equals(elems[0])
                        || !contextAuth.getPassword().equals(elems[1]))
                    {
                        throw new AccessDeniedException();
                    }
                }
            }

            Map<String, Object> params = new HashMap<String, Object>();

            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames!=null)
                while (headerNames.hasMoreElements())
                {
                    String headerName = headerNames.nextElement();
                    params.put(headerName, request.getHeader(headerName));
                }

            Enumeration<String> reqParams = request.getParameterNames();
            if (reqParams!=null)
                while (reqParams.hasMoreElements())
                {
                    String paramName = reqParams.nextElement();
                    params.put(paramName, request.getParameter(paramName));
                }

            boolean isPut = request.getMethod().equalsIgnoreCase("PUT");
            if (isPut)
                params.put(
                        NetworkResponseService.REQUEST_CONTENT_PARAMETER, request.getInputStream());
            
            String result = responseService.getResponse(
                    context, request.getRemoteAddr(), params);
            if (!isPut)
            {
                String charset = null;
                String charsetsStr = request.getHeader("Accept-Charset");
                if (charsetsStr!=null)
                {
                    String[] charsets = charsetsStr.split("\\s*,\\s*");
                    if (charsets!=null && charsets.length>0)
                    {
                        charset = charsets[0].split(";")[0];
                        getServletContext().log(
                                String.format("Charset (%s) selected from response", charset));
                    }
                }
                if (charset==null)
                {
                    getServletContext().log(
                            "Can't detect charset from request. Using default charset (UTF-8)");
                    charset = "UTF-8";
                }
                response.setCharacterEncoding(charset);
                PrintWriter out = response.getWriter();
                try
                {
                    out.append(result);
                }
                finally
                {
                    out.close();
                }
            }
            else
            {
//                response.sendError(response.SC_OK);
                PrintWriter out = response.getWriter();
                try
                {
                    out.append("OK");
                }
                finally
                {
                    out.close();
                }
            }
        }
        catch(Throwable e)
        {
            if (e instanceof NetworkResponseServiceUnavailableException)
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
            else if (e instanceof ContextUnavailableException)
                response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            else if (e instanceof AccessDeniedException)
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            else if (e instanceof RequiredParameterMissedException)
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

}

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
import org.apache.tapestry.ioc.Registry;
import org.raven.net.AccessDeniedException;
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

            Map<String, Object> params = null;
            Enumeration<String> reqParams = request.getParameterNames();
            if (reqParams!=null)
            {
                params = new HashMap<String, Object>();
                while (reqParams.hasMoreElements())
                {
                    String paramName = reqParams.nextElement();
                    params.put(paramName, request.getParameter(paramName));
                }
            }
            String result = responseService.getResponse(
                    context, request.getRemoteAddr(), params);
            response.setCharacterEncoding("UTF-8");
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
    public String getServletInfo()
    {
        return "Simple requests interface";
    }

}

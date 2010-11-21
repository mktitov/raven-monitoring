/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.server.app.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.raven.server.app.App;
import org.raven.server.app.service.RavenServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class RavenFilter implements Filter
{
    private RavenServletContext ravenServletContext;
    
    public void init(FilterConfig filterConfig) throws ServletException
    {
        filterConfig.getServletContext().log("Initializing raven...");
        App.createRegistry();
        ravenServletContext = App.REGISTRY.getService(RavenServletContext.class);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException
    {
        ravenServletContext.setContext((HttpServletRequest)request, (HttpServletResponse)response);
        try
        {
            chain.doFilter(request, response);
        } 
        finally
        {
            ravenServletContext.removeContext();
        }
    }

    public void destroy()
    {
        App.shutdownRegistry();
    }
}

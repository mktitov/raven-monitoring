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
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.binary.Base64;
import org.raven.auth.AuthService;
import org.raven.auth.UserContext;
import org.raven.auth.UserContextConfiguratorService;
import org.raven.net.AccessDeniedException;
import org.raven.server.app.App;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class LoginFilter implements Filter
{
    @Service
    private static AuthService authService;
    
    @Service
    private static UserContextConfiguratorService userContextConfiguratorService;

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) 
            throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String requestAuth = request.getHeader("Authorization");
        if (requestAuth==null)
        {
            response.setHeader("WWW-Authenticate", "BASIC realm=\"RAVEN authentication service\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        else
        {
            try{
                String userAndPath = new String(Base64.decodeBase64(
                        requestAuth.substring(6).getBytes()));
                String elems[] = userAndPath.split(":");
                if (elems.length!=2)
                    throw new AccessDeniedException();
                String username = elems[0];
                String password = elems[1];

                UserContext context = authService.authenticate(username, password);
                if (context==null)
                    throw new AccessDeniedException();

                userContextConfiguratorService.configure(context);

                HttpSession session = request.getSession();
                session.setAttribute(App.USER_CONTEXT_SESSION_ATTR, context);
            }catch(Throwable e){
                if (e instanceof AccessDeniedException)
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        chain.doFilter(req, resp);
    }

    public void destroy()
    {
    }
}

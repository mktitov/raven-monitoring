/*
 * Copyright 2013 Mikhail Titov.
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
package org.raven.ui.filter;

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
import org.apache.tapestry5.ioc.Registry;
import org.raven.auth.LoginException;
import org.raven.auth.LoginManager;
import org.raven.auth.UserContext;
import org.raven.auth.impl.AccessControl;
import org.raven.auth.impl.SystemLoginService;
import org.raven.conf.Configurator;
import org.raven.tree.Tree;
import org.raven.ui.util.RavenRegistry;

/**
 *
 * @author Mikhail Titov
 */
public class LoginFilter implements Filter {
    public final static String USER_CONTEXT_ATTR = "raven_user_context";
    private LoginManager loginManager;
    private Tree tree;

    public void init(FilterConfig filterConfig) throws ServletException {
    	Registry registry = RavenRegistry.getRegistry();
        tree = registry.getService(Tree.class);
        loginManager = registry.getService(LoginManager.class);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
        throws IOException, ServletException 
    {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;
        if (!req.getPathInfo().startsWith("/logout")) {
            HttpSession session = req.getSession();
            UserContext user = (UserContext) session.getAttribute(USER_CONTEXT_ATTR);
            if (user==null && !checkAuth(req, resp, session))
                return;
        }
        chain.doFilter(request, response);
    }

    public void destroy() {
    	Registry registry = RavenRegistry.getRegistry();
        Tree tree = registry.getService(Tree.class);
        tree.shutdown();
        Configurator configurator = registry.getService(Configurator.class);
        try {
            configurator.close();
        } catch (Exception ex) { }
        registry.shutdown();
    }

    private boolean checkAuth(HttpServletRequest request, HttpServletResponse response, HttpSession session) 
        throws IOException 
    {
        String requestAuth = request.getHeader("Authorization");
        if (requestAuth!=null) {
            String userAndPath = new String(Base64.decodeBase64(requestAuth.substring(6).getBytes()));
            String elems[] = userAndPath.split(":");
            if (elems.length==2) {
                try {
                    UserContext user = loginManager.getLoginService(SystemLoginService.NAME).login(
                        elems[0], elems[1], request.getRemoteAddr(), null);
                    if (user.getAccessForNode(tree.getRootNode())==AccessControl.NONE) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return false;
                    } else {
                        session.setAttribute(USER_CONTEXT_ATTR, user);
                        session.getServletContext().log(String.format("User (%s) successfully logged in", user.getLogin()));
                        return true;
                    }
                } catch (LoginException ex) {
                    session.getServletContext().log("Auth error", ex);
                }                
            }
            session.getServletContext().log("Authentification failed for user");
        }
        response.setHeader("WWW-Authenticate", "BASIC realm=\"RAVEN\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return false;    
    }
}

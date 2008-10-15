/*
 *  Copyright 2008 Sergey Pinevskiy.
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

package org.raven.ui.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.tapestry.ioc.Registry;
import org.raven.ui.util.RavenRegistry;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.tree.Tree;

import jcifs.http.NtlmHttpFilter;
//import javax.servlet.http.HttpServletResponse;

public class JcifsFilter extends NtlmHttpFilter {

   public static final String WINS = "jcifs.netbios.wins";
   public static final String DOMAIN = "jcifs.smb.client.domain";
   public static final String USERNAME = "jcifs.smb.client.username";
   public static final String PASSWORD = "jcifs.smb.client.password";
   public static final String CONTROLLER = "jcifs.http.domainController";
   
   public static final String[] ravenParams = {	Configurator.WINS_SERVERS,
						Configurator.WIN_DOMAIN,
						Configurator.ACCOUNT_NAME,
						Configurator.BIND_PASSWORD,
						Configurator.DOMAIN_CONTROLLER};
   public static final String[] jcifsParams  = {WINS,DOMAIN,USERNAME,PASSWORD,CONTROLLER};
   private boolean testMode = false;
	
    public void init(FilterConfig filterConfig ) throws ServletException 
    {
    	Registry registry = RavenRegistry.getRegistry();
		Configurator configurator = registry.getService(Configurator.class);
		Config config;
		try { config = configurator.getConfig(); }
        catch(Exception e) { throw new ServletException("init filter: " + e.getMessage()); }
        String param;
        for(int i=0;i<ravenParams.length;i++)
        {
        	param = config.getStringProperty(ravenParams[i], null);
            if(param!=null) jcifs.Config.setProperty(jcifsParams[i], param);        	
        }
        testMode = config.getBooleanProperty(Configurator.TEST_MODE, Boolean.FALSE);
        if(!testMode) super.init(filterConfig);
    }
    
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain ) 
    throws IOException, ServletException
    {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
    	if(!testMode) super.doFilter(request, response, chain);
    	else
    	{
            HttpServletRequest req = (HttpServletRequest)request;
            HttpSession ses =  req.getSession();
            Object obj = ses.getAttribute(AuthFilter.NTLM_AUTH);
            if(obj==null) ses.setAttribute(AuthFilter.NTLM_AUTH,AuthFilter.TEST_DOMAIN+"\\"+AuthFilter.TEST_USER);
    		chain.doFilter( request, response );
    	}
    }
    
    public void destroy()
    {
    	Registry registry = RavenRegistry.getRegistry();
        Tree tree = registry.getService(Tree.class);
        tree.stop(tree.getRootNode());
    	super.destroy();
    }

}

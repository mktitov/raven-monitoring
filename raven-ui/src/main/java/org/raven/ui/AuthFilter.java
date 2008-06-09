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

package org.raven.ui;

import org.apache.tapestry.ioc.Registry;
import org.raven.RavenRegistry;
import org.raven.conf.Config;
import org.raven.conf.Configurator;

import org.raven.conf.impl.UserAcl;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;

public class AuthFilter implements Filter 
{
	public static final String NTLM_AUTH = "NtlmHttpAuth";
	public static final String USER_ACL = "UserAcl";
	public static final String TEST_DOMAIN = "TEST";
	public static final String TEST_USER = "test";
	private Config config = null;
	private String domain = ""; 
	private boolean first=true;
	private boolean testMode = false;
	
    public void init(FilterConfig filterConfig ) throws ServletException 
    {
    	Registry registry = RavenRegistry.getRegistry();
		org.raven.conf.Configurator configurator = registry.getService(Configurator.class);
		try { config = configurator.getConfig(); }
        catch(Exception e) { throw new ServletException("init filter: " + e.getMessage(), e); }
        domain = config.getStringProperty(Configurator.WIN_DOMAIN, domain);
        testMode = config.getBooleanProperty(Configurator.TEST_MODE, Boolean.FALSE);
        if(testMode) domain = TEST_DOMAIN;
    }

    public void destroy() { }
    
    public void parm(javax.servlet.ServletContext c)
    {
    	if(!first) return;
    	first=false;
    	c.log(Configurator.ACCOUNT_NAME+" "+config.getStringProperty(Configurator.ACCOUNT_NAME, null));
    	c.log(Configurator.BIND_NAME+" "+config.getStringProperty(Configurator.BIND_NAME, null));
    	c.log(Configurator.BIND_PASSWORD+" "+config.getStringProperty(Configurator.BIND_PASSWORD, null));
    	c.log(Configurator.DOMAIN_CONTROLLER+" "+config.getStringProperty(Configurator.DOMAIN_CONTROLLER, null));
    	c.log(Configurator.PROVIDER_URL+" "+config.getStringProperty(Configurator.PROVIDER_URL, null));
    	c.log(Configurator.SEARCH_CONTEXT+" "+config.getStringProperty(Configurator.SEARCH_CONTEXT, null));
    	c.log(Configurator.WIN_DOMAIN+" "+config.getStringProperty(Configurator.WIN_DOMAIN, null));
    	c.log(Configurator.WINS_SERVERS+" "+config.getStringProperty(Configurator.WINS_SERVERS, null));
    }

    public void doFilter( ServletRequest request,
                ServletResponse response,
                FilterChain chain ) throws IOException, ServletException 
    {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;
        HttpSession ses =  req.getSession();
        Object obj = ses.getAttribute(USER_ACL);
        boolean ok = true;
        while(true)
        {
            if(obj!=null) break;
            ok = false;
            obj = ses.getAttribute(NTLM_AUTH);
            if(obj==null)
            {
            	ses.getServletContext().log(NTLM_AUTH+" is null !");
            	break;
            }
           	String domainAccount = obj.toString();
            ses.getServletContext().log(NTLM_AUTH+" found "+domainAccount);
            parm(ses.getServletContext());
           	String[] da = domainAccount.split("\\\\");
           	String account = "";
            if(da.length!=2 || !da[0].equalsIgnoreCase(domain)) break;
           	account = da[1];
            ses.getServletContext().log("Account "+account);
           	
           	UserAcl ua = new UserAcl(account,config);
           	if(ua.isEmpty()) break;
           	ses.getServletContext().log("UA is not empty !");
           	ses.getServletContext().log("UA = "+ua.toString());
           	ok = true;
           	ses.setAttribute(USER_ACL, ua);
           	break;
        }
        if(ok) chain.doFilter( req, resp );
        return;
    }


    public void setFilterConfig( FilterConfig f ) 
    {
        try { init( f ); }
        catch( Exception e ) { e.printStackTrace(); }
    }

    public FilterConfig getFilterConfig() { return null; }

}

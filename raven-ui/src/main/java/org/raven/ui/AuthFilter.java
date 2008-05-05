package org.raven.ui;

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
import org.apache.tapestry.ioc.Registry;

import java.io.IOException;

public class AuthFilter implements Filter 
{
	public static final String NTLM_AUTH = "NtlmHttpAuth";
	public static final String USER_ACL = "UserAcl";
	private Config config = null;
	private String domain = ""; 
	
    public void init(FilterConfig filterConfig ) throws ServletException 
    {
		Registry registry = RavenRegistry.getRegistry();
        Configurator configurator = registry.getService(Configurator.class);
        try { config = configurator.getConfig(); } 
        catch(Exception e) { throw new ServletException("init filter: " + e.getMessage()); }
        domain = config.getStringProperty(Configurator.WIN_DOMAIN, domain);
    }

    public void destroy() { }

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
            if(obj==null) break;
           	String domainAccount = obj.toString();
           	String[] da = domainAccount.split("\\\\");
           	String account = "";
            if(da.length!=2 || !da[0].equalsIgnoreCase(domain)) break;
           	account = da[1];
           	UserAcl ua = new UserAcl(account,config);
           	if(ua.isEmpty()) break;
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

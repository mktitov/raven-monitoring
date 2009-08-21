package org.raven.ui;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionListener implements HttpSessionListener 
{
		public SessionListener() 
		{
		}
		
		public void sessionCreated(HttpSessionEvent event) 
		{
		}
		
		public void sessionDestroyed(HttpSessionEvent event) 
		{
			HttpSession s = event.getSession();
			try {
				SessionBean sb = (SessionBean) s.getAttribute(SessionBean.BEAN_NAME);
				if(sb!=null) sb.onSessionStop();
			} catch(Exception e) {
				s.getServletContext().log("on sessionDestroyed: ", e);
			}
		}
	
}

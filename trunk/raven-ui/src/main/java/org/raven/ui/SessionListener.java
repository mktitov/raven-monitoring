package org.raven.ui;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.raven.audit.Action;
import org.raven.audit.AuditRecord;
import org.raven.audit.Auditor;
import org.raven.auth.UserContext;
import org.raven.auth.UserContextService;
import org.raven.tree.Node;
import org.raven.ui.util.RavenRegistry;

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
                else {
                    final UserContext userContext = (UserContext) s.getAttribute(
                            UserContextService.USER_CONTEXT_SESSION_ATTR);
                    final Node serviceNode = (Node) s.getAttribute(UserContextService.SERVICE_NODE_SESSION_ATTR);
                    if (userContext!=null) {
                        final Auditor auditor = RavenRegistry.getRegistry().getService(Auditor.class);
                        auditor.write(new AuditRecord(
                                serviceNode, userContext.getLogin(), userContext.getHost(), Action.SESSION_STOP, null));
                    }
                }
			} catch(Exception e) {
				s.getServletContext().log("on sessionDestroyed: ", e);
			}
		}
	
}

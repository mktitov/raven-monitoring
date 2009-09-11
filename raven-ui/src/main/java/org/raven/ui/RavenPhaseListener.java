package org.raven.ui;

//import java.io.IOException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RavenPhaseListener implements PhaseListener 
{
	private static Logger logger = LoggerFactory.getLogger(RavenPhaseListener.class);
	private static final long serialVersionUID = 1L;
	public static final String ON_EXPIRED = "logout";
	public static final String POSTFIX = ".html";
	public static final String INDEX = "/index.jspx";
	public static final String PATTERN = "/"+ON_EXPIRED+"(_[a-z][a-z])?\\"+POSTFIX;
	public void afterPhase(PhaseEvent e) 
	{
	}

	public void beforePhase(PhaseEvent e) 
	{
		if (e.getPhaseId() == PhaseId.RESTORE_VIEW) 
		{
			FacesContext fc = e.getFacesContext();
			ExternalContext ec = fc.getExternalContext();
			HttpSession s = (HttpSession)ec.getSession(false);
			if (s == null || s.getAttribute(SessionBean.BEAN_NAME)==null)
			{
				HttpServletRequest r = (HttpServletRequest) ec.getRequest();
				String p = r.getPathInfo();
				if(!p.equals(INDEX)) 
					if(!p.matches(PATTERN))
				{
					String ret = SessionBean.getOutcomeWithLang(fc, ON_EXPIRED)+POSTFIX;
					try {
						//HttpServletResponse resp = (HttpServletResponse)ec.getResponse();
						//resp.setHeader("Window-target", "_top");
						//resp.setHeader("Window-target", "_parent");
						ec.redirect(ret);
						//resp.sendRedirect(ret);
						//resp.setHeader("Window-target","_top");
						//resp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
						//resp.setHeader("Location", ret);
						//resp.sendError(HttpServletResponse.SC_MOVED_TEMPORARILY, ret);
					} catch (Exception e1) {
						logger.info("on redirect:", e1);
					}
				}
				
			}	
		}
	}
	
	public PhaseId getPhaseId() 
	{
		 return PhaseId.RESTORE_VIEW;
	}

}

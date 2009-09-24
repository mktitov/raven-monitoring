package org.raven.ui;

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
	
	public void afterPhase(PhaseEvent event) 
	{
		FacesContext fc=event.getFacesContext();
		UIViewRoot vr = fc.getViewRoot();
		if(vr==null) return;
		String rootId = vr.getViewId();
		if(rootId==null) return;
		String key = rootId; 
		if(!key.startsWith(IconResource.RES_SIGN2)) return;
		key=rootId.substring(1);
		
		IconResource ir = ResourcesCache.getInstance().get(key);
	    if(ir==null || !ir.isValid())
	    {
	    	fc.responseComplete();
	    	return;
	    }	
		HttpServletResponse resp = (HttpServletResponse) fc.getExternalContext().getResponse();
		try {
		      resp.setContentType(ir.getMimeType());
		      resp.setStatus(HttpServletResponse.SC_OK);
		      ServletOutputStream os = resp.getOutputStream();
		      os.write(ir.getData());
		      os.flush();
		      os.close();
		} catch (Exception e) { logger.error("afterPhase:",e); }
		finally { fc.responseComplete(); }
	}
	
	public void beforePhase(PhaseEvent event) 
	{
		//Redirect to logout page if session has been expired.
		if (event.getPhaseId() != PhaseId.RESTORE_VIEW) return; 
		FacesContext fc = event.getFacesContext();
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
				try { ec.redirect(ret);	} 
				catch (Exception e) {
					logger.info("on redirect:", e);
				}
			}
		}	
	}
	
	public PhaseId getPhaseId() 
	{
		 return PhaseId.RESTORE_VIEW;
	}

}

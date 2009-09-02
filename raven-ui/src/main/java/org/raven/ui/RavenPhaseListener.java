package org.raven.ui;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

public class RavenPhaseListener implements PhaseListener 
{
	private static final long serialVersionUID = 1L;
	public static final String EXPIRED = "sessionExpired";

	public void afterPhase(PhaseEvent arg0) 
	{
	}

	public void beforePhase(PhaseEvent e) 
	{
		if (e.getPhaseId() == PhaseId.RESTORE_VIEW) 
		{
			FacesContext fc = e.getFacesContext();
			if (fc.getExternalContext().getSession(false) == null)
			{
				fc.getExternalContext().log("!!!EXPIRED!!!");
				String ret = SessionBean.getOutcomeWithLang(fc, EXPIRED);
				fc.getApplication().getNavigationHandler().handleNavigation(fc, "", ret);
			}	
		}
	}
	
	public PhaseId getPhaseId() 
	{
		 return PhaseId.RESTORE_VIEW;
	}

}

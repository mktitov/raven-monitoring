package org.raven.ui.node;

import org.apache.myfaces.trinidad.context.RequestContext;
//import org.apache.myfaces.trinidad.event.ReturnEvent;
import org.raven.ui.SessionBean;

public class RenameNodeBean 
{
	public static final String BEAN_NAME = "renameNode";
	private String name = "";

/*	public void handleReturn(ReturnEvent event)
	{
		SessionBean sb = (SessionBean) SessionBean.getElValue(SessionBean.BEAN_NAME);
		sb.reloadBothFrames();
	}
*/	 
	
	public static RenameNodeBean getInstance()
	{
		return (RenameNodeBean) SessionBean.getElValue(BEAN_NAME);
	}
	
	public String save()
	{
		if(name==null || name.length()==0) 
			return cancel();
		RequestContext.getCurrentInstance().returnFromDialog(name, null);
		return null;
	}
		
	public String cancel()
	{
		RequestContext.getCurrentInstance().returnFromDialog(null, null);
		return null;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	
}

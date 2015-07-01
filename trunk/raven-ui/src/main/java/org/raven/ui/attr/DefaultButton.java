package org.raven.ui.attr;

import javax.faces.context.FacesContext;

import org.apache.myfaces.trinidad.component.core.nav.CoreCommandButton;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
public class DefaultButton
{
//    private Logger logger = LoggerFactory.getLogger(DefaultButton.class);	
	
	private CoreCommandButton button = null;

	public void setButton(CoreCommandButton c)
	{
//		logger.info("setButton - old:{} new:{}",button,c);
		button = c;
	}

	
	public CoreCommandButton getButton() 
	{
//		logger.info("getButton: {}",button);
		return button;
	}


	public String getId()
	{
//		logger.info("getId:{}",button.getId());
//		logger.info("getClientId:{}",button.getClientId(FacesContext.getCurrentInstance()));
//		logger.info("getContainerClientId:{}",button.getContainerClientId(FacesContext.getCurrentInstance()));
		return button.getClientId(FacesContext.getCurrentInstance());
	}
}

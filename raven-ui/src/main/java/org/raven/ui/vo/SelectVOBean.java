package org.raven.ui.vo;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import javax.faces.component.UIComponent;
import javax.faces.event.ActionEvent;

import org.apache.myfaces.trinidad.context.RequestContext;
import org.apache.myfaces.trinidad.event.ReturnEvent;
import org.raven.tree.ViewableObject;
import org.raven.ui.SessionBean;

public class SelectVOBean 
{
	public static final String PARAM_NAME = "selectVO";
	private Queue<ViewableObjectWrapper> queue = new LinkedList<ViewableObjectWrapper>();
	private ViewableObjectWrapper object = null;
	
	public void select(ActionEvent event)
	{
		  UIComponent component=event.getComponent();
		  if(component==null)
		  {
			 // logger.info("component==null");
			  return;
		  }	  
		  Map<String, Object> params = component.getAttributes();
		  ViewableObject vo = (ViewableObject)params.get(PARAM_NAME);
		  queue.add(new ViewableObjectWrapper(vo));
		  object = null;
	}
	
	public void handleReturn(ReturnEvent event)
	{
		SessionBean.getInstance().reloadBothFrames();
	}

	public String cancel()
	{
		RequestContext.getCurrentInstance().returnFromDialog(null, null);
		return null;
	}
	
	public String getParamName()
	{
		return PARAM_NAME;
	}
	
	public ViewableObjectWrapper getObject()
	{
		ViewableObjectWrapper vo = queue.poll();
		if(vo!=null) object = vo;
		return object;
	}
	
}

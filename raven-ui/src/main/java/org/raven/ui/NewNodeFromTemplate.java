package org.raven.ui;

//import java.util.Collection;
//import java.util.Iterator;

import javax.faces.context.FacesContext;

import org.apache.myfaces.trinidad.context.RequestContext;
import org.apache.myfaces.trinidad.event.ReturnEvent;
import org.raven.template.TemplateNode;
import org.raven.template.TemplateWizard;
import org.raven.tree.Node;
//import org.raven.tree.NodeAttribute;
import org.weda.constraints.ConstraintException;

public class NewNodeFromTemplate 
{
	TemplateWizard wizard = null;
	NodeWrapper wrapper = null;
	
	public void init(TemplateNode template, Node node,String name)
	{
		wizard = new TemplateWizard(template,node,name);
		wrapper = new NodeWrapper(wizard.getVariablesNode());
	}
	
	public void clear() 
	{ 
		wizard = null;
		wrapper = null;
	}
	
	 public void handleReturn(ReturnEvent event)
	  {
	    //Object returnedValue = event.getReturnValue();
		    FacesContext context = FacesContext.getCurrentInstance();
		    SessionBean sb = (SessionBean) context.getELContext().getELResolver().getValue(context.getELContext(), null, SessionBean.BEAN_NAME);
		    sb.reloadBothFrames();
	  }

	public String write()
	{
		try { wizard.createNodes(); } 
		catch(ConstraintException e) { return cancel();} 
	    RequestContext.getCurrentInstance().returnFromDialog(null, null);
		return null;
	}
	
	public String cancel()
	{
		wizard.cancelWizard();
	    RequestContext.getCurrentInstance().returnFromDialog(null, null);
		return null;
	}

	public NodeWrapper getWrapper() { return wrapper; }
	public void setWrapper(NodeWrapper wrapper) { this.wrapper = wrapper; }

}

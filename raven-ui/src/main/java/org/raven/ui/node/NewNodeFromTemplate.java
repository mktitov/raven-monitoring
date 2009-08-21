package org.raven.ui.node;

import org.apache.myfaces.trinidad.context.RequestContext;
import org.apache.myfaces.trinidad.event.ReturnEvent;
import org.raven.audit.Action;
import org.raven.template.impl.TemplateWizard;
import org.raven.template.impl.TemplateNode;
import org.raven.tree.Node;
import org.raven.ui.SessionBean;
import org.slf4j.helpers.MessageFormatter;
import org.weda.constraints.ConstraintException;

public class NewNodeFromTemplate 
{
	TemplateWizard wizard = null;
	NodeWrapper wrapper = null;
	TemplateNode template = null;
	
	public void init(TemplateNode template, Node node,String name)
	{
		this.template = template;
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
	    SessionBean sb = (SessionBean) SessionBean.getElValue(SessionBean.BEAN_NAME);
	    sb.reloadBothFrames();
	  }

	public String write()
	{
		wrapper.save(false);
		try { 
			wizard.createNodes();
			String mes = MessageFormatter.format("template: id='{}' path='{}'"+template.getPath(),template.getId(),template.getPath());
			SessionBean.getInstance().getAuditor().write(wrapper.getNode(), SessionBean.getUserAcl().getAccountName(), Action.NODE_CREATE, mes);            	  
		} 
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

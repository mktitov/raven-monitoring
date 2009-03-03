package org.raven.ui.attr;

import javax.faces.event.ValueChangeEvent;
import org.apache.myfaces.trinidad.component.core.output.CoreMessage;
//import org.apache.myfaces.trinidad.model.RowKeySet;
//import javax.faces.component.UIComponent;
//import org.apache.myfaces.trinidad.component.UIXTable;
//import java.util.Iterator;
//import java.util.List;
//import javax.faces.event.ActionEvent;
import org.raven.ui.SessionBean;
import org.raven.ui.node.NodeWrapper;

public class RefreshAttributesTableBean {
	public static final String BEAN_NAME = "cNodeRefreshAttrs";
	private CoreMessage message = null;
	
	public void saveAttributes(ValueChangeEvent vce)	
	{
		saveAttributes();
	}
	public String saveAttributes()
	{
		NodeWrapper nw = SessionBean.getNodeWrapper();
		String ret = nw.saveRefreshAttributes();
		if(ret!=null && message!=null) message.setMessage(ret);
		else message.setMessage("");
		nw.onRefresh();
		return "";
	}
	
	public CoreMessage getMessage() 
	{ 
		return message; 
	}

	public void setMessage(CoreMessage message) 
	{ 
		this.message = message; 
		//this.message.setMessage("");
	}

	//public void setMessageText(String message) { this.message.setMessage(message); }

}

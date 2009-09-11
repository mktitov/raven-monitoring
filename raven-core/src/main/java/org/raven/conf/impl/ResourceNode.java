package org.raven.conf.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
//import org.raven.tree.Node.Status;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ViewableObjectImpl;

@NodeClass(childNodes=org.raven.conf.impl.AccessControlNode.class, 
		parentNode=org.raven.conf.impl.ResourcesListNode.class)
public class ResourceNode extends BaseNode implements Viewable 
{
	@Parameter
	private String title;

	@Parameter
	private String dsc;
	
	@Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
	private Node show;
	
	public Boolean getAutoRefresh() 
	{
		return true;
	}

	public Map<String, NodeAttribute> getRefreshAttributes() throws Exception 
	{
		return null;
	}

	public static StringBuffer appendParam(StringBuffer buf,String parName,String parValue)
	{
		buf.append(parName);
		buf.append(AccessControl.DELIMITER);
		buf.append(parValue);
		buf.append(AccessControl.EXPRESSION_DELIMITER);
		return buf;
	}
	
	public String getResourceString()
	{
		if (getStatus()!=Status.STARTED)
		    return "";
		StringBuffer sb = new StringBuffer();
		appendParam(sb, AccessControlList.NAME_PARAM, getName());

		if(title!=null)
			appendParam(sb, AccessControlList.TITLE_PARAM, title);
		
		if(show!=null)
			appendParam(sb, AccessResource.SHOW_PARAM, show.getName());

		for(Node n: getChildrenList())
		{
			AccessControlNode acn = (AccessControlNode) n;
			String acStr = acn.getACString();
			if(acStr!=null && acStr.length()>0)
				appendParam(sb, AccessResource.AC_PARAM, acStr);
		}
		
		return sb.toString();
	}
	
	public List<ViewableObject> getViewableObjects(
			Map<String, NodeAttribute> refreshAttributes) throws Exception 
	{
		if (getStatus()!=Status.STARTED)
		    return null;
		ViewableObject textObj = 
			new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, getResourceString());
		return Arrays.asList(textObj);		
	}

	public void setTitle(String title) {
		String t = title.replaceAll(AccessControl.disabledInParValues, "");
		this.title = t ;//title.replaceAll(AccessControl.disabledInParValues, "");
	}

	public String getTitle() {
		return title;
	}

	public void setShow(Node show) {
		this.show = show;
	}

	public Node getShow() {
		return show;
	}

	public void setDsc(String dsc) {
		this.dsc = dsc.replaceAll(AccessControl.disabledInParValues, "");
	}

	public String getDsc() {
		return dsc;
	}

}

package org.raven.auth.impl;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.util.NodeUtils;

@NodeClass(childNodes=org.raven.auth.impl.AccessControlNode.class)
public class ResourceNode extends BaseNode implements Viewable 
{
	public static final String PREFIX = LdapGroupAcl.RESOURCE_PARAM+AccessControl.DELIMITER+" ";
	
//	@Parameter
//	private String title;

	@Parameter
	private String dsc;

	@Parameter(defaultValue="true")
	private Boolean showInResources;
	
	@Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
	private Node show;

	
	public Boolean getAutoRefresh() {
		return true;
	}

	public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
		return null;
	}

	public static StringBuffer appendParam(StringBuffer buf,String parName,String parValue) {
		buf.append(parName);
		buf.append(AccessControl.DELIMITER);
		if(AccessControlList.AC_PARAM.equals(parName))
				buf.append(parValue);
		else
			buf.append(AccessControl.removeDeniedSymbols(parValue));
		buf.append(AccessControl.EXPRESSION_DELIMITER);
		return buf;
	}
    
    public AccessResource getAccessResource() {
        LinkedList<AccessControl> accessControls = new LinkedList<AccessControl>();
        for (AccessControlNode node: NodeUtils.getChildsOfType(this, AccessControlNode.class))
            accessControls.addAll(node.getAccessControls());
        
        return null;
    }
	
	public String getResourceString()	{
		if (getStatus()!=Status.STARTED)
		    return "";
		StringBuffer sb = new StringBuffer();
		appendParam(sb, AccessControlList.NAME_PARAM, getName());

		if(showInResources)
			appendParam(sb, AccessControlList.TITLE_PARAM, getName());
		
		if(show!=null)
			appendParam(sb, AccessResource.SHOW_PARAM, show.getPath());

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

	public String getTitle() {
		if(showInResources!=null && showInResources==true ) 
			return  getName();
		return null;
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
	
	public String getPrefix()
	{
		return PREFIX;
	}

	public Boolean getShowInResources() {
		return showInResources;
	}

	public void setShowInResources(Boolean showInResources) {
		this.showInResources = showInResources;
	}

}

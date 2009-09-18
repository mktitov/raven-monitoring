package org.raven.conf.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ViewableObjectImpl;
//import org.weda.annotations.constraints.NotNull;
//import org.raven.annotations.Parameter;

@NodeClass(parentNode=org.raven.conf.impl.AccessGroupNode.class)
public class AccessUserNode extends BaseNode implements Viewable 
{
	public static final String prefix = LdapGroupAcl.USER_PARAM+" ";
	public static final String PREFIX = LdapGroupAcl.USER_PARAM+AccessControl.DELIMITER+" ";

	/*
	@Parameter
	@NotNull
	private String user;

	public String toString()
	{
		return user;
	}
*/	
	
	public AccessUserNode()
	{
		super();
	}
	
	
	public List<ViewableObject> getViewableObjects(
			Map<String, NodeAttribute> refreshAttributes) throws Exception 
	{
		if (getStatus()!=Status.STARTED)
		    return null;
		ViewableObject textObj = 
			new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, prefix+getName());
		return Arrays.asList(textObj);		
	}

	public Boolean getAutoRefresh() 
	{
		return true;
	}

	public Map<String, NodeAttribute> getRefreshAttributes() throws Exception 
	{
		return null;
	}

	public String getPrefix()
	{
		return PREFIX;
	}
	
}

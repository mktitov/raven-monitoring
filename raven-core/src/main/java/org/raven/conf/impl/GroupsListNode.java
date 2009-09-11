package org.raven.conf.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
//import org.raven.tree.Node;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ViewableObjectImpl;

@NodeClass(childNodes=org.raven.conf.impl.AccessGroupNode.class,
		parentNode=org.raven.conf.impl.AuthorizationNode.class)
public class GroupsListNode extends BaseNode implements Viewable
{
	public static final String NODE_NAME = "Groups";
	public static final int START_NUM = 50000;
	
	public GroupsListNode()
	{
		super(NODE_NAME);
	}
	
	public String getAllGroupsString(boolean v)
	{
		StringBuffer sb = new StringBuffer();
//		ResourceNode.appendParam(sb, LdapGroupAcl.LDAP_GROUP_PARAM, ldapGroup);
		if(v) sb.append(ResourcesListNode.LB);
		int i = START_NUM;
		for(Node n: getChildrenList())
		{
			if(n.getStatus()!=Status.STARTED) continue;
			AccessGroupNode r = (AccessGroupNode) n;
			if(v) sb.append(ResourcesListNode.IB);
				else sb.append(GroupsAclStorage.GROUP_PARAM_NAME + i++ + "=");
			sb.append(r.getGroupString());
			if(v) sb.append(ResourcesListNode.IE);
				else sb.append("\n");
		}
		if(v) sb.append(ResourcesListNode.LE);
		
		return sb.toString();
	}
	
	public List<ViewableObject> getViewableObjects(
			Map<String, NodeAttribute> refreshAttributes) throws Exception 
	{
		if (getStatus()!=Status.STARTED)
		    return null;
		ViewableObject textObj = 
			new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, getAllGroupsString(true));
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
	

}

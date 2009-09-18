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
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.constraints.NotNull;

@NodeClass(childNodes={org.raven.conf.impl.ResourceLinkNode.class,
		org.raven.conf.impl.AccessUserNode.class},
		parentNode=org.raven.conf.impl.GroupsListNode.class)
public class AccessGroupNode extends BaseNode implements Viewable 
{

	@Parameter
	@NotNull
	private String ldapGroup;
	
	public String getGroupString()
	{
		StringBuffer sb = new StringBuffer();
		ResourceNode.appendParam(sb, LdapGroupAcl.NAME_PARAM, getName());
		ResourceNode.appendParam(sb, LdapGroupAcl.LDAP_GROUP_PARAM, ldapGroup);
		for(Node n: getChildrenList())
		{
			if(n.getStatus()!=Status.STARTED) continue;
			if (n instanceof ResourceLinkNode) {
				ResourceLinkNode r = (ResourceLinkNode) n;
				ResourceNode.appendParam(sb, LdapGroupAcl.RESOURCE_PARAM, r.getResourceName());
				continue;
			}
			if (n instanceof AccessUserNode) {
				AccessUserNode r = (AccessUserNode) n;
				ResourceNode.appendParam(sb, LdapGroupAcl.USER_PARAM, r.getName());
			}
		}
		return sb.toString();
	}

	
	public List<ViewableObject> getViewableObjects(
			Map<String, NodeAttribute> refreshAttributes) throws Exception 
	{
		if (getStatus()!=Status.STARTED)
		    return null;
		ViewableObject textObj = 
			new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, getGroupString()); 
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

	public void setLdapGroup(String ldapGroup) {
		this.ldapGroup = AccessControl.removeDeniedSymbols(ldapGroup);
	}

	public String getLdapGroup() {
		return ldapGroup;
	}
	
}

package org.raven.auth.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

@NodeClass(childNodes={ResourceLinkNode.class, AccessUserNode.class}, parentNode=GroupsListNode.class)
public class AccessGroupNode extends BaseNode implements Viewable 
{
	@NotNull @Parameter
	private String ldapGroup;
    
    public AccessGroup getAccessGroup() {
        LinkedList<String> users = new LinkedList<String>();
        LinkedList<AccessResource> resources = new LinkedList<AccessResource>();
        for (Node node: getNodes())
            if (node.isStarted()) {
                if (node instanceof ResourceLinkNode)
                    resources.add(((ResourceLinkNode)node).getNode().getAccessResource());
                else if (node instanceof AccessUserNode)
                    users.add(node.getName());
            }
        return new AccessGroup(getName(), ldapGroup, resources, users);
    }
    
    public void addPoliciesIfNeed(UserContext user, AccessControlList policies) {
        if (!isUserAllowed(user))
            return;
        for (ResourceLinkNode resLink: NodeUtils.getChildsOfType(this, ResourceLinkNode.class)) {
            ResourceNode resNode = resLink.getNode();
            if (resNode.isStarted())
                policies.appendACL(resNode.getAccessResource());
        }
    }
    
    public List<AccessResource> getResourcesForUser(UserContext user) {
        if (!isUserAllowed(user))
            return Collections.EMPTY_LIST;
        LinkedList<AccessResource> resources = new LinkedList<AccessResource>();
        for (ResourceLinkNode resLink: NodeUtils.getChildsOfType(this, ResourceLinkNode.class)) {
            ResourceNode resNode = resLink.getNode();
            if (resNode.isStarted())
                resources.add(resNode.getAccessResource());
        }
        return resources;
    }
    
    private boolean isUserAllowed(UserContext user) {
        if (user.isAdmin())
            return true;
        if (!user.getGroups().contains(ldapGroup))
            return false;
        Node userNode = getNode(user.getLogin());
        if (userNode!=null && userNode instanceof AccessUserNode)
            return true;
        return NodeUtils.getChildsOfType(this, AccessUserNode.class).isEmpty();
    }
	
	public String getGroupString()
	{
		StringBuffer sb = new StringBuffer();
		ResourceNode.appendParam(sb, LdapGroupAcl.NAME_PARAM, getName());
		ResourceNode.appendParam(sb, LdapGroupAcl.LDAP_GROUP_PARAM, ldapGroup);
		for(Node n: getChildrenList())
		{
//			try {
//				ResourceNode r = (ResourceNode) n;
//			}
//			catch(Exception e) 
//			{
//				logger.error("AAA: ", e);
//			}
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

	public Boolean getAutoRefresh() {
		return true;
	}

	public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
		return null;
	}

	public void setLdapGroup(String ldapGroup) {
		//this.ldapGroup = AccessControl.removeDeniedSymbols(ldapGroup);
		this.ldapGroup = ldapGroup;
	}

	public String getLdapGroup() {
		return ldapGroup;
	}
	
}

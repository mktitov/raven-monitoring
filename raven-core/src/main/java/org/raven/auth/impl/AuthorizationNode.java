package org.raven.auth.impl;

import org.raven.annotations.NodeClass;
import org.raven.conf.Config;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

@NodeClass(parentNode=org.raven.tree.impl.InvisibleNode.class)
public class AuthorizationNode extends BaseNode 
{
	public static final String NODE_NAME = "Authorization";
	
	Config config;  
	
	private boolean allowEvents = false;
	
	public AuthorizationNode()
	{
		super(NODE_NAME);
		setSubtreeListener(true);
		setStartAfterChildrens(true);
		try {
			config = configurator.getConfig();
		} catch (Exception e) {
			logger.error("getConfig(): ",e);
		}
	}

	private void initChildren()
	{
		if(getChildren(ResourcesListNode.NODE_NAME)==null)
			addAndSaveChildren(new ResourcesListNode());
		if(getChildren(GroupsListNode.NODE_NAME)==null)
			addAndSaveChildren(new GroupsListNode());
	}
	
    @Override
	public synchronized void doInit() throws Exception
	{
		super.doInit();
		initChildren();
	}

    @Override
	public synchronized void doStart() throws Exception
	{
		super.doStart();
		initChildren();
		setAllowEvents(true);
		config.setAuthorizationTreeUpdated();
	}
	
    @Override
	public synchronized void doStop() throws Exception
	{
		super.doStart();
		setAllowEvents(false);
		config.setAuthorizationTreeUpdated();
	}

    @Override
	public synchronized void nodeStatusChanged(Node node, Status oldStatus, Status newStatus)
	{
		if(isAllowEvents())
			config.setAuthorizationTreeUpdated();
	}
	
	public String getAuthorizationData()
	{
		StringBuffer sb = new StringBuffer();
		try {
			ResourcesListNode res = (ResourcesListNode) getChildren(ResourcesListNode.NODE_NAME);
			if(res!=null && res.getStatus()==Status.STARTED)
				sb.append(res.getAllResourcesString());
			GroupsListNode gr = (GroupsListNode) getChildren(GroupsListNode.NODE_NAME);
			if(gr!=null && gr.getStatus()==Status.STARTED)
				sb.append(gr.getAllGroupsString());
		} catch(Exception e) {
			logger.error("on load authorization data from tree:",e);
		}
		logger.info("TREE_INFO len={}",sb.length());
		return sb.toString();
	}
	
	private boolean isAllowEvents() {
		return allowEvents;
	}

	private void setAllowEvents(boolean allowEvents) {
		this.allowEvents = allowEvents;
	}
	
	
}

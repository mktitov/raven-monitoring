package org.raven.auth.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.constraints.NotNull;

@NodeClass(parentNode=org.raven.auth.impl.AccessGroupNode.class)
public class ResourceLinkNode extends BaseNode implements Viewable 
{
	public static final String prefix = "res: ";
	public static final String PREFIX = LdapGroupAcl.RESOURCE_PARAM+AccessControl.DELIMITER+" ";
	
	@NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
	private ResourceNode node;
	
	public List<ViewableObject> getViewableObjects(
			Map<String, NodeAttribute> refreshAttributes) throws Exception 
	{
		if (getStatus()!=Status.STARTED)
		    return null;
		ViewableObject textObj = 
			new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, prefix+node.getName());
		return Arrays.asList(textObj);		
	}

	public Boolean getAutoRefresh() {
		return true;
	}

	public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
		return null;
	}

    public ResourceNode getNode() {
        return node;
    }

    public void setNode(ResourceNode node) {
        this.node = node;
    }

	public String getResourceName()	{
		if (getStatus()!=Status.STARTED)
		    return null;
		return node.getName();
	}
	
	public String getPrefix() {
		return PREFIX;
	}

}

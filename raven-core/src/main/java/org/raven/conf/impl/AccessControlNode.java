package org.raven.conf.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.weda.annotations.constraints.NotNull;

@NodeClass(parentNode=org.raven.conf.impl.ResourceNode.class)
public class AccessControlNode extends BaseNode implements Viewable 
{
	public static final String PREFIX = LdapGroupAcl.AC_PARAM+AccessControl.DELIMITER+" ";
	
	@Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
	@NotNull
	private Node node;

	@Parameter
	@NotNull
	private NodePathModifier modifier;
	
	@Parameter
	@NotNull
	private AccessRight	right;
	
	public AccessControlNode()
	{
		super();
	}

	
	public Boolean getAutoRefresh() 
	{
		return true;
	}

	public Map<String, NodeAttribute> getRefreshAttributes() throws Exception 
	{
		return null;
	}

	public String getACString()
	{
		if (getStatus()!=Status.STARTED)
		    return "";
		StringBuffer sb = new StringBuffer(node.getPath()); 
		sb.append(modifier.getModifier());
		sb.append(AccessControl.DELIMITER);
		sb.append(right.getRights());
		return sb.toString();
	}
	
	public List<ViewableObject> getViewableObjects(
			Map<String, NodeAttribute> refreshAttributes) throws Exception 
	{
		if (getStatus()!=Status.STARTED)
		    return null;
		ViewableObject textObj = new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, getACString());
		return Arrays.asList(textObj);		
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public Node getNode() {
		return node;
	}

	public void setModifier(NodePathModifier modifier) {
		this.modifier = modifier;
	}

	public NodePathModifier getModifier() {
		return modifier;
	}

	public void setRight(AccessRight right) {
		this.right = right;
	}

	public AccessRight getRight() {
		return right;
	}

	public String getPrefix()
	{
		return PREFIX;
	}

}

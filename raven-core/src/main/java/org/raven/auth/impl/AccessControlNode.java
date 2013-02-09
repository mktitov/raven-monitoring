package org.raven.auth.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
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
import static org.raven.auth.impl.NodePathModifier.*;

@NodeClass(parentNode=org.raven.auth.impl.ResourceNode.class)
public class AccessControlNode extends BaseNode implements Viewable 
{
	public static final String PREFIX = LdapGroupAcl.AC_PARAM+AccessControl.DELIMITER+" ";
	
	@NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
	private Node node;

	@NotNull @Parameter
	private NodePathModifier modifier;
	
	@NotNull @Parameter
	private AccessRight	right;
	
	public Boolean getAutoRefresh() {
		return true;
	}

	public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
		return null;
	}

	public String getACString()
	{
		if (getStatus()!=Status.STARTED)
		    return "";
		StringBuilder sb = new StringBuilder(node.getPath()); 
		sb.append(modifier.getModifier());
		sb.append(AccessControl.DELIMITER);
		sb.append(right.getRights());
		return sb.toString();
	}
    
    public List<AccessControl> getAccessControls() {
        if (!isStarted())
            return Collections.EMPTY_LIST;
        LinkedList<AccessControl> controls = new LinkedList<AccessControl>();
        String rights = right.getRights();
        NodePathModifier _modifier = modifier;
        if (_modifier!=CHILDREN_ONLY)
            controls.add(new AccessControl(node.getPath(), rights));
        if (_modifier==NODE_and_CHILDREN || _modifier==CHILDREN_ONLY)
            controls.add(new AccessControl(node.getPath()+AccessControl.CHILDREN, rights));
        return controls;
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

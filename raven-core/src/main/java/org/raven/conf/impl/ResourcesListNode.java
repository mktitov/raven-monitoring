package org.raven.conf.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
//import org.raven.tree.Node.Status;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ViewableObjectImpl;

@NodeClass(childNodes=org.raven.conf.impl.ResourceNode.class)
public class ResourcesListNode extends BaseNode implements Viewable 
{
	public static final String NODE_NAME = "Resources";
	public static final int START_NUM = 50000;
	
	public static final String LB = "<ol>";
	public static final String LE = "</ol>";
	public static final String IB = "<li>";
	public static final String IE = "</li>";

	public ResourcesListNode()
	{
		super(NODE_NAME);
	}
	
	public String getAllResourcesString(boolean v)
	{
		if (getStatus()!=Status.STARTED)
		    return "";
		StringBuffer sb = new StringBuffer();
		//appendParam(sb, AccessControlList.NAME_PARAM, getName());
		
		if(v) sb.append(LB);
		int i = START_NUM;
		for(Node n: getChildrenList())
		{
			if(n.getStatus()!=Status.STARTED) continue;
			ResourceNode rn = (ResourceNode) n;
			String rs = rn.getResourceString();
			if(rs!=null && rs.length()>0)
			{
				//appendParam(sb, AccessResource.AC_PARAM, rs);
				if(v) sb.append(IB);
					else sb.append(GroupsAclStorage.RESOURSE_PARAM_NAME + i++ + "=");
				sb.append(rs);
				if(v) sb.append(IE);
					else sb.append("\n");
			}	
		}
		if(v) sb.append(LE);
		return sb.toString();
	}
	
	public List<ViewableObject> getViewableObjects(
			Map<String, NodeAttribute> refreshAttributes) throws Exception 
	{
		if (getStatus()!=Status.STARTED)
		    return null;
		ViewableObject textObj = 
			new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, getAllResourcesString(true));
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

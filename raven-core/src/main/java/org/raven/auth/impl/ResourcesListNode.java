package org.raven.auth.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ViewableObjectImpl;

@NodeClass(childNodes={org.raven.auth.impl.ResourceNode.class,org.raven.auth.impl.ResourcesContainerNode.class})
public class ResourcesListNode extends BaseNode implements Viewable 
{
	public static final String NAME = "Resources";
	public static final int START_NUM = 50000;
	public static final String LB = "<ol>";
	public static final String LE = "</ol>";
	public static final String IB = "<li>";
	public static final String IE = "</li>";

	public static String makeHtmlList(List<String> z)
	{
		if(z==null || z.size()==0) return "";
		StringBuffer sb = new StringBuffer();
		sb.append(LB);
		for(String x: z)
			sb.append(IB).append(x).append(IE);
		sb.append(LE);
		return sb.toString();
	}
	
	public ResourcesListNode()
	{
		super(NAME);
	}
	
	public List<String> getAllResources()
	{
		ArrayList<String> all = new ArrayList<String>();
		if (getStatus()!=Status.STARTED) return all;
		for(Node n: getChildrenList())
		{
			if(n.getStatus()!=Status.STARTED) continue;
			if (n instanceof ResourceNode) {
				String rs = ((ResourceNode) n).getResourceString();
				if(rs!=null && rs.length()>0) all.add(rs);
			}
			if (n instanceof ResourcesContainerNode) {
				all.addAll( ((ResourcesContainerNode) n).getResourceStrings() );
			}
		}
		return all;
	}
	
	public String getAllResourcesString()
	{
		if (getStatus()!=Status.STARTED) return "";
		StringBuffer sb = new StringBuffer();
		List<String> all = getAllResources();
		int i = START_NUM;
		for(String x: all)
		{
			sb.append(GroupsAclStorage.RESOURSE_PARAM_NAME);
			sb.append(i++).append("=").append(x).append("\n");
		}
		return sb.toString();
	}
	
	public List<ViewableObject> getViewableObjects(
			Map<String, NodeAttribute> refreshAttributes) throws Exception 
	{
		if (getStatus()!=Status.STARTED)
		    return null;
		String s = makeHtmlList(getAllResources());
		ViewableObject textObj = 
			new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, s);
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

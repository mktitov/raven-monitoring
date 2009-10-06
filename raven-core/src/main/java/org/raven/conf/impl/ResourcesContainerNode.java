package org.raven.conf.impl;

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

@NodeClass(childNodes={org.raven.conf.impl.ResourceNode.class,
		org.raven.conf.impl.ResourcesContainerNode.class})
public class ResourcesContainerNode extends BaseNode implements Viewable 
{

	public List<String> getResourceStrings()
	{
		ArrayList<String> ret = new ArrayList<String>();
		if (getStatus()!=Status.STARTED) return ret;
		for(Node n: getChildrenList())
		{
			if(n.getStatus()!=Status.STARTED) continue;
			if (n instanceof ResourceNode) {
				String rs = ((ResourceNode) n).getResourceString();
				if(rs!=null && rs.length()>0) ret.add(rs);
			}
			if (n instanceof ResourcesContainerNode) {
				ret.addAll( ((ResourcesContainerNode) n).getResourceStrings() );
			}
		}
		return ret;
	}	
	
	public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
	throws Exception 
	{
		if (getStatus()!=Status.STARTED)
		    return null;
		String rs = ResourcesListNode.makeHtmlList(getResourceStrings());
		ViewableObject textObj = 
			new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, rs);
		return Arrays.asList(textObj);		
	}
	
	public Boolean getAutoRefresh() {
		return true;
	}

	public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
		return null;
	}
	
}

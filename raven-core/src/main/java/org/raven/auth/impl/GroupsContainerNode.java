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
import org.raven.tree.impl.InvisibleNode;
import org.raven.tree.impl.ViewableObjectImpl;

@NodeClass(parentNode=InvisibleNode.class, importChildTypesFromParent=true)
public class GroupsContainerNode extends BaseNode implements Viewable
{
    @Override
    public boolean isConditionalNode() {
        return true;
    }

	public List<String> getGroupStrings()
	{
		ArrayList<String> ret = new ArrayList<String>();
		if (getStatus()!=Status.STARTED) return ret;
		for(Node n: getChildrenList())
		{
			if(n.getStatus()!=Status.STARTED) continue;
			if (n instanceof AccessGroupNode) {
				String rs = ((AccessGroupNode) n).getGroupString();
				if(rs!=null && rs.length()>0) ret.add(rs);
			}
			if (n instanceof GroupsContainerNode) {
				ret.addAll( ((GroupsContainerNode) n).getGroupStrings() );
			}
		}
		return ret;
	}	
	
	public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
	throws Exception 
	{
		if (getStatus()!=Status.STARTED) return null;
		String s = ResourcesListNode.makeHtmlList(getGroupStrings());
		ViewableObject textObj = new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, s);
		return Arrays.asList(textObj);		
	}

	public Boolean getAutoRefresh() {
		return true;
	}

	public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
		return null;
	}
	
}

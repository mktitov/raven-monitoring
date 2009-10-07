package org.raven.ui;

import java.util.ArrayList;
import java.util.List;
import org.raven.util.Utl;

public class ResourceInfo 
{
	public static final int LIMIT = 10;
	public static final boolean recursive = true;
	public static final boolean stripTitle = true;
	//private AccessResource res;
	private String title;
	private String visibleTitle;
	private String path;
	private boolean valid = false;
	private List<ResourceInfo> list = new ArrayList<ResourceInfo>();
	
	public static final List<ResourceInfo> makeListX(List<ResourceInfo> lst)
	{
		List<ResourceInfo> x = new ArrayList<ResourceInfo>();
		ResourceInfo ri = new ResourceInfo(null,null);
		makeList(lst);
		ri.setChildrenList(lst);
		x.add(ri);
		return x;
	}
	
	public static final void makeList(List<ResourceInfo> lst)
	{
		if(lst.size() < LIMIT) return;
		//int i=0;
	//	for(int i=0; i<lst.size()-1 ;i++)
	//	{
		ResourceInfo ri = lst.get(0); 
		ri.findChildren(lst, 1);
	//	}
	}
	
	public ResourceInfo(String t, String p)
	{
		title = Utl.trim2Empty(t);
		path = Utl.trim2Empty(p);
		if(title.length()!=0 && path.length()!=0)
			valid = true;
		visibleTitle = title;
	}
	
	public boolean findChildren(List<ResourceInfo> r, int beg)
	{
		boolean ret = false;
		if(!isValid()) return ret;
		for(int i=beg; i< r.size()-1; i++)
		{
			ResourceInfo ri = r.get(i);
			if(ri.title.startsWith(title)) 
					//&& ri.path.startsWith(path))
			{
				list.add(ri);
				r.remove(i);
				if(stripTitle)
					ri.visibleTitle = ri.title.replace(title, "").trim();
				if(recursive) 
					ri.findChildren(r, i);
				i--;
			}
		}
		return ret;
	}

	public String getTitle() {
		return title;
	}
	
	public String getPath() {
		return path;
	}
	
	public boolean isValid() {
		return valid;
	}
	
	public List<ResourceInfo> getChildrenList() {
		return list;
	}

	private void setChildrenList(List<ResourceInfo> x) {
		list = x;
	}
	 
	public boolean hasChildren()
	{
		return list!=null && list.size()>0;
	}
	
	public String getVisibleTitle() {
		return visibleTitle;
	}
	
}

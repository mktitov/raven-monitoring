package org.raven.ui;

import java.util.ArrayList;
import java.util.List;

import org.raven.util.Utl;

public class ResourceInfo 
{
	public static final int LIMIT = 1;
	//private AccessResource res;
	private String title;
	private String path;
	private boolean valid = false;
	private List<ResourceInfo> list = new ArrayList<ResourceInfo>();

	public static final void makeList(List<ResourceInfo> lst)
	{
		if(lst.size() < LIMIT) return;
		//int i=0;
		for(int i=0; i<lst.size()-1 ;i++)
		{
			lst.get(i).findChildren(lst, i+1);
		}
	}
	
	public ResourceInfo(String t, String p)
	{
		title = Utl.trim2Null(t);
		path = Utl.trim2Null(p);
		if(title!=null && path!=null)
			valid = true;
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
	
	public List<ResourceInfo> getList() {
		return list;
	}

	public boolean hasChildren()
	{
		return list!=null && list.size()>0;
	}
	
	public boolean findChildren(List<ResourceInfo> r, int beg)
	{
		boolean ret = false;
		if(!isValid()) return ret;
		for(int i=beg; i< r.size()-1; i++)
		{
			ResourceInfo ri = r.get(i);
			if(ri.title.startsWith(title) && ri.path.startsWith(path))
			{
				list.add(ri);
				r.remove(i);
			}
		}
		return ret;
	}
	
}

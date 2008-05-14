package org.raven.conf.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupsList extends ArrayList<String> 
{
	static final long serialVersionUID =1 ;
	private long created;
	private boolean sorted = false;
	private String glist = "";
	
	public GroupsList() 
	{
		super();
		created = System.currentTimeMillis();
	}
	
	public boolean add(String grp)
	{
		addGroup(grp);
		return true;
	}
	
	private String pattern = "^CN=(.*?),OU=.*"; 
	public void addGroup(String fullName)
	{
		sorted = false;
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(fullName);
		if(m.find())
		 {
			String tmp = m.group(1);
		    if(tmp!=null && tmp.length()>0) super.add(tmp);
		 }
	}
	
	public void sort()
	{
		if(sorted) return;
		Collections.sort(this);
		StringBuffer sb = new StringBuffer();
		for(String x : this)
			sb.append(x);
		sorted = true;
	}

	public long getCreated() { return created; }
	
	public boolean equals(Object o)
	{
		if(o!=null && o instanceof GroupsList) 
		{
			GroupsList gl = (GroupsList) o;
			this.sort();
			gl.sort();
			if(this.glist.equals(gl.glist)) return true;
		}
		return false;
	}

}

/*
 *  Copyright 2008 Sergey Pinevskiy.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.conf.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupsList extends ArrayList<String> 
{
	protected Logger logger = LoggerFactory.getLogger(GroupsList.class);
	static final long serialVersionUID =1 ;
	public static final String pattern = "^CN=(.*?),OU=.*"; 

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
	
	public void addGroup(String fullName)
	{
		sorted = false;
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(fullName);
		if(m.find())
		 {
			String tmp = m.group(1);
		    if(tmp!=null && tmp.length()>0)
		    	super.add(tmp);
		 } 
	}
	
	public void sort()
	{
		if(sorted) return;
		Collections.sort(this);
		StringBuffer sb = new StringBuffer();
		for(String x : this) sb.append(x);
		glist = sb.toString();
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

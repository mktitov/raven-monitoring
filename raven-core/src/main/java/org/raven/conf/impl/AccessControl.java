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
import java.util.List;

import org.raven.tree.Node;

public class AccessControl {
	public static final int WRITE = 3; 
	public static final int READ = 2; 
	public static final int TRANSIT = 1; 
	public static final int NONE = 0; 
	
	private String resource = "";
	private String regExp = "";
	private int right = 0;
	
	public AccessControl(String rule)
	{
		String[] x = rule.split(":");
		if(x.length==2) loadData(x[0],x[1]);
	}

	public AccessControl(String resource, String right) { loadData(resource, right); }
	
	private void loadData(String resource, String right)
	{
		this.resource = resource;
		regExp = this.resource.replaceAll("\\*", ".*");
		String tmp = right.toLowerCase();
		if(tmp.length()==0 || tmp.charAt(0)=='n') this.right = NONE;
			else if(tmp.charAt(0)=='r') this.right = READ;
				else if(tmp.charAt(0)=='w') this.right = WRITE;
	}
	
	public static List<AccessControl> getACs(String rule)
	{
		ArrayList<AccessControl> al = new ArrayList<AccessControl>();
		if(rule==null || rule.length()==0) return al;
		String[] x = rule.split(":");
		if(x.length!=2) return al;
		
		if(x[0].endsWith("+"))
		{
			String t = x[0].substring(0, x[0].length()-2);
			al.add(new AccessControl(t,x[1]));
			al.add(new AccessControl(t+Node.NODE_SEPARATOR+"*",x[1]));
		} else al.add(new AccessControl(rule));
		
		return al;
	}
	
	public synchronized String getResource() { return resource; }
	public synchronized int getRight() { return right; }

	public synchronized String getRegExp() { return regExp; }
	
	public synchronized String toString() { return resource+":"+right; }
}

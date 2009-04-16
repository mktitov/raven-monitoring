package org.raven.conf.impl;
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


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessResource implements Comparator<AccessControl> 
{
	    protected Logger logger = LoggerFactory.getLogger(AccessResource.class);
		static final long serialVersionUID = 1;
		public static final String RESOURSE_NAME_PARAM = "name";
		public static final String FILTER_PARAM = "filter";
		public static final String EXCLUDE_FILTER_PARAM = "efilter";
		public static final String INCLUDE_FILTER_PARAM = "ifilter";
		private ArrayList<AccessControl> acl;
		public ArrayList<AccessControl> getAcl() {
			return acl;
		}

		private Set<String> filters;
		private boolean filtersLocked = false;
		private String name = null;

		public AccessResource() 
		{ 
			acl = new ArrayList<AccessControl>();
			filters = new HashSet<String>(); 
		}
		
		public AccessResource(String list) 
		{
			this();
			logger.info("Loading AR from : {}",list);
			String[] tokens = list.split(";"); 
			for(String token : tokens)
			{
				if(token==null || token.length()==0) 
					continue;
				String[] x = token.split(":");
				if(x.length<2 || x[1]==null || x[1].length()==0 )
					continue;
				if(x[0].equals(RESOURSE_NAME_PARAM))
				{
					setName(x[1]);
					continue;
				}
				if(x[0].equals(FILTER_PARAM))
				{	
					addFilter(x[1]);
					continue;
				}	
				acl.addAll(AccessControl.getACs(token));
			}	
			if(filters.size()==0) 
				setFiltersLocked();
			Collections.sort(acl, this);
		}
		
		private void addFilter(String f)
		{
			if( !isFiltersLocked() ) 
				filters.add(f);
		}
		
	    private void addAll(AccessResource x) 
	    {
	    	for(AccessControl ac : x.acl)
	    		acl.add(ac);
	    	if(x.isFiltersLocked()) setFiltersLocked();
	    		else if(!isFiltersLocked())
	    				filters.addAll(x.getFilters());
		}
		
		public int compare(AccessControl a, AccessControl b)
	    {
	    	if(a.getResource().length() > b.getResource().length()) return -1;
	    	if(a.getResource().length() < b.getResource().length()) return 1;
	    	if(a.getRight() > b.getRight()) return -1; 
	    	if(a.getRight() < b.getRight()) return 1; 
	    	return 0;
	    }
	    
	    public void appendACL(AccessResource x)
	    {
	    	if(x==null || !x.isValid()) return;
	    	this.addAll(x);
			Collections.sort(acl, this);
	    }

		@SuppressWarnings("unchecked")
		public static Set<String> getClassesAndInterfaces(Class cl)
		{
			HashSet<String> set = new HashSet<String>();
			Class x = cl;
			do {
				if(x.getName().equals(Object.class.getName())) break;
				set.add(x.getName());
				Class[]  ff = x.getInterfaces();
				for(Class f: ff )
					set.add(f.getName());
			} while( (x=x.getSuperclass()) != null );
			return set;
		}
	    
	    public boolean dropByFilterNodeOnly(Node n)
	    {
	    	if(isFiltersLocked()) return false;
	    	Set<String> set = getClassesAndInterfaces(n.getClass());
	    	Iterator<String> it = filters.iterator();
	    	while(it.hasNext())
	    	{
	    		String fl = it.next();
	    		if(set.contains(fl)) return false;
	    	}
	    	return true;
	    }

	    public boolean dropByFilter(Node n)
	    {
	    	if(!dropByFilterNodeOnly(n)) return false;
	    	Iterator<Node> it = n.getChildrenList().iterator();
	    	while(it.hasNext())
	    	{
	    		Node x = it.next();
	    		if(x.getChildrenCount()>0) return false;
	    		if(!dropByFilterNodeOnly(x)) return false;
	    	}
	    	return true;
	    }
	    
		public int getAccessForNode(Node node)
	    {
			String path = node.getPath();
	    	Iterator<AccessControl> it = acl.iterator();
	    	int curRight = AccessControl.NONE;
	    	//logger.info("access for node '"+node.getPath()+"'");
	    	while(it.hasNext())
	    	{
	    		AccessControl ac = it.next();
	        	//logger.info("AC "+ac.getRegExp()+" "+ac.getRight()+" "+ac.getResource());
	    		int right = ac.getRight();
	    		if(ac.getResource().startsWith(path))
	    			if(right > AccessControl.NONE )
	    			{
	    				curRight = AccessControl.TRANSIT;
	    				//continue;
	    			}	
	    		if( path.matches(ac.getRegExp()) && 
	    				right>=curRight )
	    		{
	    			curRight = right;
	    			break;
	    		}	
	    	}
	    	if( curRight>AccessControl.NONE && dropByFilter(node))
	    	{
	    		//logger.info("droped by filter "+node.getPath());
	    		return AccessControl.NONE;
	    	}
			//logger.info("node rigth = "+curRight+" for "+node.getPath());
	    	return curRight;
	    }

	    public String toString()
	    {
	    	StringBuffer sb = new StringBuffer();
	    	for(AccessControl ac : acl)
	    	{
	    		sb.append(ac.toString());
	    		sb.append(";");
	    	}
	    	return sb.toString();
	    }
	    
	    public int getACCount()
	    {
	    	return acl.size();
	    }

	    public boolean isValid()
	    {
	    	if(name!=null && acl.size()>0) 
	    		return true;
	    	return false;
	    }
	    
		private void setName(String x)	
		{
			if(name == null)
				name = x;
		}

		public String getName() {
			return name;
		}

		public Set<String> getFilters() {
			return filters;
		}

		public void setFiltersLocked() 
		{
			filters = null;
			this.filtersLocked = true;
		}

		public boolean isFiltersLocked() {
			return filtersLocked;
		}

}

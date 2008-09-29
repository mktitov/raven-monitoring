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
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessControlList implements Comparator<AccessControl> 
{
    protected Logger logger = LoggerFactory.getLogger(AccessControlList.class);
	static final long serialVersionUID = 1;
	public static final String LDAP_NAME_PARAM = "ldapName";
	public static final String FILTER_PARAM = "filter";
	private ArrayList<AccessControl> acl;
	private Set<String> filters;
	private boolean filtersLocked = false;
	private String group = null;

	public AccessControlList() 
	{ 
		acl = new ArrayList<AccessControl>();
		filters = new HashSet<String>(); 
	}
	
/*	public AccessControlList(String[] list, int startWith) 
	{
		this();
		for(int i=startWith; i < list.length; i++ )
			acl.addAll(AccessControl.getACs(list[i]));
		Collections.sort(acl, this);
	}
*/
	
	public AccessControlList(String list) 
	{
		this();
		logger.info("Loading ACL from : {}",list);
		String[] tokens = list.split(";"); 
		for(String token : tokens)
		{
			if(token==null || token.length()==0) 
				continue;
			String[] x = token.split(":");
			if(x.length<2 || x[1]==null || x[1].length()==0 )
				continue;
			if(x[0].equals(LDAP_NAME_PARAM))
				setGroup(x[1]);
			else
				if(x[0].equals(FILTER_PARAM)) 
					addFilter(x[1]);
				else 
					//acl.add(new AccessControl(x[0],x[1]));
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
	
    private void addAll(AccessControlList x) 
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
    
    public void appendACL(AccessControlList x)
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
    		if(ac.getResource().startsWith(path+Node.NODE_SEPARATOR))
    			if(right > AccessControl.NONE )
    			{
    				curRight = AccessControl.TRANSIT;
    				continue;
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
    	if(group!=null && acl.size()>0) 
    		return true;
    	return false;
    }
    
	private void setGroup(String group)	
	{
		if(this.group == null)
			this.group = group;
	}

	public String getGroup() {
		return group;
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

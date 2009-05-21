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

public abstract class AccessControlList implements Comparator<AccessControl> 
{
    protected Logger logger = LoggerFactory.getLogger(AccessControlList.class);
	static final long serialVersionUID = 1;
	public static final String EXPRESSION_DELIMITER = ";";
	public static final String PARAM_DELIMITER = ":";
	public static final String NAME_PARAM = "name";
	public static final String FILTER_PARAM = "filter";
	public static final String AC_PARAM = "ac";
	private ArrayList<AccessControl> acl = new ArrayList<AccessControl>();
	private Set<String> filters = new HashSet<String>();
	private boolean filtersLocked = false;
	//private String group = null;
	private String name = null;

	public AccessControlList() 
	{ 
	}
	
	public AccessControlList(String list) 
	{
		init(list);
	}
	
	protected void init(String list)
	{
		//this();
		logger.info("Loading ACL from : {}",list);
		String[] tokens = list.split(EXPRESSION_DELIMITER); 
		for(String token : tokens)
		{
			if(token==null || token.length()==0) 
				continue;
			String[] x = token.split(PARAM_DELIMITER);
			if(x.length<2 || x[1]==null || x[1].length()==0 )
				continue;
			if(x[0].equals(NAME_PARAM))
			{
				setName(x[1]);
				continue;
			}
			if(x[0].equals(FILTER_PARAM))
			{	
				addFilter(x[1]);
				continue;
			}
			if(x[0].equals(AC_PARAM))
			{
				if(x[2]!=null && x[2].length()>0)
					acl.addAll(AccessControl.getACs(x[1]+PARAM_DELIMITER+x[2]));
				continue;
			}	
			if(!applyExpression(x))
				logger.warn("unknown ACL expression : "+token);
		}	
		if(filters!=null && filters.size()==0) 
			setFiltersLocked();
		Collections.sort(acl, this);		
	}
	
	protected abstract boolean applyExpression(String[] tokens);
	public abstract boolean isValid();
	
	private void addFilter(String f)
	{
		if( !isFiltersLocked() ) 
			filters.add(f);
	}
	
    protected void addAll(AccessControlList x) 
    {
    	for(AccessControl ac : x.acl)
    		acl.add(ac);
    	if(x.isFiltersLocked()) setFiltersLocked();
    		else if(!isFiltersLocked())
    				filters.addAll(x.getFilters());
	}

/*    
    private void addAll(AccessResource x) 
    {
    	for(AccessControl ac : x.getAcl())
    		acl.add(ac);
    	if(x.isFiltersLocked()) setFiltersLocked();
    		else if(!isFiltersLocked())
    				filters.addAll(x.getFilters());
	}
   */
	public int compare(AccessControl a, AccessControl b)
    {
		int rightA = a.getRight();
		int rightB = b.getRight();
		int adm = (rightA & AccessControl.ADMIN) - (rightB & AccessControl.ADMIN);
		if(adm>0) return -1;
		if(adm<0) return 1;
		int lenCmp = a.getResource().length() - b.getResource().length();
    	if(lenCmp>0) return -1;
    	if(lenCmp<0) return 1;
    	if(rightA > rightB) return -1; 
    	if(rightA < rightB) return 1; 
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
    
	public ArrayList<AccessControl> getAcl() {
		return acl;
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

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}

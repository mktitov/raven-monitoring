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

package org.raven.auth.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AccessControlList implements Comparator<AccessControl> 
{
    protected Logger logger = LoggerFactory.getLogger(AccessControlList.class);
    
	static final long serialVersionUID = 1;
	public static final String NAME_PARAM = "name";
	public static final String FILTER_PARAM = "filter";
	public static final String AC_PARAM = "ac";
	public static final String DSC_PARAM = "dsc";
	public static final String TITLE_PARAM = "title";
    
	private final ArrayList<AccessControl> acl;
	private Set<String> filters = new HashSet<String>();
	private boolean filtersDisabled = false;
	//private String group = null;
	private String name = null;
	private String dsc = null;
	private String title = null;
	private AccessControl first = null;

	public AccessControlList() { 
        acl = new ArrayList<AccessControl>();
	}
	
	public AccessControlList(String list) {
        acl = new ArrayList<AccessControl>();
		init(list);
	}
    
    public AccessControlList(String name, String title, Collection<AccessControl> accessControls) {
        this.name = name;
        this.title = title;
        this.acl = new ArrayList<AccessControl>(accessControls);
        Collections.sort(acl, new AccessControllComparator());
		filters = null;
		this.filtersDisabled = true;
    }
	
	protected void init(String list)
	{
		//this();
		logger.info("Loading ACL from : {}",list);
		String[] tokens = list.split(AccessControl.EXPRESSION_DELIMITER); 
		for(String token : tokens)
		{
			if(token==null) continue;
			token = token.trim();
			if( token.length()==0) 
				continue;
			String[] x = token.split(AccessControl.DELIMITER);
			if(x.length<2 || x[1]==null || x[1].length()==0 )
				continue;
			x[0] = x[0].trim();
			x[1] = x[1].trim();
			if(x[0].equals(NAME_PARAM))
			{
				setName(x[1]);
				continue;
			}
			if(x[0].equals(DSC_PARAM))
			{
				setDsc(x[1]);
				continue;
			}
			if(x[0].equals(FILTER_PARAM))
			{	
				addFilter(x[1]);
				continue;
			}
			if(x[0].equals(TITLE_PARAM))
			{	
				setTitle(x[1]);
				continue;
			}
			if(x[0].equals(AC_PARAM))
			{
				if(x.length>2 && x[2]!=null && x[2].length()>0)
				{
					List<AccessControl> acAr = 
						AccessControl.getACs(x[1]+AccessControl.DELIMITER+x[2]);
					if(getFirst()==null) setFirst(acAr.get(0));
					acl.addAll(acAr);
				}	
				continue;
			}	
			if(!applyExpression(x))
				logger.warn("unknown ACL expression : "+token);
		}	
		if(filters!=null && filters.size()==0) 
			setFiltersLocked();
		Collections.sort(acl, this);		
	}
	
	/**
	 * Отвечает за разбор фрагмента строки описания группы 
	 * (фрагмент - строка вида parameterName[PARAM_DELIMITER]parValue1[PARAM_DELIMITER]parValue2...).
	 * Устанавливает значение найденного параметра. 
	 * @param tokens - фрагмент, разбитый на строки (разделитель-PARAM_DELIMITER).  
	 * @return <b>true</b> фрагмент успешно обработан, 
	 * <br><b>false</b> обнаружен неизвестный parameterName или недопустимое значение параметра.
	 */
	protected abstract boolean applyExpression(String[] tokens);
	public abstract boolean isValid();
	
	private void addFilter(String f)
	{
		if( !isFiltersDisabled() ) 
			filters.add(f);
	}
	
	/**
	 * Пополняет ACL элементами из другого.
	 * @param x добавляемый ACL.
	 */
    protected void addAll(AccessControlList x) 
    {
    	if(x==null || x.acl==null || x.acl.size()==0) return;
    	if(getFirst()==null)
    	{
    		AccessControl aa = x.acl.get(0);
    		setFirst(aa);
    	}	
    	for(AccessControl ac : x.acl)
    		acl.add(ac);
    	if(x.isFiltersDisabled()) setFiltersLocked();
    		else if(!isFiltersDisabled())
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
    	if(isFiltersDisabled()) return false;
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
    	for(Node x : n.getNodes()) {
    		if(x.getNodesCount()>0) return false;
    		if(!dropByFilterNodeOnly(x)) return false;
    	}
    	return true;
    }

	public int getAccessForNodeWF(String path)
    {
		if(path==null)
			return AccessControl.NONE;
    	int curRight = AccessControl.NONE;
    	for(AccessControl ac : acl)
    	{
    		int right = ac.getRight();
    		String res = ac.getResource();
    		if(res==null) continue;
    		if(res.startsWith(path))
    			if(right > AccessControl.NONE )
    				curRight = AccessControl.TRANSIT;
    		if( path.matches(ac.getRegExp()))
    		{
    			if(right > curRight) curRight = right;
    			break;
    		}	
    	}
    	return curRight;
    }
    
	public int getAccessForNode(Node node)
    {
		String path = node.getPath();
    	int curRight = getAccessForNodeWF(path);
    	if( curRight>AccessControl.NONE && dropByFilter(node))
    	{
    		//logger.info("droped by filter "+node.getPath());
    		return AccessControl.NONE;
    	}
		//logger.info("node rigth = "+curRight+" for "+node.getPath());
    	return curRight;
    }
/*
	public int getAccessForNode(Node node)
    {
		int z =0;
		if(node.getId()==196)
		{
			z = 1;
		}
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
    		if( path.matches(ac.getRegExp()))
    		{
    			if( right>=curRight ) curRight = right;
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
 */
	
	
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	for(AccessControl ac : acl) 
    		sb.append(ac.toString()).append(";");
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
		this.filtersDisabled = true;
	}

	public boolean isFiltersDisabled() {
		return filtersDisabled;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setDsc(String dsc) {
		this.dsc = dsc;
	}

	public String getDsc() {
		return dsc;
	}

	public void setTitle(String title) {
		if(title!=null) title = title.trim();
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setFirst(AccessControl first) {
		this.first = first;
	}

	public AccessControl getFirst() {
		return first;
	}
    
    private static class AccessControllComparator implements Comparator<AccessControl> {
        public int compare(AccessControl a, AccessControl b) {
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
    }
}

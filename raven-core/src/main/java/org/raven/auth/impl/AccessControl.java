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
import java.util.List;
import java.util.regex.Pattern;

public class AccessControl {
	public static final int ADMIN = 64;
	public static final char ADMIN_SYMBOL = 'a';
	public static final int TREE_EDIT = 32;
	public static final char TREE_EDIT_SYMBOL = 't';
	public static final int CONTROL = 16;
	public static final char CONTROL_SYMBOL = 's';
	public static final int WRITE = 8; 
	public static final char WRITE_SYMBOL = 'w'; 
	public static final int READ = 4; 
	public static final char READ_SYMBOL = 'r'; 
	public static final int VIEW = 2; 
	public static final char VIEW_SYMBOL = 'v'; 
	public static final int TRANSIT = 1; 
	public static final int NONE = 0; 
	public static final char NONE_SYMBOL = 'n'; 
	public static final String NODE = "";
	public static final String CHILDREN = "*";
	public static final String NODE_AND_CHILDREN = "+";
//	public static final String DELIM = ":";
//	public static final String AC_DELIM = ";";
	public static final String EXPRESSION_DELIMITER = ";";
	public static final String DELIMITER = ":";
//    public static final String disabledInParValues = "[\\^:;\"\\~\\"+Node.NODE_SEPARATOR+"\\"+Node.ATTRIBUTE_SEPARATOR+"]+";
    public static final String disabledInParValues = "[:;]+";
	
	private String resource = "";
	private String regExp = "";
	private int right = 0;
	
	private AccessControl(String rule)	{
		String[] x = rule.split(DELIMITER);
		if(x.length==2) loadData(x[0],x[1]);
	}

	public AccessControl(String resource, String right) { 
        loadData(resource, right); 
    }
    
//    private AccessControl
	
	public static String removeDeniedSymbols(String s) {
		return s.replaceAll(AccessControl.disabledInParValues, "");
	}
	
	private void loadData(String resource, String rightString)
	{
		this.resource = resource;
		if(resource.endsWith(CHILDREN)) {
			regExp = resource.substring(0, resource.length()-1);
			regExp = Pattern.quote(regExp)+".+";
		}
		else 
            regExp = Pattern.quote(resource);
		//regExp = this.resource.replaceAll("\\*", ".*");
		String tmp = rightString.toLowerCase();
		if(tmp.length()==0) tmp = ""+READ_SYMBOL;
		
		char[] chars = tmp.toCharArray();
		right = 0;
		boolean none = false;
		for(char cc: chars)
		{
			switch(cc)
			{
				case NONE_SYMBOL	: right = NONE; none= true; break;
				case VIEW_SYMBOL	: right |= VIEW;  break;
				case READ_SYMBOL	: right |= READ;  break;
				case WRITE_SYMBOL  	: right |= READ|WRITE;  break;
				case CONTROL_SYMBOL : right |= READ|CONTROL;  break;
				case TREE_EDIT_SYMBOL : right |= READ|WRITE|TREE_EDIT;  break;
				case ADMIN_SYMBOL : right |= READ|WRITE|CONTROL|TREE_EDIT|ADMIN;  break;
			}
			if(none) break;
		}
	}
	
	public static List<AccessControl> getACs(String rule)
	{
		ArrayList<AccessControl> al = new ArrayList<AccessControl>();
		if(rule==null || rule.length()==0) return al;
		String[] x = rule.split(DELIMITER);
		if(x.length!=2) return al;
		
		if(x[0].endsWith(NODE_AND_CHILDREN))
		{
			String t = x[0].substring(0, x[0].length()-1);
			al.add(new AccessControl(t,x[1]));
			al.add(new AccessControl(t+CHILDREN,x[1]));
		} else al.add(new AccessControl(rule));
		
		return al;
	}
	
	public synchronized String getResource() { return resource; }

	public synchronized String getNodePath() 
	{ 
		if(resource!=null && resource.endsWith(CHILDREN) )
			return resource.substring(0, resource.length()-1);
		return resource; 
	}
	
	public synchronized int getRight() { return right; }

	public synchronized String getRegExp() { return regExp; }
	
    @Override
	public synchronized String toString() { return resource+":"+right; }
}

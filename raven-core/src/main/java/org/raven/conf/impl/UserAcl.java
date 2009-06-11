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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UserAcl 
{
	protected Logger logger = LoggerFactory.getLogger(UserAcl.class);
	public static final long expireInterval = 300000;
	public static final String TEST_GROUP = "CN=testGroup,OU=testOU";
	public static final String PUBLIC_GROUP = "CN=PUBLIC,OU=testOU";
	
	private String accountName;
	private AccessControlList acl;
	private Config config;
	private long groupsTime=0;
	private long storageTime=0;
	private GroupsList gList;
	private GroupsAclStorage gaStorage;
	private boolean refreshed = true;
	private boolean testMode = false;
	
	public UserAcl(String accountName, Config cfg) 
	{
		this.accountName = accountName;
		this.config = cfg;
		testMode = config.getBooleanProperty(Configurator.TEST_MODE, Boolean.FALSE);
		gList = loadGroupsList();
		gaStorage = GroupsAclStorage.getInstance(config);
		storageTime = gaStorage.getLastUpdate();
		groupsTime = System.currentTimeMillis();
		acl = gaStorage.getAclForGroups(gList,accountName);
	}
	
	@SuppressWarnings("unchecked")
	private GroupsList loadGroupsList()
	{
	    GroupsList glist = new GroupsList();
	    glist.add(PUBLIC_GROUP);
	    if(testMode)
	    {
	    	glist.add(TEST_GROUP);
	    	glist.sort();
	    	return glist;
	    }

		String providerUrl = config.getStringProperty(Configurator.PROVIDER_URL, null);
		String bindName = config.getStringProperty(Configurator.BIND_NAME, null);
		String bindPassword = config.getStringProperty(Configurator.BIND_PASSWORD, null);
		String searchContext = config.getStringProperty(Configurator.SEARCH_CONTEXT, null);
		
	    Hashtable<String,String> env = new Hashtable<String,String>();
	    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
	    env.put(Context.PROVIDER_URL, providerUrl); 
	    env.put(Context.SECURITY_AUTHENTICATION, "simple"); //"DIGEST-MD5"
	    env.put(Context.SECURITY_PRINCIPAL, bindName); 
	    env.put(Context.SECURITY_CREDENTIALS, bindPassword);
	    InitialDirContext context = null;
	    try {
	    	context = new InitialDirContext(env); 
	    	String flt = "(sAMAccountName="+accountName+")";
	    	SearchControls sc = new SearchControls();
	    	sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
	    	String returnedAtts[]={"memberOf"};
	    	sc.setReturningAttributes(returnedAtts);
	    	NamingEnumeration<SearchResult> answer = context.search(searchContext,flt,sc); 
	    	while(answer.hasMoreElements()) 
	    	{
				SearchResult sr = (SearchResult)answer.next();
				Attributes attrs = sr.getAttributes();
				if (attrs == null) continue; 
				try {
					for(NamingEnumeration ae = attrs.getAll();ae.hasMore();) 
						for(NamingEnumeration ne = ((Attribute)ae.next()).getAll(); ne.hasMore();) 
						{
							String grpName = ne.next().toString();
							logger.info("found group:{}  for account:{}", grpName, this.accountName);
							glist.add(grpName);
						}	
				} catch (NamingException e)	
					{ logger.error("Problem listing groups for account: "+this.accountName , e); }
	    	}
		} catch(NamingException e) { logger.error("Problem searching directory: " , e); }
	    finally { try { context.close(); } catch(Exception e) {} }
    	glist.sort();
	    return glist;
	}
	
	public int getAccessForNode(Node node)
	{
		boolean refresh = false;
		if(System.currentTimeMillis() - groupsTime > UserAcl.expireInterval)
		{
			GroupsList gl = loadGroupsList();
			if(!gList.equals(gl))
			{
				refresh = true;
				gList = gl;
			}
			if(storageTime != gaStorage.getLastUpdate()) refresh = true;
			if(refresh)
			{
				logger.info("refreshing ACL for account: {}",accountName);
				storageTime = gaStorage.getLastUpdate();
				acl = gaStorage.getAclForGroups(gList,accountName);
				refreshed = true;
			}
			groupsTime = System.currentTimeMillis();
		}
		return acl.getAccessForNode(node);
	}

	public String getAccountName() { return accountName; }
	
	public HashMap<String,String>  getResourcesList()
	{
		HashMap<String,String> rl = new HashMap<String,String>();
		for(AccessResource ar : gaStorage.getResources().values())
		{
			String title = ar.getTitle();
			if(title==null || title.length()==0) continue;
			List<AccessControl> lst = ar.getAcl();
			if(lst.size()==0) continue;
			AccessControl ac = lst.get(0);
			if(ac==null) continue;
			String path = ac.getResource();
			if(path.endsWith("*")) path = path.substring(0, path.length()-1);
			rl.put(title, path);
		}
		return rl;
	}

	public boolean isRefreshed() 
	{
		boolean r = refreshed;
		refreshed = false;
		return r;
	}
	
	public boolean isEmpty()
	{
		if(acl.getACCount()!=0) return false;
		return true;
	}
}

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

import java.util.Hashtable;

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
	
	private String accountName;
	private AccessControlList acl;
	private Config config;
	private long groupsTime=0;
	private long storageTime=0;
	private GroupsList gList;
	private GroupsAclStorage gaStorage;
	private boolean refreshed = true;
	
	public UserAcl(String accountName, Config cfg) 
	{
		this.accountName = accountName;
		this.config = cfg;
		gList = loadGroupsList();
		gaStorage = GroupsAclStorage.getInstance(config);
		storageTime = gaStorage.getLastUpdate();
		groupsTime = System.currentTimeMillis();
		acl = gaStorage.getAclForGroups(gList);
	}
	
	@SuppressWarnings("unchecked")
	private GroupsList loadGroupsList()
	{
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
	    GroupsList glist = new GroupsList();
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
	
	@SuppressWarnings("unchecked")
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
				logger.info("refreshing ACL for account: {}",this.accountName);
				storageTime = gaStorage.getLastUpdate();
				acl = gaStorage.getAclForGroups(gList);
				refreshed = true;
			}
			groupsTime = System.currentTimeMillis();
		}
		return acl.getAccessForNode(node);
	}

	public String getAccountName() { return accountName; }

	public boolean isRefreshed() 
	{
		boolean r = refreshed;
		refreshed = false;
		return r;
	}
	
	public boolean isEmpty()
	{
		if(acl.size()!=0) return false;
		return true;
	}
}

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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.raven.auth.UserContext;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserAcl implements UserContext
{
	protected Logger logger = LoggerFactory.getLogger(UserAcl.class);
	public static final long expireInterval = 300000;
	public static final String TEST_GROUP = "CN=testGroup,OU=testOU";
	public static final String PUBLIC_GROUP = "CN=PUBLIC,OU=testOU";
	public static final String A_DN = "distinguishedName";
	public static final String A_MEMBER_OF = "memberOf";
	public static final String A_ANAME = "sAMAccountName";
	
	private String accountName;
	private AccessControlList acl;
	private Config config;
	private long groupsTime=0;
	private long storageTime=0;
	private GroupsList gList;
	private GroupsAclStorage gaStorage;
	private boolean refreshed = true;
	private boolean testMode = false;
    private Map<String, Object> params;
    private Map<String, List<Object>> attributes;
    private String userDN = null;
	
	public UserAcl(String accountName, Config cfg) 
	{
		this.accountName = accountName;
		this.config = cfg;
        this.params = new HashMap<String, Object>();
		testMode = config.getBooleanProperty(Configurator.TEST_MODE, Boolean.FALSE);
        if (testMode)
            logger.warn("Authorization work in TEST MODE!");
        attributes = loadLdapAttributes();
		gList = loadGroupsList();
		gaStorage = GroupsAclStorage.getInstance(config);
		storageTime = gaStorage.getLastUpdate();
		groupsTime = System.currentTimeMillis();
		acl = gaStorage.getAclForGroups(gList,accountName);
	}

    public Map<String, Object> getParams() {
        return params;
    }

    public List<String> getGroups() {
        return gList;
    }
    
    public String getDN()
    {
    	if(userDN==null)
    		userDN = findUserDN();
    	return userDN;
    }

    private String findUserDN()
    {
    	List<Object> l = attributes.get(A_DN);
    	if(l==null) return null;
    	Object o = l.get(0);
    	if(o==null) return null;
    	return o.toString();
    }

	@SuppressWarnings("unchecked")
	private Map<String, List<Object>> findLdapAttributes(String flt, String[] returnedAtts)
	{
		Map<String, List<Object>> ldapAttrs = new HashMap<String, List<Object>>();

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
	    	//String flt = "(sAMAccountName="+accountName+")";
	    	SearchControls sc = new SearchControls();
	    	sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
	    	//String returnedAtts[]={"memberOf"};
	    	sc.setReturningAttributes(returnedAtts);
	    	NamingEnumeration<SearchResult> answer = context.search(searchContext,flt,sc); 
	    	while(answer.hasMoreElements()) 
	    	{
				SearchResult sr = (SearchResult)answer.next();
				Attributes as = sr.getAttributes();
				if (as == null) continue; 
				try {
					for(NamingEnumeration ae = as.getAll();ae.hasMore();)
					{
						Attribute a = (Attribute) ae.next();
						String aId = a.getID();
						List<Object> vals = new ArrayList<Object>();
						for(NamingEnumeration ne = a.getAll(); ne.hasMore();) 
						{
							String val = ne.next().toString();
							logger.info("found attribute for account={}: name={} value={}", new Object[] { this.accountName, aId, val} );
							vals.add(val);
						}
						ldapAttrs.put(aId, vals);
					}	
				} catch (NamingException e)	
					{ logger.error("Problem listing attributes for account: "+this.accountName , e); }
	    	}
		} catch(NamingException e) { logger.error("Problem searching directory: " , e); }
	    finally { try { context.close(); } catch(Exception e) {} }
	    return ldapAttrs;
	}
    
	private Map<String, List<Object>> loadLdapAttributes()
	{
		return findLdapAttributes("(sAMAccountName="+accountName+")", null);
	}
    
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
	    
	    Map<String, List<Object>> a = findLdapAttributes("("+A_ANAME+"="+accountName+")", new String[] {A_MEMBER_OF} );
	    List<Object> x = a.get(A_MEMBER_OF);
	    if(x!=null)
	    {
	    	for(Object t : x)
	    		glist.add(t.toString());
//				logger.info("found group:{}  for account:{}", grpName, this.accountName);
	    	glist.sort();
	    }
	    return glist;
	}
	
	public boolean isSuperUser()
	{
		checkRefresh();
		int r = acl.getAccessForNodeWF(""+Node.NODE_SEPARATOR);
		return (r&AccessControl.ADMIN)!=0;
	}
	
	private void checkRefresh()
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
	}
	
	public int getAccessForNode(Node node)
	{
		checkRefresh();
		return acl.getAccessForNode(node);
	}

	public String getAccountName() { return accountName; }

	
	public HashMap<String,String>  getResourcesList(Tree tree)
	{
		HashMap<String,String> rl = new HashMap<String,String>();
		for(AccessResource ar : gaStorage.getResourcesX(tree).values())
		{
			String title = ar.getTitle();
			if(title==null || title.length()==0) continue;
			String path = null;
			if(ar.isPresent())
			{
				path = ar.getShow();
				if(acl.getAccessForNodeWF(path)<AccessControl.READ) continue;				
			}	
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

	public Map<String, List<Object>> getAttrs() {
		return attributes;
	}
}

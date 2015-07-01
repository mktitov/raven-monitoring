package org.raven.auth.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Хранилище ACL для конкрентной LDAP-группы
 * и, опционально, для конкрентных пользователей из данной группы
 */
public class LdapGroupAcl extends AccessControlList 
{
	public static final String LDAP_GROUP_PARAM = "ldapGroup";
	public static final String RESOURCE_PARAM = "res";
	public static final String USER_PARAM = "user";
	private String group = null;
	private List<String> users = null;
	private Map<String,AccessResource> resources = null;

	public LdapGroupAcl()
	{
		super();
	}
	
	public LdapGroupAcl(String x, HashMap<String,AccessResource> res)
	{
		setResources(res);
		init(x);
	}

    public LdapGroupAcl(String name, String group, Collection<String> resourcesNames, 
        Collection<String> users, Map<String, AccessResource> resources) 
    {
        super(name, null, Collections.EMPTY_LIST);
        this.group = group;
        if (users!=null && !users.isEmpty())
            this.users = new ArrayList<String>(users);
        if (resources!=null) {
            if (resourcesNames!=null && !resourcesNames.isEmpty())
                for (String resourceName: resourcesNames) {
                    AccessResource res = resources.get(resourceName);
                    if (res!=null) addAll(res);
                }
            this.resources = resources;
        }
    }
	
	protected boolean applyExpression(String[] tokens) 
	{
		if(tokens[0].equals(LDAP_GROUP_PARAM))
		{
			setLdapGroup(tokens[1]);
			return true;
		}
		if(tokens[0].equals(USER_PARAM))
		{
			addUser(tokens[1]);
			return true;
		}
		if(tokens[0].equals(RESOURCE_PARAM) && resources!=null)
		{
			AccessResource ar = resources.get(tokens[1]); 
			if(ar!=null)
			{
				addAll(ar);
				return true;
			}	
		}
		return false;		
	}
	
    public Map<String, AccessResource> getResources() {
		return resources;
	}

	public void setResources(HashMap<String, AccessResource> resources) {
		this.resources = resources;
	}

	public boolean isValid()
    {
    	if(group!=null && getAcl().size()>0) 
    		return true;
    	return false;
    }
	
	
	public String getLdapGroup() {
		return group;
	}

	private void setLdapGroup(String group) {
		this.group = group;
	}

	private void addUser(String user) 
	{
		if(users==null) users = new ArrayList<String>();
		users.add(user);
	}

	public boolean allowedUser(String user) 
	{
		if(users==null) return true;
		return users.contains(user);
	}
	
}

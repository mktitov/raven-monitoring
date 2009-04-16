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
import java.util.Iterator;
import java.util.List;
import org.raven.conf.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupsAclStorage  
{
    protected Logger logger = LoggerFactory.getLogger(GroupsAclStorage.class);
	public static final String GROUP_PARAM_NAME = "auth.group";
	public static final String RESOURSE_PARAM_NAME = "auth.resource";
	private static GroupsAclStorage instance = null;
	private Config config;
	private HashMap<String, AccessControlList> aclMap = null;
	private HashMap<String, AccessResource> arMap = null;
	private long lastUpdate = 0;

	protected GroupsAclStorage(Config config)
	{
		this.config = config;
		load();
	}

	protected void loadAR()
	{
		HashMap<String, AccessResource> acln = new HashMap<String, AccessResource>();
		for(int i=1; ;i++)
		{
			String gname = RESOURSE_PARAM_NAME+i;
			String val = config.getStringProperty(gname, null);
			StringBuffer sb = new StringBuffer();
			if(val!=null && val.length()>0)
				sb.append(val);
			for(int k=1; ;k++)
			{
				String v = config.getStringProperty(gname+"-"+k, null);
				if(v==null || v.length()==0) break;
				sb.append(";");
				sb.append(v);
			}
			if(sb.length()==0) break;
			AccessResource ar = new AccessResource(sb.toString());
			if(ar.isValid())
				acln.put(ar.getName(), ar);
			if(logger.isInfoEnabled())
				logger.info("resource name: {}  acl: {}",ar.getName(),ar.toString());
			
		}
		//lastUpdate = config.getLastUpdate();
		arMap = acln;
	}	
	
	protected void load()
	{
		loadAR();
		HashMap<String, AccessControlList> acln = new HashMap<String, AccessControlList>();
		for(int i=1; ;i++)
		{
			String gname = GROUP_PARAM_NAME+i;
			String val = config.getStringProperty(gname, null);
			StringBuffer sb = new StringBuffer();
			if(val!=null && val.length()>0)
				sb.append(val);
			for(int k=1; ;k++)
			{
				String v = config.getStringProperty(gname+"-"+k, null);
				if(v==null || v.length()==0) break;
				sb.append(";");
				sb.append(v);
			}
			if(sb.length()==0) break;
			AccessControlList acl = new AccessControlList(sb.toString(),arMap);
			if(acl.isValid())
				acln.put(acl.getGroup(), acl);
			if(logger.isInfoEnabled())
				logger.info("group name: {}  acl: {}",acl.getGroup(),acl.toString());
			
		}
		lastUpdate = config.getLastUpdate();
		aclMap = acln;
	}
	
	/**
	 * Returns a GroupsAclStorage object.
	 * @param config configurations parameters storage.
	 */
    public static final synchronized GroupsAclStorage getInstance(Config config)  
    {
        if( instance == null ) instance = new GroupsAclStorage(config);
        return instance;
    }

    /**
     * Returns summary AccessControlList for list of groups.
     * @param ls list of groups
     */
    public synchronized AccessControlList getAclForGroups(List<String> ls)
    {
    	if(lastUpdate!=config.getLastUpdate())
    	{
    		logger.info("reloading ACL for groups");
    		load();
    	}	
    	AccessControlList acl = new AccessControlList();
    	Iterator<String> it = ls.iterator();
    	while(it.hasNext())
    		acl.appendACL( aclMap.get(it.next()) );
    	return acl;
    }

	public synchronized long getLastUpdate() { return config.getLastUpdate(); }
	
}

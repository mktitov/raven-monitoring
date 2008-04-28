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
import java.util.List;
import org.raven.conf.Config;

public class GroupsAclStorage {
	public static final String GROUP_PARAM_NAME = "group";
	private static GroupsAclStorage instance = null;
	private Config config;
	private HashMap<String, AccessControlList> acl = null;

	protected GroupsAclStorage(Config config)
	{
		this.config = config;
		acl = new HashMap<String, AccessControlList>();
		
	}
	
	protected void load()
	{
		HashMap<String, AccessControlList> acln = new HashMap<String, AccessControlList>();
		for(int i=1; ;i++)
		{
			String val = config.getStringProperty(GROUP_PARAM_NAME+i, null);
			if(val==null || val.length()==0) break;
			String[] va = val.split(";");
			if(va.length<2) continue;
			AccessControlList acl = new AccessControlList(va,1);
			acln.put(va[0], acl);
		}
		acl = acln;
	}
	
	/**
	 * Returns a GroupsAclStorage object.
	 * @param config configurations parameters storage.
	 */
    public static final GroupsAclStorage getInstance(Config config)  
    {
        if( instance == null ) instance = new GroupsAclStorage(config);
        return instance;
    }

	
}

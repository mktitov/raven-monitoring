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
import java.util.Iterator;
import org.raven.tree.Node;

public class AccessControlList extends ArrayList<AccessControl> 
implements Comparator<AccessControl> 
{
	static final long serialVersionUID = 1;

	public AccessControlList() { super(); }
	
	public AccessControlList(String[] acl, int startWith) 
	{
		super();
		for(int i=startWith; i < acl.length-1; i++ )
		{
			this.add(new AccessControl(acl[i]));
		}
		Collections.sort(this, this);
	}
	
    public int compare(AccessControl a, AccessControl b)
    {
    	if(a.getResource().length() > b.getResource().length()) return -1;
    	if(a.getResource().length() < b.getResource().length()) return 1;
    	if(a.getRight() > b.getRight()) return -1; 
    	if(a.getRight() < b.getRight()) return 1; 
    	return 0;
    }
    
    public void appendACL(AccessControlList acl)
    {
    	if(acl==null) return;
    	this.addAll(acl);
		Collections.sort(this, this);
    }
    
    @SuppressWarnings("unchecked")
	public int getAccessForNode(Node node)
    {
		String path = node.getPath();
    	Iterator<AccessControl> it = this.iterator();
    	while(it.hasNext())
    	{
    		AccessControl ac = it.next();
    		if(ac.getResource().startsWith(path+Node.NODE_SEPARATOR))
    			if(ac.getRight() > AccessControl.NONE ) return AccessControl.READ;
    		if( path.matches(ac.getRegExp()) ) return ac.getRight();
    	}
    	return AccessControl.NONE;
    }

}

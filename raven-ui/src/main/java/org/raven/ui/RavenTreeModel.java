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

package org.raven.ui;

import java.util.ArrayList;
import java.util.List;
import org.apache.myfaces.trinidad.model.ChildPropertyTreeModel;
import org.raven.conf.impl.UserAcl;
import org.raven.tree.Node;

public class RavenTreeModel extends ChildPropertyTreeModel 
{
	private UserAcl userAcl = null;

	public RavenTreeModel() { super(); }
	
	public RavenTreeModel(Object instance, String childProperty)
	{
		super(instance,childProperty);
	}
	
	  @SuppressWarnings("unchecked")
	protected Object getChildData(Object parentData)
	  {
		  if(userAcl==null) return null;
		  Object o = super.getChildData(parentData);
		  if(o==null ) return null;
		  Object[] olist = null;
		  if (o instanceof Object[]) olist = (Object[]) o;
	    	else if (o instanceof List) olist = ((List) o).toArray();
		  if(olist==null) return null;
		  ArrayList<Node> ret = new ArrayList<Node>(); 
		  for(Object ob : olist)
		  	{
			  Node n = (Node) ob;
			  if(userAcl.getAccessForNode(n)>0) ret.add(n);
		  	}  
		  if(ret.size()==0) return null;
	    return ret;
	  }
	
	public UserAcl getUserAcl() { return userAcl; }
	public void setUserAcl(UserAcl userAcl) { this.userAcl = userAcl; }

}

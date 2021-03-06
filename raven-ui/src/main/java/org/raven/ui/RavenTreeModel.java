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
import org.raven.auth.UserContext;
import org.raven.auth.impl.AccessControl;
import org.raven.ui.node.NodeWrapper;

public class RavenTreeModel extends ChildPropertyTreeModel 
{
	private UserContext user = null;

	public RavenTreeModel() { super(); }
	
	public RavenTreeModel(Object instance, String childProperty)
	{
		super(instance,childProperty);
		//super.
	}
	
	@SuppressWarnings("unchecked")
    @Override
	protected Object getChildData(Object parentData)
	  {
		  if(user==null) return null;
		  Object o = super.getChildData(parentData);
		  if(o==null ) return null;
		  Object[] olist = null;
		  if (o instanceof Object[]) olist = (Object[]) o;
	    	else if (o instanceof List) olist = ((List) o).toArray();
		  if(olist==null) return null;
		  //ArrayList<Node> ret = new ArrayList<Node>();
		  ArrayList<NodeWrapper> ret = new ArrayList<NodeWrapper>();
		  int access = AccessControl.NONE; 
		  for(Object ob : olist)
		  	{
			  //Node n = (Node) ob;
			  NodeWrapper n = (NodeWrapper) ob; //new NodeWrapper((Node)ob);
			  int acc = user.getAccessForNode(n.getNode());
			  if(acc > AccessControl.NONE)
			  {
				  access = acc;
				  ret.add(n);
			  }  
		  	}  
		  if(ret.size()==0) return null;
		  if(ret.size()==1 && access == AccessControl.TRANSIT )
			  return getChildData(ret.get(0));
	    return ret;
	  }
	
	public UserContext getUserAcl() { return user; }
	public void setUserAcl(UserContext user) { this.user = user; }

}

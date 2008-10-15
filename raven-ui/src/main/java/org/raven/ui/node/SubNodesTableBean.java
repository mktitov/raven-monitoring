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

package org.raven.ui.node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.apache.myfaces.trinidad.component.UIXTable;
import org.apache.myfaces.trinidad.model.RowKeySet;
import org.apache.myfaces.trinidad.component.core.output.CoreMessage;
//import org.apache.myfaces.trinidad.component.UIXCollection;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.raven.ui.SessionBean;
import org.raven.ui.util.Messages;
import org.apache.myfaces.trinidad.event.ReturnEvent;

public class SubNodesTableBean 
{
	  private UIComponent table = null;
//	  private List<NodeWrapper> selected;
	  private CoreMessage message = null;

	  public SubNodesTableBean() 
	  {
		//  selected = Collections.EMPTY_LIST; 
	  }

	  @SuppressWarnings("unchecked")
	  public boolean loadNodeWrappers(List<NodeWrapper> lst)
	  {
		lst.clear();
		UIXTable tbl = (UIXTable) table;
	    RowKeySet state;
	    state = tbl.getSelectedRowKeys();
	    Iterator it = state.iterator();
	    Object oldKey = tbl.getRowKey();
	    NodeWrapper nw = null;
	    while(it.hasNext())
	    {
	      tbl.setRowKey(it.next());
	      nw = (NodeWrapper)tbl.getRowData();
	      lst.add(nw);
	    }
	    tbl.setRowKey(oldKey);
	    if(nw == null) return false;
	    state.clear();
	    return true;
	  }
	  
	  
	  public String copyNodes()
	  {
		 // CopyMoveNodeBean nb = (CopyMoveNodeBean) SessionBean.getElValue(CopyMoveNodeBean.BEAN_NAME);
		  
		  return "";
	  }
	  
	  @SuppressWarnings("unchecked")
	  public void deleteNodes2(ActionEvent action)
	  {
		//CopyMoveNodeBean nb = (CopyMoveNodeBean) SessionBean.getElValue(CopyMoveNodeBean.BEAN_NAME);
		//loadNodeWrappers(nb);
		UIXTable tbl = (UIXTable) table;
	    RowKeySet state;
	    StringBuffer retb = new StringBuffer();
	    state = tbl.getSelectedRowKeys();
	    Iterator it = state.iterator();
	    Object oldKey = tbl.getRowKey();
	    SessionBean sb = (SessionBean) SessionBean.getElValue(SessionBean.BEAN_NAME);
	    NodeWrapper nw = null;
	    while(it.hasNext())
	    {
	      tbl.setRowKey(it.next());
	      nw = (NodeWrapper)tbl.getRowData();
	      int x = sb.deleteNode(nw);
	      if(x==-1)
	      {
	    	if(retb.length()==0) retb.append("This nodes have dependensies: ");
	    	retb.append(" "+nw.getNodeName());
	      } else it.remove();
	      
	    }
	    tbl.setRowKey(oldKey);
	    if(nw == null)
	    {
	    	message.setMessage("No selected nodes !");
	    	return;
	    }
	    state.clear();
	    //tbl.setSelectedRowKeys(state);
	    sb.afterDeleteNodes();
	    
	    if(message!=null) message.setMessage(retb.toString());
	  }

	  
	  @SuppressWarnings("unchecked")
	  public void deleteNodes(ActionEvent action)
	  {
		UIXTable tbl = (UIXTable) table;
	    RowKeySet state;
	    StringBuffer retb = new StringBuffer();
	    state = tbl.getSelectedRowKeys();
	    Iterator it = state.iterator();
	    Object oldKey = tbl.getRowKey();
	    SessionBean sb = (SessionBean) SessionBean.getElValue(SessionBean.BEAN_NAME);
	    NodeWrapper nw = null;
	    while(it.hasNext())
	    {
	      tbl.setRowKey(it.next());
	      nw = (NodeWrapper)tbl.getRowData();
	      int x = sb.deleteNode(nw);
	      if(x==-1)
	      {
	    	if(retb.length()==0) retb.append("This nodes have dependensies: ");
	    	retb.append(" "+nw.getNodeName());
	      } else it.remove();
	      
	    }
	    tbl.setRowKey(oldKey);
	    if(nw == null)
	    {
	    	message.setMessage("No selected nodes !");
	    	return;
	    }
	    state.clear();
	    //tbl.setSelectedRowKeys(state);
	    sb.afterDeleteNodes();
	    
	    if(message!=null) message.setMessage(retb.toString());
	  }
	  
	  public String tryDelete(List<NodeWrapper> nodes)
	  {
	    SessionBean sb = (SessionBean) SessionBean.getElValue(SessionBean.BEAN_NAME);
	    return sb.deleteNodes(nodes);
	  }

	  public void moveHandleReturn(ReturnEvent event)
	  {
		  copyAndMove(event, false);
	  }

	  public void copyHandleReturn(ReturnEvent event)
	  {
		copyAndMove(event, true);  
	  }
	  
	  public void copyAndMove(ReturnEvent event, boolean copy)
		{
		  	SessionBean sb = (SessionBean) SessionBean.getElValue(SessionBean.BEAN_NAME);
		  	StringBuffer retb = new StringBuffer();
		  	Node n = (Node) event.getReturnValue();
		  	if(n==null)
		  	{
		  		message.setMessage(Messages.getUiMessage("noSelectedNodes"));
				sb.reloadBothFrames();
		  		return;
		  	}	
		  	NodeWrapper nx = new NodeWrapper(n);
		  	if(!nx.isAllowTreeEdit())
		  	{
		  		message.setMessage(Messages.getUiMessage("accessDenied"));
				sb.reloadBothFrames();
		  		return;
		  	}	
		  		
		  	ArrayList<NodeWrapper> nws = new ArrayList<NodeWrapper>();
		  	loadNodeWrappers(nws);
		  	for(NodeWrapper nw: nws)
		  		if(n.getPath().startsWith(nw.getNode().getPath()))
		  		{
		  			message.setMessage(Messages.getUiMessage("inadmissibleDstNode"));
		  			sb.reloadBothFrames();
		  			return;
		  		}	
		  	
		  	Tree tree = SessionBean.getTree();
		  	for(NodeWrapper nw: nws)
		  	{
		  		if(copy)
		  		try {tree.copy(nw.getNode(), n, null, null, true, true, false); }
		  		catch(Exception e) 
		  		{
		  			if(retb.length()==0) retb.append(Messages.getUiMessage("nodesCantBeCopied"));
		  			retb.append(" "+nw.getNodeName());
		  		}
		  	}	
			sb.reloadBothFrames();
			if(message!=null) message.setMessage(retb.toString());
		}

	  
	//  public List<Node> getSelected() { return selected; }
	  
	  public CoreMessage getMessage() { return message; }
	  public void setMessage(CoreMessage message) { this.message = message; }

	  public UIComponent getTable() { return table; }
	  public void setTable(UIComponent table) { this.table = table; }
}

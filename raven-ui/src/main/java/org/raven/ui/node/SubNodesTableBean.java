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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.faces.component.UIComponent;
//import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.apache.myfaces.trinidad.component.UIXTable;
import org.apache.myfaces.trinidad.model.RowKeySet;
import org.apache.myfaces.trinidad.component.core.output.CoreMessage;
//import org.apache.myfaces.trinidad.component.UIXCollection;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.raven.ui.SessionBean;
import org.raven.ui.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.myfaces.trinidad.event.ReturnEvent;

public class SubNodesTableBean 
{
	private static final Logger logger = LoggerFactory.getLogger(NodeWrapper.class);
	private UIComponent table = null;
	private CoreMessage message = null;

	public SubNodesTableBean() 
	{
		//  selected = Collections.EMPTY_LIST; 
	}

	  public static ArrayList<Object> getSeletedTableRowsData(UIXTable tbl, boolean clearState)
	  {
	    RowKeySet state = tbl.getSelectedRowKeys();
	    ArrayList<Object> ret = new ArrayList<Object>();
	    Object oldKey = tbl.getRowKey();
	    for(Iterator<Object> it = state.iterator(); it.hasNext();)
	    {
	    	Object x = it.next();
	      tbl.setRowKey(x);
	      ret.add(tbl.getRowData());
	    }
	    tbl.setRowKey(oldKey);
	    if(clearState) state.clear();
	    return ret;
	  }

	  public static ArrayList<Object> getSeletedTableRowsData(UIXTable tbl)
	  {
		  return getSeletedTableRowsData(tbl,true);
	  }
	  
	  /**
	   * Loads selected nodes into the list.
	   * @param lst
	   * @return true if nodes has been loaded
	   */
	  public boolean loadNodeWrappers(List<NodeWrapper> lst)
	  {
		lst.clear();
		ArrayList<Object> sel = getSeletedTableRowsData((UIXTable) table);
	    NodeWrapper nw = null;
	    for(Iterator<Object> it = sel.iterator(); it.hasNext();)
	    {
	      nw = (NodeWrapper)it.next();
	      lst.add(nw);
	    }
	    if(nw == null) return false;
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
		ArrayList<Object> sel = getSeletedTableRowsData((UIXTable) table);
	    StringBuffer retb = new StringBuffer();
	    SessionBean sb = SessionBean.getInstance();
	    NodeWrapper nw = null;
	    for(Iterator it = sel.iterator(); it.hasNext();)
	    {
	    	nw = (NodeWrapper)it.next();
	    	int x = sb.deleteNode(nw);
	    	if(x==-1)
	    	{
	    		if(retb.length()==0) retb.append(Messages.getUiMessage(Messages.NODES_HAVE_DEPEND)+" ");
	    		retb.append(" "+nw.getNodeName());
	    	} else it.remove();
	    }
	    if(nw == null)
	    {
	    	//"No selected nodes !"
	    	message.setMessage(Messages.getUiMessage(Messages.NO_SELECTED_NODES));
	    	return;
	    }
     	sb.afterDeleteNodes();
	    if(message!=null) message.setMessage(retb.toString());
	  }
	  
	  public void deleteNodes(ActionEvent action)
	  {
		ArrayList<Object> sel = getSeletedTableRowsData((UIXTable) table);
	    StringBuffer retb = new StringBuffer();
	    SessionBean sb = SessionBean.getInstance();
	    NodeWrapper nw = null;
	    for(Iterator<Object> it = sel.iterator(); it.hasNext();)
	    {
	    	nw = (NodeWrapper)it.next();
	    	int x = sb.deleteNode(nw);
	    	if(x==-1)
	    	{
	    		if(retb.length()==0) retb.append(Messages.getUiMessage(Messages.NODES_HAVE_DEPEND)+" ");
	    		retb.append(" "+nw.getNodeName());
	    	} else it.remove();
	    }
	    if(nw == null)
	    {
	    	message.setMessage(Messages.getUiMessage(Messages.NO_SELECTED_NODES));
	    	return;
	    }
	    sb.afterDeleteNodes();
	    if(message!=null) message.setMessage(retb.toString());
	  }

	  public void upNodes(ActionEvent action)
	  {
		  upDownNodes((UIXTable) table, true);
	  }

	  public void upDownNodes(UIXTable table, boolean up)
	  {
		ArrayList<Object> sel = getSeletedTableRowsData(table, true);
	    NodeWrapper nw = null;
	    List<NodeWrapper> nodes = null;
	    int maxIndex=0;
	    int minIndex=1;
	    NodeWrapper parent = null;
	    if( sel.size() == 0 ) 
	    {
	    	message.setMessage(Messages.getUiMessage(Messages.NO_SELECTED_NODES));
	    	return;
	    }
	    NodeWrapper t = (NodeWrapper) sel.get(0);
	    parent = new NodeWrapper(t.getNode().getParent());
	    nodes = parent.getChildrenList();
	    int ncnt = 0;
	    if(nodes!=null && (ncnt=nodes.size())>1)
	    	maxIndex = nodes.get(ncnt-1).getNode().getIndex();
	    if(maxIndex==0) return;
	    for(Object it : sel) nodes.remove(it);
	    for(Object ob : sel)
	    {
	    	nw = (NodeWrapper)ob;
	    	int cidx = nw.getNode().getIndex();
	    	int nup = -2;
	    	int mx = nodes.size();
	    	for(int i=0; i<mx; i++)
	    		if(cidx<nodes.get(i).getNode().getIndex())
	    		{
	    			if(up) nup = i-1;
	    				else nup = i;
	    			break;
	    		}
	    	if(nup==-2)
	    	{
	    		if(up) nup = mx-1; 
	    			else nup = mx;
	    	}	    		
	    	if(nup>=0 && nup<nodes.size()) 
	    	{
	    		Node upnode = nodes.get(nup).getNode();
	    		int upidx = upnode.getIndex();
	    		if(upidx>=minIndex)
	    		{
	    			nw.getNode().setIndex(upidx);
	    			upnode.setIndex(cidx);
	    			nw.getNode().save();
	    			upnode.save();
	    		}
	    	}
	    }
	    RowKeySet state = table.getSelectedRowKeys();
	    nodes = parent.getChildrenList();
	    for(Object it : sel)
	    {
	    	int idx = nodes.indexOf(it);
	    	if(idx>=0) state.add(new Integer(idx)); 
	    }
	    SessionBean.getInstance().reloadBothFrames();
	  }
	  
	  public void downNodes(ActionEvent action)
	  {
		  upDownNodes((UIXTable) table, false);
	  }
	  
	  public void selectAllNodes(ActionEvent action)
	  {
		  RowKeySet state = ((UIXTable) table).getSelectedRowKeys();
		  state.clear();
		  state.addAll();
//		  int all = ((UIXTable) table).getRowCount();
//		  for(int i=0;i<all;i++)
		  SessionBean.getInstance().reloadBothFrames();
	  }

	  public void cancelSelectNodes(ActionEvent action)
	  {
		  RowKeySet state = ((UIXTable) table).getSelectedRowKeys();
		  state.clear();
		  SessionBean.getInstance().reloadBothFrames();
	  }
	  
	  public String tryDelete(List<NodeWrapper> nodes)
	  {
	    return SessionBean.getInstance().deleteNodes(nodes);
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
		  	Node n = (Node) event.getReturnValue(); // dst node
		  	if(n==null)
		  	{
		  		message.setMessage(Messages.getUiMessage(Messages.NO_SELECTED_NODES));
				sb.reloadBothFrames();
		  		return;
		  	}	
		  	NodeWrapper nx = new NodeWrapper(n);
		  	if(!nx.isAllowTreeEdit())
		  	{
		  		message.setMessage(Messages.getUiMessage(Messages.ACCESS_DENIED));
				sb.reloadBothFrames();
		  		return;
		  	}	
		  		
		  	ArrayList<NodeWrapper> nws = new ArrayList<NodeWrapper>();
		  	loadNodeWrappers(nws);
		  	String copyPostfix = Messages.getUiMessage(Messages.NODES_COPY_POSTFIX);
		  	HashMap<Integer,String> newNames = new HashMap<Integer, String>();
		  	for(NodeWrapper nw: nws)
		  	{
		  		String to = n.getPath();
		  		String parent = nw.getNode().getParent().getPath();
		  		String from = nw.getNode().getPath();
		  		//logger.info("to="+to+" , from="+from+" , parent="+parent);
		  		if(to.equalsIgnoreCase(parent))
		  		{
		  			newNames.put(nw.getNodeId(), nw.getNode().getName()+copyPostfix);
		  			continue;
		  		}
		  		if(to.startsWith(from))
		  		{
		  			message.setMessage(Messages.getUiMessage(Messages.BAD_DST_NODE));
		  			sb.reloadBothFrames();
		  			return;
		  		}
		  	}	
		  	
		  	Tree tree = SessionBean.getTree();
		  	for(NodeWrapper nw: nws)
		  	{
  				String newName = newNames.get(nw.getNodeId());
		  		
		  		try {
		  			if(copy) tree.copy(nw.getNode(), n, newName, null, true, true, false);
		  				else tree.move(nw.getNode(), n, newName); 
		  		}
		  		catch(Exception e) 
		  		{
		  		 if(retb.length()==0)
		  		 {
		  			 String ms;
		  			 if(!copy) ms = Messages.NODES_CANT_BE_MOVED;
		  			 	else ms = Messages.NODES_CANT_BE_COPIED;
		  			 retb.append(Messages.getUiMessage(ms));
		  		 }			
		  		 retb.append(" "+nw.getNodeName());
		  		}
		  	}	
			sb.reloadBothFrames();
			if(message!=null) message.setMessage(retb.toString());
		}
	  
	  public CoreMessage getMessage() { return message; }
	  public void setMessage(CoreMessage message) { this.message = message; }

	  public UIComponent getTable() { return table; }
	  public void setTable(UIComponent table) { this.table = table; }
}

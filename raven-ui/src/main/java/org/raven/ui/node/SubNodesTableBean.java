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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.apache.myfaces.trinidad.component.UIXTable;
import org.apache.myfaces.trinidad.model.RowKeySet;
import org.apache.myfaces.trinidad.component.core.output.CoreMessage;
import org.raven.audit.Action;
import org.raven.audit.AuditRecord;
import org.raven.audit.Auditor;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.raven.tree.impl.NodeListenerAdapter;
import org.raven.tree.store.impl.XMLReader;
import org.raven.tree.store.impl.XMLWriter;
import org.raven.ui.SessionBean;
import org.raven.ui.util.Messages;
import org.raven.ui.vo.ExportBean;
import org.apache.myfaces.trinidad.event.ReturnEvent;
import org.raven.RavenUtils;
import org.raven.ds.DataConsumer;
import org.raven.template.impl.TemplateEntry;
import org.raven.util.NodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubNodesTableBean 
{
	private static final Logger logger = LoggerFactory.getLogger(SubNodesTableBean.class);
	private UIComponent table = null;
	private CoreMessage message = null;
	private UploadedFile uFile = null;

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
	public void deleteNodes22(ActionEvent action)
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
	    	message.setMessage(Messages.getUiMessage(Messages.NO_SELECTED_NODES));
	    	return;
	    }
     	sb.afterDeleteNodes();
	    if(message!=null) message.setMessage(retb.toString());
	  }

	public void deleteNodesX(ActionEvent action, boolean force)
	{
		ArrayList<Object> sel = getSeletedTableRowsData((UIXTable) table);
	    StringBuffer retb = new StringBuffer();
	    SessionBean sb = SessionBean.getInstance();
	    NodeWrapper nw = null;
	    for(Iterator<Object> it = sel.iterator(); it.hasNext();)
	    {
	    	nw = (NodeWrapper)it.next();
	    	int x;
	    	if(force) x = sb.forceDeleteNode(nw);
	    		else x = sb.deleteNode(nw);
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
	  
	  
	public void deleteNodes(ActionEvent action)
	{
		deleteNodesX(action,false);
	}

	public void forceDeleteNodes(ActionEvent action)
	{
		deleteNodesX(action,true);
	}
	  
	public void upNodes(ActionEvent action)
	{
		upDownNodesX((UIXTable) table, true);
	}
	  
	public void downNodes(ActionEvent action)
	{
		upDownNodesX((UIXTable) table, false);
	}
/*
	  public void importNodes(ValueChangeEvent event)
	  {
		  logger.info("!! importNodes: ");
		  UploadedFile uf = (UploadedFile) event.getNewValue();
		  if(uf==null) return;
		  logger.info("!! importNodes: {}", uf.getName());
		  logger.info("!! importNodes: len  {}", uf.getSize());
		  InputStream is = null;
		  try {
			  is = uf.getInputStream();
			  XMLReader xr = new XMLReader();
			  xr.read(SessionBean.getNodeWrapper().getNode(), is);
		  } catch (Exception e) {
			  logger.error("on importNodes: ",e);
		  }
		  finally {
			  try { is.close(); }  catch (Exception e) {}
		  }
	  }
*/	  
	  
	  public UploadedFile getFile() {
		  return uFile;
	  }
	  
	  public void setFile(UploadedFile f) {
		  uFile = f;
	  }
	  
	  public String doUpload()
	  {
		  message.setMessage("");
		  UploadedFile file = getFile();
	      if(file==null)
	      {
	    	  //message.setMessage("");
	    	  return null;
	      }	  
		  InputStream is = null;
		  SessionBean sb = SessionBean.getInstance();
		  AddNodeListener listener = new AddNodeListener();
		  Node node = sb.getCurrentNode();
		  node.addListener(listener);
		  try {
			  is = file.getInputStream();
			  XMLReader xr = new XMLReader();
			  xr.read(SessionBean.getNodeWrapper().getNode(), is);
			  SessionBean.getInstance().reloadLeftFrame();
		  } catch (Throwable e) {
			  logger.error("on importNodes: ",e);
			  message.setMessage(e.getMessage());
		  }
		  finally {
			  try { is.close(); }  catch (Exception e) {}
		  }
		  node.removeListener(listener);
		  if(listener.nodes.size()>0)
		  {
			  StringBuilder b = new StringBuilder("nodes: ");
			  for(String name : listener.nodes)
			  {	
				  b.append(name).append("; ");
				  sb.writeAuditRecord(Action.NODES_IMPORT, b.toString());
			  }	
		  }
	    return null;
	  }
	  
	  public void exportNodes(ActionEvent action)
	  {
		  if( !SessionBean.getInstance().isSuperUser() ) 
		  {
			  message.setMessage(Messages.getUiMessage(Messages.ACCESS_DENIED));
			  return;
		  }
		  
		  ArrayList<Object> sel = getSeletedTableRowsData((UIXTable) table, true);
		  if( sel.size() == 0 ) 
		  {
			  message.setMessage(Messages.getUiMessage(Messages.NO_SELECTED_NODES));
			  return;
		  }
		  Node[] nodes = new Node[sel.size()];
		  int i=0;
		  StringBuilder b = new StringBuilder("nodes: ");
		  for(Object o : sel)
		  {
			  Node n = ((NodeWrapper)o).getNode();
			  nodes[i++] = n;
			  b.append(n.getName()).append("; ");
		  }	  
		  writeExportNodesResponce(nodes);
		  SessionBean sb = SessionBean.getInstance(); 
		  sb.writeAuditRecord(Action.NODES_EXPORT, b.toString());
	  }
		
	  private void writeExportNodesResponce(Node[] nodes)
	  {
		  FacesContext fc = FacesContext.getCurrentInstance();
		  HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
		  String fName = ExportBean.makeFileName("nodes-", "xml");
		  String charset = "UTF-8";
		  response.setHeader("Content-disposition", "attachment; filename=" + fName);
		  response.setContentType("text/xml");
		  response.setCharacterEncoding(charset);	
		  OutputStream os = null;
		  try 
		  {
			  os = response.getOutputStream();
			  XMLWriter x = new XMLWriter();
			  x.write(os, charset, nodes);
		  }
		  catch (Exception e) { logger.error("on writeResponce: ",e); }
		  finally { try {os.close();} catch(Exception e) {}}
		  fc.responseComplete(); 			
	  }
	  
	  public void upDownNodesX(UIXTable table, boolean up)
	  {
		ArrayList<Object> sel = getSeletedTableRowsData(table, true);
	    if( sel.size() == 0 ) 
	    {
	    	message.setMessage(Messages.getUiMessage(Messages.NO_SELECTED_NODES));
	    	return;
	    }
        NodeWrapper parent =((NodeWrapper) sel.get(0)).getParent();
	    List<NodeWrapper> nodes = parent.getChildrenList();
	    if(nodes==null || nodes.size() < 2)	return;
	    int limit = 0;
	    int maxIndex = nodes.size()-1;
	    if(!up) {
	    	Collections.reverse(nodes);
	    	Collections.reverse(sel);
	    }	
	    RowKeySet state = table.getSelectedRowKeys();
	    state.clear();
	    for(Object ob : sel)
	    {
	    	NodeWrapper nw = (NodeWrapper)ob;
	    	int n = nodes.indexOf(nw);
	    	int nx = n;
	    	if(n==-1) break;
    		if(--n < limit) continue;
	    	int cidx = nw.getIndex();
	    	NodeWrapper nwx = nodes.get(n);
	    	int nidx = nwx.getIndex();
	    	nodes.set(n, nw);
	    	nodes.set(nx, nwx);
			nw.getNode().setIndex(nidx);
			nwx.getNode().setIndex(cidx);
			nw.getNode().save();
			nwx.getNode().save();
			limit = nx;
			if(up)
				state.add(new Integer(n));
			else 
				state.add(new Integer(maxIndex-n));
	    }
        NodeUtils.reconnectDataSources(parent.getNode());
            
	    //SessionBean.getInstance().reloadBothFrames();
	    SessionBean.getInstance().reloadRightFrame();
	  }
	  
	  
	  public void selectAllNodes(ActionEvent action)
	  {
		  RowKeySet state = ((UIXTable) table).getSelectedRowKeys();
		  state.clear();
		  state.addAll();
//		  int all = ((UIXTable) table).getRowCount();
//		  for(int i=0;i<all;i++)
		  //SessionBean.getInstance().reloadBothFrames();
		  SessionBean.getInstance().reloadRightFrame();
	  }

	  public void unselectNodes()
	  {
		  RowKeySet state = ((UIXTable) table).getSelectedRowKeys();
		  if(state!=null && !state.isEmpty())
			  state.clear();
	  }
	  
	  
	  public void cancelSelectNodes(ActionEvent action)
	  {
		  unselectNodes();
		  //SessionBean.getInstance().reloadBothFrames();
		  SessionBean.getInstance().reloadRightFrame();
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
		  	NodeWrapper nx = (NodeWrapper) event.getReturnValue(); // dst node
		  	if(nx==null)
		  	{
		  		message.setMessage(Messages.getUiMessage(Messages.NO_SELECTED_NODES));
				//sb.reloadBothFrames();
		  		sb.reloadRightFrame();
		  		return;
		  	}	
		  	//NodeWrapper nx = new NodeWrapper(n);
		  	if(!nx.isAllowTreeEdit())
		  	{
		  		message.setMessage(Messages.getUiMessage(Messages.ACCESS_DENIED));
				//sb.reloadBothFrames();
				sb.reloadRightFrame();
		  		return;
		  	}	
		  		
		  	ArrayList<NodeWrapper> nws = new ArrayList<NodeWrapper>();
		  	loadNodeWrappers(nws);
		  	String copyPostfix = Messages.getUiMessage(Messages.NODES_COPY_POSTFIX);
		  	HashMap<Integer,String> newNames = new HashMap<Integer, String>();
		  	for(NodeWrapper nw: nws)
		  	{
		  		String to = nx.getPath();
		  		Node parentNode = nw.getNode().getParent();
		  		if(parentNode==null) continue;
		  		String parent = parentNode.getPath();
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
		  			//sb.reloadBothFrames();
		  			sb.reloadRightFrame();
		  			return;
		  		}
		  	}	
		  	
		  	Tree tree = SessionBean.getTree();
		  	for(NodeWrapper nw: nws)
		  	{
  				String newName = newNames.get(nw.getNodeId());
	  			if(newName==null) newName =  nw.getNode().getName();
		  		
		  		try {
					String mes = "dst: node='{}' name='{}'";
		  			Action a = Action.NODE_MOVE;
		  			if(copy) a = Action.NODE_COPY;
		  			Auditor au = SessionBean.getInstance().getAuditor();
		  			
		  			AuditRecord aRec = au.prepare(nw.getNode(), SessionBean.getAccountNameS(), a, mes, nx.getPath(), newName);
                    boolean validateNodeType = !(nx.getNode() instanceof TemplateEntry);
		  			if(copy)
		  				tree.copy(nw.getNode(), nx.getNode(), newName, null, true, validateNodeType, false);
	  				else
	  					tree.move(nw.getNode(), nx.getNode(), newName);
		  			au.write(aRec);
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
		  		 logger.error("copyAndMove:",e);
		  		}
		  	}	
			//sb.reloadBothFrames();
		  	sb.reloadRightFrame();
			if(message!=null) message.setMessage(retb.toString());
		}
	  
	  public CoreMessage getMessage() { return message; }
	  public void setMessage(CoreMessage message) { this.message = message; }

	  public UIComponent getTable() { return table; }
	  public void setTable(UIComponent table) 
	  { 
		  this.table = table;
		  unselectNodes();  
	  }
	  
	  class AddNodeListener extends NodeListenerAdapter
	  {
		  List<String> nodes = new ArrayList<String>();
		  
		  AddNodeListener() {
				super(false);
		  }

		  public void childrenAdded(Node owner, Node children) 
		  {
			if(children!=null)
				nodes.add(children.getName());
		  }
		  
	  }

}

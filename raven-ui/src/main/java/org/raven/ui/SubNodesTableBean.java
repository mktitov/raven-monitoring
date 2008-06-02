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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.apache.myfaces.trinidad.component.UIXCollection;
import org.apache.myfaces.trinidad.component.UIXTable;
import org.apache.myfaces.trinidad.model.RowKeySet;
import org.apache.myfaces.trinidad.component.core.output.CoreMessage;
import org.raven.tree.Node;

public class SubNodesTableBean 
{
	  private UIComponent table = null;
	  private List<Node> selected;
	  private CoreMessage message = null;

	  @SuppressWarnings("unchecked")
	  public SubNodesTableBean() { selected = Collections.EMPTY_LIST; }
	  
	  @SuppressWarnings("unchecked")
	  public void deleteNodes(ActionEvent action)
	  {
	    UIXCollection tbl = (UIXCollection) table;
	    final RowKeySet state;
	    state = ((UIXTable) tbl).getSelectedRowKeys();
	    Iterator it = state.iterator();
	    Object oldKey = tbl.getRowKey();
	    selected = new ArrayList<Node>();
	    while (it.hasNext())
	    {
	      tbl.setRowKey(it.next());
	      selected.add((Node)tbl.getRowData());
	    }
	    tbl.setRowKey(oldKey);
	    if(selected.size()==0)
	    {
	    	message.setMessage("No selected nodes !");
	    	return;
	    }	
	    String ret = tryDelete(selected);
	    if(ret!=null && message!=null) message.setMessage(ret);
	  }
	  
	  public String tryDelete(List<Node> nodes)
	  {
	    FacesContext context = FacesContext.getCurrentInstance();
	    SessionBean sb = (SessionBean) context.getELContext().getELResolver().getValue(context.getELContext(), null, SessionBean.BEAN_NAME);
	    return sb.deleteNodes(nodes);
	  }

	//  public List<Node> getSelected() { return selected; }
	  
	  public CoreMessage getMessage() { return message; }
	  public void setMessage(CoreMessage message) { this.message = message; }

	  public UIComponent getTable() { return table; }
	  public void setTable(UIComponent table) { this.table = table; }
}
